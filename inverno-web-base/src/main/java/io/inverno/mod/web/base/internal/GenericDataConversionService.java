/*
 * Copyright 2024 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.web.base.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.ws.BaseWebSocketExchange;
import io.inverno.mod.http.base.ws.WebSocketException;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.base.InboundDataDecoder;
import io.inverno.mod.web.base.MissingConverterException;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link DataConversionService} implementation
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean( name = "dataConversionService" )
public class GenericDataConversionService implements @Provide DataConversionService {

	private final Map<String, MediaTypeConverter<ByteBuf>> convertersCache;

	private final List<MediaTypeConverter<ByteBuf>> converters;

	/**
	 * <p>
	 * Creates a data conversion service with the specified list of media type converters.
	 * </p>
	 *
	 * @param converters a list of converters
	 */
	public GenericDataConversionService(List<MediaTypeConverter<ByteBuf>> converters) {
		this.converters = converters;
		this.convertersCache = new HashMap<>();
	}

	@Override
	public MediaTypeConverter<ByteBuf> getConverter(String mediaType) throws MissingConverterException {
		if(mediaType == null) {
			throw new MissingConverterException();
		}
		mediaType = mediaType.toLowerCase();
		MediaTypeConverter<ByteBuf> result = this.convertersCache.get(mediaType);
		if (result == null && !this.convertersCache.containsKey(mediaType)) {
			for (MediaTypeConverter<ByteBuf> converter : this.converters) {
				if (converter.canConvert(mediaType)) {
					this.convertersCache.put(mediaType, converter);
					result = converter;
					break;
				}
			}
			if (result == null) {
				this.convertersCache.put(mediaType, null);
			}
		}
		if (result == null) {
			throw new MissingConverterException(mediaType);
		}
		return result;
	}

	@Override
	public <T> InboundDataDecoder<T> createDecoder(InboundData<ByteBuf> rawData, String mediaType, Class<T> type) throws MissingConverterException {
		return new GenericInboundDataDecoder<>(rawData, this.getConverter(mediaType), type);
	}

	@Override
	public <T> InboundDataDecoder<T> createDecoder(InboundData<ByteBuf> rawData, String mediaType, Type type) throws MissingConverterException {
		return new GenericInboundDataDecoder<>(rawData, this.getConverter(mediaType), type);
	}

	@Override
	public <T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType) throws MissingConverterException {
		return new GenericOutboundDataEncoder<>(rawData, this.getConverter(mediaType));
	}

	@Override
	public <T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType, Class<T> type) throws MissingConverterException {
		return new GenericOutboundDataEncoder<>(rawData, this.getConverter(mediaType), type);
	}

	@Override
	public <T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType, Type type) throws MissingConverterException {
		return new GenericOutboundDataEncoder<>(rawData, this.getConverter(mediaType), type);
	}

	@Override
	public BaseWeb2SocketExchange.Inbound createWebSocketDecodingInbound(BaseWebSocketExchange.Inbound inbound, String subprotocol) {
		return new GenericWebSocketInbound(inbound, () -> {
			try {
				return this.getConverter(MediaTypes.normalizeApplicationMediaType(subprotocol));
			}
			catch(MissingConverterException e) {
				throw new WebSocketException("No converter found for subprotocol: " + subprotocol, e);
			}
		});
	}

	@Override
	public BaseWeb2SocketExchange.Outbound createWebSocketEncodingOutbound(BaseWebSocketExchange.Outbound outbound, String subprotocol) {
		return new GenericWebSocketOutbound(outbound, () -> {
			try {
				return this.getConverter(MediaTypes.normalizeApplicationMediaType(subprotocol));
			}
			catch(MissingConverterException e) {
				throw new WebSocketException("No converter found for subprotocol: " + subprotocol, e);
			}
		});
	}

	/**
	 * <p>
	 * Generic {@link InboundDataDecoder} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @param <A> the type of the decoded object
	 */
	private static class GenericInboundDataDecoder<A> implements InboundDataDecoder<A> {

		private final InboundData<ByteBuf> rawData;

		private final MediaTypeConverter<ByteBuf> converter;

		private final Type type;

		/**
		 * <p>
		 * Creates a generic inbound data decoder.
		 * </p>
		 *
		 * @param rawData   the raw data consumer
		 * @param converter the converter
		 * @param type      a class of A
		 */
		public GenericInboundDataDecoder(InboundData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawData, converter, (Type) type);
		}

		/**
		 * <p>
		 * Creates a generic inbound data decoder.
		 * </p>
		 *
		 * @param rawData   the raw data consumer
		 * @param converter the converter
		 * @param type      the type of the decoded object
		 */
		public GenericInboundDataDecoder(InboundData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Type type) {
			this.rawData = rawData;
			this.converter = converter;
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Publisher<A> stream() {
			return (Publisher<A>) this.converter
				.decodeMany(this.rawData.stream(), this.type);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Mono<A> one() {
			return (Mono<A>) this.converter
				.decodeOne(this.rawData.stream(), this.type);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Flux<A> many() {
			return (Flux<A>) this.converter
				.decodeMany(this.rawData.stream(), this.type);
		}
	}

	/**
	 * <p>
	 * Generic {@link OutboundDataEncoder} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @param <A> the type of the object to encode
	 */
	private static class GenericOutboundDataEncoder<A> implements OutboundDataEncoder<A> {

		private final OutboundData<ByteBuf> rawData;

		private final MediaTypeConverter<ByteBuf> converter;

		private final Type type;

		/**
		 * <p>
		 * Creates a generic outbound data encoder.
		 * </p>
		 *
		 * @param rawData   the raw data producer
		 * @param converter the converter
		 */
		public GenericOutboundDataEncoder(OutboundData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter) {
			this(rawData, converter, null);
		}

		/**
		 * <p>
		 * Creates a generic outbound data encoder.
		 * </p>
		 *
		 * @param rawData   the raw data producer
		 * @param converter the converter
		 * @param type      a class of A
		 */
		public GenericOutboundDataEncoder(OutboundData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawData, converter, (Type) type);
		}

		/**
		 * <p>
		 * Creates a generic outbound data encoder.
		 * </p>
		 *
		 * @param rawData   the raw data producer
		 * @param converter the converter
		 * @param type      the type of the object to encode
		 */
		public GenericOutboundDataEncoder(OutboundData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Type type) {
			this.rawData = rawData;
			this.converter = converter;
			this.type = type;
		}

		@Override
		public <T extends A> void stream(Publisher<T> value) {
			this.many(Flux.from(value));
		}

		@Override
		public <T extends A> void many(Flux<T> value) {
			if (this.type == null) {
				this.rawData.stream(this.converter.encodeMany(value));
			}
			else {
				this.rawData.stream(this.converter.encodeMany(value, this.type));
			}
		}

		@Override
		public <T extends A> void one(Mono<T> value) {
			if (this.type == null) {
				this.rawData.stream(this.converter.encodeOne(value));
			}
			else {
				this.rawData.stream(this.converter.encodeOne(value, this.type));
			}
		}

		@Override
		public <T extends A> void value(T value) {
			if(value != null) {
				if (this.type == null) {
					this.rawData.stream(Mono.fromSupplier(() -> this.converter.encode(value)));
				}
				else {
					this.rawData.stream(Mono.fromSupplier(() -> this.converter.encode(value, this.type)));
				}
			}
			else {
				this.rawData.stream(Mono.empty());
			}
		}
	}

	/**
	 * <p>
	 * A generic {@link BaseWeb2SocketExchange.Inbound} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private static class GenericWebSocketInbound implements BaseWeb2SocketExchange.Inbound {

		private final BaseWebSocketExchange.Inbound inbound;

		private final Supplier<MediaTypeConverter<ByteBuf>> converterSupplier;

		private MediaTypeConverter<ByteBuf> converter;

		/**
		 * <p>
		 * Creates a generic decodable WebSocket inbound.
		 * </p>
		 *
		 * @param inbound           the original inbound
		 * @param converterSupplier the converter supplier
		 */
		public GenericWebSocketInbound(BaseWebSocketExchange.Inbound inbound, Supplier<MediaTypeConverter<ByteBuf>> converterSupplier) {
			this.inbound = inbound;
			this.converterSupplier = converterSupplier;
		}

		@Override
		public <A> Publisher<A> decodeTextMessages(Type type) {
			if(this.converter == null) {
				this.converter = converterSupplier.get();
			}
			return Flux.from(this.inbound.textMessages()).flatMap(message -> this.converter.decodeOne(message.raw(), type));
		}

		@Override
		public <A> Publisher<A> decodeBinaryMessages(Type type) {
			if(this.converter == null) {
				this.converter = converterSupplier.get();
			}
			return Flux.from(this.inbound.binaryMessages()).flatMap(message -> this.converter.decodeOne(message.raw(), type));
		}

		@Override
		public Publisher<WebSocketFrame> frames() {
			return this.inbound.frames();
		}

		@Override
		public Publisher<WebSocketMessage> messages() {
			return this.inbound.messages();
		}

		@Override
		public Publisher<WebSocketMessage> textMessages() {
			return this.inbound.textMessages();
		}

		@Override
		public Publisher<WebSocketMessage> binaryMessages() {
			return this.inbound.binaryMessages();
		}
	}

	/**
	 * <p>
	 * A generic {@link BaseWeb2SocketExchange.Outbound} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class GenericWebSocketOutbound implements BaseWeb2SocketExchange.Outbound {

		private final BaseWebSocketExchange.Outbound outbound;

		private final Supplier<MediaTypeConverter<ByteBuf>> converterSupplier;

		private MediaTypeConverter<ByteBuf> converter;

		/**
		 * <p>
		 * Creates a generic encodable WebSocket outbound.
		 * </p>
		 *
		 * @param outbound          the original outbound
		 * @param converterSupplier the converter supplier
		 */
		public GenericWebSocketOutbound(BaseWebSocketExchange.Outbound outbound, Supplier<MediaTypeConverter<ByteBuf>> converterSupplier) {
			this.outbound = outbound;
			this.converterSupplier = converterSupplier;
		}

		@Override
		public BaseWeb2SocketExchange.Outbound closeOnComplete(boolean closeOnComplete) {
			this.outbound.closeOnComplete(closeOnComplete);
			return this;
		}

		@Override
		public void frames(Function<WebSocketFrame.Factory, Publisher<WebSocketFrame>> frames) {
			this.outbound.frames(frames);
		}

		@Override
		public void messages(Function<WebSocketMessage.Factory, Publisher<WebSocketMessage>> messages) {
			this.outbound.messages(messages);
		}

		@Override
		public <T> void encodeTextMessages(Publisher<T> messages, Type type) {
			if(this.converter == null) {
				this.converter = converterSupplier.get();
			}
			if(type == null) {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.text(
						Flux.from(GenericDataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message))).map(buf -> buf.toString(Charsets.UTF_8))
				)));
			}
			else {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.text(
						Flux.from(GenericDataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message), type)).map(buf -> buf.toString(Charsets.UTF_8))
				)));
			}
		}

		@Override
		public <T> void encodeBinaryMessages(Publisher<T> messages, Type type) {
			if(this.converter == null) {
				this.converter = converterSupplier.get();
			}
			if(type == null) {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.binary(
						Flux.from(GenericDataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message)))
				)));
			}
			else {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.binary(
						Flux.from(GenericDataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message), type))
				)));
			}
		}
	}
}

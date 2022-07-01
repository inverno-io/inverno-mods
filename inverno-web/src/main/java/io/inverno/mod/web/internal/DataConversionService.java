/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.server.RequestData;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.http.server.ResponseData;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocketFrame;
import io.inverno.mod.http.server.ws.WebSocketMessage;
import io.inverno.mod.web.RequestDataDecoder;
import io.inverno.mod.web.ResponseDataEncoder;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebResponseBody;
import io.inverno.mod.web.WebResponseBody.SseEncoder;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * The data conversion service is used to create payload decoders/encoders from/to raw data based on a media type.
 * </p>
 *
 * <p>
 * It uses the list of {@link MediaTypeConverter} injected in the web module to find the right converter to decode or encode a payload in a particular media type.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see MediaTypeConverter
 */
@Bean(visibility = Visibility.PRIVATE)
public class DataConversionService {
	
	private final Map<String, MediaTypeConverter<ByteBuf>> convertersCache;

	private final List<MediaTypeConverter<ByteBuf>> converters;

	/**
	 * <p>
	 * Creates a data conversion service with the specified list of media type converters.
	 * </p>
	 *
	 * @param converters a list of converters
	 */
	public DataConversionService(List<MediaTypeConverter<ByteBuf>> converters) {
		this.converters = converters;
		this.convertersCache = new HashMap<>();
	}

	/**
	 * <p>
	 * Returns the first media type converter that can convert the specified media type.
	 * </p>
	 *
	 * @param mediaType a media type
	 *
	 * @return a media type converter
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public MediaTypeConverter<ByteBuf> getConverter(String mediaType) throws NoConverterException {
		if(mediaType == null) {
			throw new NoConverterException();
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
			throw new NoConverterException(mediaType);
		}
		return result;
	}

	/**
	 * <p>
	 * Creates a payload decoder.
	 * </p>
	 *
	 * @param <T>       the type of the decoded object
	 * @param rawData   raw payload consumer
	 * @param mediaType the source media type
	 * @param type      a class of T
	 *
	 * @return a request data decoder
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public <T> RequestDataDecoder<T> createDecoder(RequestData<ByteBuf> rawData, String mediaType, Class<T> type) throws NoConverterException {
		return new GenericRequestDataDecoder<>(rawData, this.getConverter(mediaType), type);
	}

	/**
	 * <p>
	 * Creates a payload decoder.
	 * </p>
	 *
	 * @param <T>       the type of the decoded object
	 * @param rawData   raw payload consumer
	 * @param mediaType the source media type
	 * @param type      the type of the decoded object
	 *
	 * @return a request data decoder
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public <T> RequestDataDecoder<T> createDecoder(RequestData<ByteBuf> rawData, String mediaType, Type type) throws NoConverterException {
		return new GenericRequestDataDecoder<>(rawData, this.getConverter(mediaType), type);
	}

	/**
	 * <p>
	 * Creates a payload encoder.
	 * </p>
	 *
	 * @param <T>       the type of object to encode
	 * @param rawData   raw payload producer
	 * @param mediaType the target media type
	 *
	 * @return a response data encoder
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType) throws NoConverterException {
		return new GenericResponseDataEncoder<>(rawData, this.getConverter(mediaType));
	}

	/**
	 * <p>
	 * Creates a payload encoder.
	 * </p>
	 * 
	 * @param <T>       the type of object to encode
	 * @param rawData   raw payload producer
	 * @param mediaType the target media type
	 * @param type      a class of T
	 * 
	 * @return a response data encoder
	 * 
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType, Class<T> type) throws NoConverterException {
		return new GenericResponseDataEncoder<>(rawData, this.getConverter(mediaType), type);
	}

	/**
	 * <p>
	 * Creates a payload encoder.
	 * </p>
	 *
	 * @param <T>       the type of object to encode
	 * @param rawData   raw payload producer
	 * @param mediaType the target media type
	 * @param type      the type of object to encode
	 *
	 * @return a response data encoder
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType, Type type) throws NoConverterException {
		return new GenericResponseDataEncoder<>(rawData, this.getConverter(mediaType), type);
	}

	/**
	 * <p>
	 * Creates a server-sent event encoder.
	 * </p>
	 *
	 * @param <T>       the type of object to encode
	 * @param rawSse    the raw server-sent event
	 * @param mediaType the target media type
	 *
	 * @return a server-sent events encoder
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType) throws NoConverterException {
		return new GenericSseEncoder<>(rawSse, this.getConverter(mediaType));
	}

	/**
	 * <p>
	 * Creates a server-sent event encoder.
	 * </p>
	 *
	 * @param <T>       the type of object to encode
	 * @param rawSse    the raw server-sent event
	 * @param mediaType the target media type
	 * @param type      a class of T
	 *
	 * @return a server-sent events encoder
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Class<T> type) throws NoConverterException {
		return new GenericSseEncoder<>(rawSse, this.getConverter(mediaType), type);
	}

	/**
	 * <p>
	 * Creates a server-sent event encoder.
	 * </p>
	 *
	 * @param <T>       the type of object to encode
	 * @param rawSse    the raw server-sent event
	 * @param mediaType the target media type
	 * @param type      the type of object to encode
	 *
	 * @return a server-sent events encoder
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Type type) throws NoConverterException {
		return new GenericSseEncoder<>(rawSse, this.getConverter(mediaType), type);
	}

	/**
	 * <p>
	 * Creates a WebSocket inbound which can decode WebSocket messages.
	 * </p>
	 *
	 * <p>
	 * The sub protocol is assumed to be a compact {@code application/} media type (e.g. {@code json} => {@code application/json}}. The normalized form is used to determine which converter to use.
	 * </p>
	 *
	 * @param inbound     the original inbound
	 * @param subProtocol the negotiated sub protocol
	 *
	 * @return a decodable WebSocket inbound
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 * 
	 * @see MediaTypes#normalizeApplicationMediaType(java.lang.String) 
	 */
	public Web2SocketExchange.Inbound createWebSocketDecodedInbound(WebSocketExchange.Inbound inbound, String subProtocol) throws NoConverterException {
		return new GenericWebSocketInbound(inbound, this.getConverter(MediaTypes.normalizeApplicationMediaType(subProtocol)));
	}
	
	/**
	 * <p>
	 * Creates a WebSocket outbound which can encode WebSocket messages.
	 * </p>
	 *
	 * <p>
	 * The sub protocol is assumed to be a compact {@code application/} media type (e.g. {@code json} => {@code application/json}}. The normalized form is used to determine which converter to use.
	 * </p>
	 *
	 * @param outbound    the original outbound
	 * @param subProtocol the negotiated sub protocol
	 *
	 * @return an encodable WebSocket outbound
	 *
	 * @throws NoConverterException if there's no converter that can convert the specified media type
	 * 
	 * @see MediaTypes#normalizeApplicationMediaType(java.lang.String) 
	 */
	public Web2SocketExchange.Outbound createWebSocketEncodedOutbound(WebSocketExchange.Outbound outbound, String subProtocol) throws NoConverterException {
		return new GenericWebSocketOutbound(outbound, this.getConverter(MediaTypes.normalizeApplicationMediaType(subProtocol)));
	}
	
	/**
	 * <p>
	 * Generic {@link RequestDataDecoder} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @param <A> the type of the decoded object
	 */
	private static class GenericRequestDataDecoder<A> implements RequestDataDecoder<A> {

		private final RequestData<ByteBuf> rawData;

		private final MediaTypeConverter<ByteBuf> converter;

		private final Type type;

		/**
		 * <p>
		 * Creates a generic request body decoder.
		 * </p>
		 * 
		 * @param rawData   the raw data consumer
		 * @param converter the converter
		 * @param type      a class of A
		 */
		public GenericRequestDataDecoder(RequestData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawData, converter, (Type) type);
		}

		/**
		 * <p>
		 * Creates a generic request body decoder.
		 * </p>
		 * 
		 * @param rawData   the raw data consumer
		 * @param converter the converter
		 * @param type      the type of the decoded object
		 */
		public GenericRequestDataDecoder(RequestData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Type type) {
			this.rawData = rawData;
			this.converter = converter;
			this.type = type;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Publisher<A> stream() {
			return (Publisher<A>) this.converter
				.decodeMany(this.rawData.stream(), this.type)
				.onErrorMap(ConverterException.class, ex -> new BadRequestException(ex));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Mono<A> one() {
			return (Mono<A>) this.converter
				.decodeOne(this.rawData.stream(), this.type)
				.onErrorMap(ConverterException.class, ex -> new BadRequestException(ex));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Flux<A> many() {
			return (Flux<A>) this.converter
				.decodeMany(this.rawData.stream(), this.type)
				.onErrorMap(ConverterException.class, ex -> new BadRequestException(ex));
		}
	}

	/**
	 * <p>
	 * Generic {@link ResponseDataEncoder} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @param <A> the type of the object to encode
	 */
	private static class GenericResponseDataEncoder<A> implements ResponseDataEncoder<A> {

		private final ResponseData<ByteBuf> rawData;

		private final MediaTypeConverter<ByteBuf> converter;

		private final Type type;

		/**
		 * <p>
		 * Creates a generic request body encoder.
		 * </p>
		 * 
		 * @param rawData   the raw data producer
		 * @param converter the converter
		 */
		public GenericResponseDataEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter) {
			this(rawData, converter, null);
		}

		/**
		 * <p>
		 * Creates a generic request body encoder.
		 * </p>
		 * 
		 * @param rawData   the raw data producer
		 * @param converter the converter
		 * @param type      a class of A
		 */
		public GenericResponseDataEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawData, converter, (Type) type);
		}

		/**
		 * <p>
		 * Creates a generic request body encoder.
		 * </p>
		 * 
		 * @param rawData   the raw data producer
		 * @param converter the converter
		 * @param type      the type of the object to encode
		 */
		public GenericResponseDataEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Type type) {
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
	 * A generic {@link WebResponseBody.SseEncoder} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @param <A> the type of the object to encode
	 */
	private static class GenericSseEncoder<A> implements WebResponseBody.SseEncoder<A> {

		private final ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse;

		private final MediaTypeConverter<ByteBuf> converter;

		private final Type type;

		/**
		 * <p>
		 * Creates a generic server-sent event encoder.
		 * </p>
		 * 
		 * @param rawSse    the raw server-sent event
		 * @param converter the converter
		 */
		public GenericSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, MediaTypeConverter<ByteBuf> converter) {
			this(rawSse, converter, null);
		}

		/**
		 * <p>
		 * Creates a generic server-sent event encoder.
		 * </p>
		 * 
		 * @param rawSse    the raw server-sent event
		 * @param converter the converter
		 * @param type      a class of A
		 */
		public GenericSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawSse, converter, (Type) type);
		}

		/**
		 * <p>
		 * Creates a generic server-sent event encoder.
		 * </p>
		 * 
		 * @param rawSse    the raw server-sent event
		 * @param converter the converter
		 * @param type      the type of the object to encode
		 */
		public GenericSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, MediaTypeConverter<ByteBuf> converter, Type type) {
			this.rawSse = rawSse;
			this.converter = converter;
			this.type = type;
		}

		@Override
		public void from(BiConsumer<EventFactory<A>, ResponseData<Event<A>>> data) {
			this.rawSse.from((rawEvents, rawData) -> {
				data.accept(
					eventConfigurer -> {
						GenericEvent<A> event = new GenericEvent<>();
						rawEvents.create(rawEvent -> {
							event.setRawEvent(rawEvent);
							eventConfigurer.accept(event);
						});
						return event;
					}, 
					new ResponseData<WebResponseBody.SseEncoder.Event<A>>() {
	
						@Override
						@SuppressWarnings("unchecked")
						public <T extends WebResponseBody.SseEncoder.Event<A>> void stream(Publisher<T> value) {
							rawData.stream(Flux.from(value)
								.cast(GenericEvent.class)
								.<ResponseBody.Sse.Event<ByteBuf>>map(GenericEvent::getRawEvent)
							);
						}
					}
				);
			});
		}

		/**
		 * <p>
		 * Generic {@link SseEncoder.Event} implementation.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 *
		 * @param <B>
		 */
		private class GenericEvent<B> implements SseEncoder.Event<B> {

			private ResponseBody.Sse.Event<ByteBuf> rawEvent;

			/**
			 * <p>
			 * Sets the underlying raw server-sent event.
			 * </p>
			 * 
			 * @param rawEvent the underlying raw server-sent event
			 */
			public void setRawEvent(ResponseBody.Sse.Event<ByteBuf> rawEvent) {
				this.rawEvent = rawEvent;
			}

			/**
			 * <p>
			 * Returns the underlying raw server-sent event.
			 * </p>
			 * 
			 * @return a raw server-sent event
			 */
			public ResponseBody.Sse.Event<ByteBuf> getRawEvent() {
				return this.rawEvent;
			}

			@Override
			public GenericEvent<B> id(String id) {
				this.rawEvent.id(id);
				return this;
			}

			@Override
			public GenericEvent<B> comment(String comment) {
				this.rawEvent.comment(comment);
				return this;
			}

			@Override
			public GenericEvent<B> event(String event) {
				this.rawEvent.event(event);
				return this;
			}

			@Override
			public <T extends B> void stream(Publisher<T> value) {
				this.many(Flux.from(value));
			}

			@Override
			public <T extends B> void many(Flux<T> value) {
				if (GenericSseEncoder.this.type == null) {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeMany(value));
				}
				else {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeMany(value, GenericSseEncoder.this.type));
				}
			}

			@Override
			public <T extends B> void one(Mono<T> value) {
				if (GenericSseEncoder.this.type == null) {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeOne(value));
				} 
				else {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeOne(value, GenericSseEncoder.this.type));
				}
			}

			@Override
			public <T extends B> void value(T value) {
				if(value != null) {
					if (GenericSseEncoder.this.type == null) {
						this.rawEvent.stream(Mono.fromSupplier(() -> GenericSseEncoder.this.converter.encode(value)));
					} 
					else {
						this.rawEvent.stream(Mono.fromSupplier(() -> GenericSseEncoder.this.converter.encode(value, GenericSseEncoder.this.type)));
					}
				}
				else {
					this.rawEvent.stream(Mono.empty());
				}
			}
		}
	}

	/**
	 * <p>
	 * A generic {@link Web2SocketExchange.Inbound} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class GenericWebSocketInbound implements Web2SocketExchange.Inbound {

		private final WebSocketExchange.Inbound inbound;
		
		private final MediaTypeConverter<ByteBuf> converter;

		/**
		 * <p>
		 * Creates a generic decodable WebSocket inbound.
		 * </p>
		 * 
		 * @param inbound   the original inbound
		 * @param converter the converter
		 */
		public GenericWebSocketInbound(WebSocketExchange.Inbound inbound, MediaTypeConverter<ByteBuf> converter) {
			this.inbound = inbound;
			this.converter = converter;
		}
		
		@Override
		public <A> Publisher<A> decodeTextMessages(Type type) {
			return Flux.from(this.inbound.textMessages()).flatMap(message -> this.converter.decodeOne(message.binary(), type));
		}

		@Override
		public <A> Publisher<A> decodeBinaryMessages(Type type) {
			return Flux.from(this.inbound.binaryMessages()).flatMap(message -> this.converter.decodeOne(message.binary(), type));
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
	 * A generic {@link Web2SocketExchange.Outbound} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static class GenericWebSocketOutbound implements Web2SocketExchange.Outbound {

		private final WebSocketExchange.Outbound outbound;
		
		private final MediaTypeConverter<ByteBuf> converter;

		/**
		 * <p>
		 * Creates a generic encodable WebSocket outbound.
		 * </p>
		 * 
		 * @param outbound  the original outbound
		 * @param converter the converter
		 */
		public GenericWebSocketOutbound(WebSocketExchange.Outbound outbound, MediaTypeConverter<ByteBuf> converter) {
			this.outbound = outbound;
			this.converter = converter;
		}
		
		@Override
		public <T> void encodeTextMessages(Publisher<T> messages, Type type) {
			if(type == null) {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.text(
					Flux.from(DataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message))).map(buf -> buf.toString(Charsets.UTF_8))
				)));
			}
			else {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.text(
					Flux.from(DataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message), type)).map(buf -> buf.toString(Charsets.UTF_8))
				)));
			}
		}

		@Override
		public <T> void encodeBinaryMessages(Publisher<T> messages, Type type) {
			if(type == null) {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.binary(
					Flux.from(DataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message)))
				)));
			}
			else {
				this.outbound.messages(factory -> Flux.from(messages).map(message -> factory.binary(
					Flux.from(DataConversionService.GenericWebSocketOutbound.this.converter.encodeOne(Mono.just(message), type))
				)));
			}
		}

		@Override
		public void frames(Function<WebSocketFrame.Factory, Publisher<WebSocketFrame>> frames) {
			this.outbound.frames(frames);
		}

		@Override
		public void messages(Function<WebSocketMessage.Factory, Publisher<WebSocketMessage>> messages) {
			this.outbound.messages(messages);
		}
	}
	
	/**
	 * <p>
	 * Media type converters socket.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see DataConversionService
	 */
	@Bean(name = "mediaTypeConverters")
	public static interface MediaTypeConvertersSocket extends Supplier<List<MediaTypeConverter<ByteBuf>>> {
	}
}

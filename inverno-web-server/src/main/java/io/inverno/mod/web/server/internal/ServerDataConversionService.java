/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.ws.BaseWebSocketExchange;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.base.InboundDataDecoder;
import io.inverno.mod.web.base.MissingConverterException;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.server.WebResponseBody;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Extends the base {@link DataConversionService} with Server Sent events (SSE) encoders.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class ServerDataConversionService implements DataConversionService {

	private final DataConversionService baseDataConversionService;

	/**
	 * <p>
	 * Creates a server data conversion service.
	 * </p>
	 *
	 * @param baseDataConversionService the base data conversion service
	 */
	public ServerDataConversionService(DataConversionService baseDataConversionService) {
		this.baseDataConversionService = baseDataConversionService;
	}

	@Override
	public MediaTypeConverter<ByteBuf> getConverter(String mediaType) throws MissingConverterException {
		return this.baseDataConversionService.getConverter(mediaType);
	}

	@Override
	public <T> InboundDataDecoder<T> createDecoder(InboundData<ByteBuf> rawData, String mediaType, Class<T> type) throws MissingConverterException {
		return this.baseDataConversionService.createDecoder(rawData, mediaType, type);
	}

	@Override
	public <T> InboundDataDecoder<T> createDecoder(InboundData<ByteBuf> rawData, String mediaType, Type type) throws MissingConverterException {
		return this.baseDataConversionService.createDecoder(rawData, mediaType, type);
	}

	@Override
	public <T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType) throws MissingConverterException {
		return this.baseDataConversionService.createEncoder(rawData, mediaType);
	}

	@Override
	public <T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType, Class<T> type) throws MissingConverterException {
		return this.baseDataConversionService.createEncoder(rawData, mediaType, type);
	}

	@Override
	public <T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType, Type type) throws MissingConverterException {
		return this.baseDataConversionService.createEncoder(rawData, mediaType, type);
	}

	@Override
	public BaseWeb2SocketExchange.Inbound createWebSocketDecodingInbound(BaseWebSocketExchange.Inbound inbound, String subprotocol) {
		return this.baseDataConversionService.createWebSocketDecodingInbound(inbound, subprotocol);
	}

	@Override
	public BaseWeb2SocketExchange.Outbound createWebSocketEncodingOutbound(BaseWebSocketExchange.Outbound outbound, String subprotocol) {
		return this.baseDataConversionService.createWebSocketEncodingOutbound(outbound, subprotocol);
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
	 * @throws MissingConverterException if there's no converter that can convert the specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType) throws MissingConverterException {
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
	 * @throws MissingConverterException if there's no converter that can convert the specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Class<T> type) throws MissingConverterException {
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
	 * @throws MissingConverterException if there's no converter that can convert the specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Type type) throws MissingConverterException {
		return new GenericSseEncoder<>(rawSse, this.getConverter(mediaType), type);
	}

	/**
	 * <p>
	 * A generic {@link WebResponseBody.SseEncoder} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
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
		public void from(BiConsumer<EventFactory<A>, OutboundData<Event<A>>> data) {
			this.rawSse.from((rawEvents, rawData) -> data.accept(
				eventConfigurer -> {
					GenericEvent<A> event = new GenericEvent<>();
					rawEvents.create(rawEvent -> {
						event.setRawEvent(rawEvent);
						eventConfigurer.accept(event);
					});
					return event;
				},
				new OutboundData<>() {

					@Override
					public <T extends Event<A>> void stream(Publisher<T> value) {
						rawData.stream(Flux.from(value)
							.cast(GenericEvent.class)
							.<ResponseBody.Sse.Event<ByteBuf>>map(GenericEvent::getRawEvent)
						);
					}
				}
			));
		}

		/**
		 * <p>
		 * Generic {@link WebResponseBody.SseEncoder.Event} implementation.
		 * </p>
		 *
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.12
		 *
		 * @param <B> the type of data to encode
		 */
		private class GenericEvent<B> implements WebResponseBody.SseEncoder.Event<B> {

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
}

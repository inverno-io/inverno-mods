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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.server.RequestData;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.http.server.ResponseData;
import io.inverno.mod.web.RequestDataDecoder;
import io.inverno.mod.web.ResponseDataEncoder;
import io.inverno.mod.web.WebResponseBody;
import io.inverno.mod.web.WebResponseBody.SseEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * The data conversion service is used to create payload decoders/encoders
 * from/to raw data based on a media type.
 * </p>
 *
 * <p>
 * It uses the list of {@link MediaTypeConverter} injected in the web module to
 * find the right converter to decode or encode a payload in a particular media
 * type.
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
	 * Creates a data conversion service with the specified list of media type
	 * converters.
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
	 * Returns the first media type converter that can convert the specified media
	 * type.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * @return a media type converter
	 * 
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public MediaTypeConverter<ByteBuf> getConverter(String mediaType) throws NoConverterException {
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
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public <T> RequestDataDecoder<T> createDecoder(RequestData<ByteBuf> rawData, String mediaType, Class<T> type) throws NoConverterException {
		return new GenericRequestBodyDecoder<>(rawData, this.getConverter(mediaType), type);
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
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public <T> RequestDataDecoder<T> createDecoder(RequestData<ByteBuf> rawData, String mediaType, Type type) throws NoConverterException {
		return new GenericRequestBodyDecoder<T>(rawData, this.getConverter(mediaType), type);
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
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType) throws NoConverterException {
		return new GenericRequestBodyEncoder<T>(rawData, this.getConverter(mediaType));
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
		return new GenericRequestBodyEncoder<T>(rawData, this.getConverter(mediaType), type);
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
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType, Type type) throws NoConverterException {
		return new GenericRequestBodyEncoder<T>(rawData, this.getConverter(mediaType), type);
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
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
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
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Class<T> type) throws NoConverterException {
		return new GenericSseEncoder<T>(rawSse, this.getConverter(mediaType), type);
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
	 * @throws NoConverterException if there's no converter that can convert the
	 *                              specified media type
	 */
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Type type) throws NoConverterException {
		return new GenericSseEncoder<T>(rawSse, this.getConverter(mediaType), type);
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
	private static class GenericRequestBodyDecoder<A> implements RequestDataDecoder<A> {

		private RequestData<ByteBuf> rawData;

		private MediaTypeConverter<ByteBuf> converter;

		private Type type;

		/**
		 * <p>
		 * Creates a generic request body decoder.
		 * </p>
		 * 
		 * @param rawData   the raw data consumer
		 * @param converter the converter
		 * @param type      a class of A
		 */
		public GenericRequestBodyDecoder(RequestData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
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
		public GenericRequestBodyDecoder(RequestData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Type type) {
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
	private static class GenericRequestBodyEncoder<A> implements ResponseDataEncoder<A> {

		private ResponseData<ByteBuf> rawData;

		private MediaTypeConverter<ByteBuf> converter;

		private Type type;

		/**
		 * <p>
		 * Creates a generic request body encoder.
		 * </p>
		 * 
		 * @param rawData   the raw data producer
		 * @param converter the converter
		 */
		public GenericRequestBodyEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter) {
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
		public GenericRequestBodyEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
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
		public GenericRequestBodyEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Type type) {
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
			if (this.type == null) {
				this.rawData.stream(Mono.fromSupplier(() -> this.converter.encode(value)));
			} else {
				this.rawData.stream(Mono.fromSupplier(() -> this.converter.encode(value, this.type)));
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

		private ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse;

		private MediaTypeConverter<ByteBuf> converter;

		private Type type;

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
				} else {
					this.rawEvent
							.stream(GenericSseEncoder.this.converter.encodeMany(value, GenericSseEncoder.this.type));
				}
			}

			@Override
			public <T extends B> void one(Mono<T> value) {
				if (GenericSseEncoder.this.type == null) {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeOne(value));
				} else {
					this.rawEvent
							.stream(GenericSseEncoder.this.converter.encodeOne(value, GenericSseEncoder.this.type));
				}
			}

			@Override
			public <T extends B> void value(T value) {
				if (GenericSseEncoder.this.type == null) {
					this.rawEvent.stream(Mono.fromSupplier(() -> GenericSseEncoder.this.converter.encode(value)));
				} else {
					this.rawEvent.stream(Mono.fromSupplier(
							() -> GenericSseEncoder.this.converter.encode(value, GenericSseEncoder.this.type)));
				}
			}
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

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
package io.winterframework.mod.web.internal;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.base.converter.ConverterException;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.http.base.BadRequestException;
import io.winterframework.mod.http.base.InternalServerErrorException;
import io.winterframework.mod.http.server.RequestData;
import io.winterframework.mod.http.server.ResponseBody;
import io.winterframework.mod.http.server.ResponseData;
import io.winterframework.mod.web.RequestDataDecoder;
import io.winterframework.mod.web.ResponseDataEncoder;
import io.winterframework.mod.web.WebResponseBody;
import io.winterframework.mod.web.WebResponseBody.SseEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
@Bean( visibility = Visibility.PRIVATE)
public class DataConversionService {
	
	private final Map<String, MediaTypeConverter<ByteBuf>> convertersCache;
	
	private final List<MediaTypeConverter<ByteBuf>> converters;

	public DataConversionService(List<MediaTypeConverter<ByteBuf>> converters) {
		this.converters = converters;
		this.convertersCache = new HashMap<>();
	}

	public MediaTypeConverter<ByteBuf> getConverter(String mediaType) {
		MediaTypeConverter<ByteBuf> result = this.convertersCache.get(mediaType);
		if(result == null && !this.convertersCache.containsKey(mediaType)) {
			for(MediaTypeConverter<ByteBuf> converter : this.converters) {
				if(converter.canConvert(mediaType)) {
					this.convertersCache.put(mediaType, converter);
					result = converter;
					break;
				}
			}
			if(result == null) {
				this.convertersCache.put(mediaType, null);
			}
		}
		if(result == null) {
			throw new InternalServerErrorException("No encoder found for media type: " + mediaType);
		}
		return result;
	}
	
	public <T> RequestDataDecoder<T> createDecoder(RequestData<ByteBuf> rawData, String mediaType, Class<T> type) {
		return new GenericRequestBodyDecoder<>(rawData, this.getConverter(mediaType), type);
	}
	
	public <T> RequestDataDecoder<T> createDecoder(RequestData<ByteBuf> rawData, String mediaType, Type type) {
		return new GenericRequestBodyDecoder<T>(rawData, this.getConverter(mediaType), type);
	}
	
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType) {
		return new GenericRequestBodyEncoder<T>(rawData, this.getConverter(mediaType));
	}
	
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType, Class<T> type) {
		return new GenericRequestBodyEncoder<T>(rawData, this.getConverter(mediaType), type);
	}
	
	public <T> ResponseDataEncoder<T> createEncoder(ResponseData<ByteBuf> rawData, String mediaType, Type type) {
		return new GenericRequestBodyEncoder<T>(rawData, this.getConverter(mediaType), type);
	}

	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType) {
		return new GenericSseEncoder<>(rawSse, this.getConverter(mediaType));
	}
	
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Class<T> type) {
		return new GenericSseEncoder<T>(rawSse, this.getConverter(mediaType), type);
	}
	
	public <T> WebResponseBody.SseEncoder<T> createSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, String mediaType, Type type) {
		return new GenericSseEncoder<T>(rawSse, this.getConverter(mediaType), type);
	}
	
	private static class GenericRequestBodyDecoder<A> implements RequestDataDecoder<A> {

		private RequestData<ByteBuf> rawData;
		
		private MediaTypeConverter<ByteBuf> converter;
		
		private Type type;
		
		public GenericRequestBodyDecoder(RequestData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawData, converter, (Type)type);
		}
		
		public GenericRequestBodyDecoder(RequestData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Type type) {
			this.rawData = rawData;
			this.converter = converter;
			this.type = type;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Publisher<A> stream() {
			return (Publisher<A>) this.converter.decodeMany(this.rawData.stream(), this.type).onErrorMap(ConverterException.class, ex -> new BadRequestException(ex));
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Mono<A> one() {
			return (Mono<A>) this.converter.decodeOne(this.rawData.stream(), this.type).onErrorMap(ConverterException.class, ex -> new BadRequestException("dsfdgdfgf", ex));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Flux<A> many() {
			return (Flux<A>) this.converter.decodeMany(this.rawData.stream(), this.type).onErrorMap(ConverterException.class, ex -> new BadRequestException(ex));
		}
	}
	
	private static class GenericRequestBodyEncoder<A> implements ResponseDataEncoder<A> {

		private ResponseData<ByteBuf> rawData;
		
		private MediaTypeConverter<ByteBuf> converter;
		
		private Type type;
		
		public GenericRequestBodyEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter) {
			this(rawData, converter, null);
		}
		
		public GenericRequestBodyEncoder(ResponseData<ByteBuf> rawData, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawData, converter, (Type)type);
		}
		
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
			if(this.type == null) {
				this.rawData.stream(this.converter.encodeMany(value));
			}
			else {
				this.rawData.stream(this.converter.encodeMany(value, this.type));
			}
		}

		@Override
		public <T extends A> void one(Mono<T> value) {
			if(this.type == null) {
				this.rawData.stream(this.converter.encodeOne(value));
			}
			else {
				this.rawData.stream(this.converter.encodeOne(value, this.type));
			}
		}
		
		@Override
		public <T extends A> void value(T value) {
			if(this.type == null) {
				this.rawData.stream(Mono.fromSupplier(() -> this.converter.encode(value)));
			}
			else {
				this.rawData.stream(Mono.fromSupplier(() -> this.converter.encode(value, this.type)));
			}
		}
	}
	
	private static class GenericSseEncoder<A> implements WebResponseBody.SseEncoder<A> {

		private ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse;
		
		private MediaTypeConverter<ByteBuf> converter;
		
		private Type type;
		
		public GenericSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, MediaTypeConverter<ByteBuf> converter) {
			this(rawSse, converter, null);
		}
		
		public GenericSseEncoder(ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> rawSse, MediaTypeConverter<ByteBuf> converter, Class<A> type) {
			this(rawSse, converter, (Type)type);
		}
		
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
							rawData.stream(Flux.from(value).cast(GenericEvent.class).<ResponseBody.Sse.Event<ByteBuf>>map(GenericEvent::getRawEvent));
						}
					}
				);
			});
		}
		
		private class GenericEvent<B> implements SseEncoder.Event<B> {

			private ResponseBody.Sse.Event<ByteBuf> rawEvent;
			
			public void setRawEvent(ResponseBody.Sse.Event<ByteBuf> rawEvent) {
				this.rawEvent = rawEvent;
			}
			
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
				if(GenericSseEncoder.this.type == null) {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeMany(value));
				}
				else {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeMany(value, GenericSseEncoder.this.type));
				}
			}

			@Override
			public <T extends B> void one(Mono<T> value) {
				if(GenericSseEncoder.this.type == null) {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeOne(value));
				}
				else {
					this.rawEvent.stream(GenericSseEncoder.this.converter.encodeOne(value, GenericSseEncoder.this.type));
				}
			}
			
			@Override
			public <T extends B> void value(T value) {
				if(GenericSseEncoder.this.type == null) {
					this.rawEvent.stream(Mono.fromSupplier(() -> GenericSseEncoder.this.converter.encode(value)));
				}
				else {
					this.rawEvent.stream(Mono.fromSupplier(() -> GenericSseEncoder.this.converter.encode(value, GenericSseEncoder.this.type)));
				}
			}
		}
	}
	
	@Bean( name = "MediaTypeConverters")
	public static interface MediaTypeConvertersSocket extends Supplier<List<MediaTypeConverter<ByteBuf>>> {}
}

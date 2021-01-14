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
package io.winterframework.mod.web.router.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.router.WebRequestBody;
import io.winterframework.mod.web.router.WebResponseBody;
import io.winterframework.mod.web.server.RequestBody;
import io.winterframework.mod.web.server.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
@Bean( visibility = Visibility.PRIVATE)
public class BodyConversionService {
	
	private Map<String, MediaTypeConverter<ByteBuf, Object>> converters;

	public BodyConversionService(List<MediaTypeConverter<ByteBuf, Object>> converters) {
		this.converters = new HashMap<>();
		
		for(MediaTypeConverter<ByteBuf, Object> converter : converters) {
			for(String supportedMediaType : converter.getSupportedMediaTypes()) {
				supportedMediaType = supportedMediaType.toLowerCase();
				// TODO at some point this is an issue in Spring as well, we should fix this in winter
				// provide annotation for sorting at compile time and be able to inject maps as well 
				// - annotations defined on the beans with some meta data
				// - annotations defined on multiple bean socket to specify sorting for list, array or sets
				// - we can also group by key to inject a map => new multi socket type
				// - this is a bit tricky as for selector when it comes to the injection of list along with single values 
				MediaTypeConverter<ByteBuf, Object> previousConverter = this.converters.put(supportedMediaType, converter);
				if(previousConverter != null) {
					throw new IllegalStateException("Multiple converters found for media type " + supportedMediaType + ": " + previousConverter.toString() + ", " + converter.toString());
				}
			}
		}
	}

	public Optional<MediaTypeConverter<ByteBuf, Object>> getConverter(String mediaType) {
		return Optional.ofNullable(this.converters.get(mediaType));
	}
	
	public <T> WebResponseBody.Encoder createEncoder(ResponseBody.Raw raw, String mediaType) {
		return this.getConverter(mediaType).map(converter -> new GenericRequestBodyEncoder(raw, converter)).orElseThrow(() -> new InternalServerErrorException("No encoder found for media type: " + mediaType));
	}
	
	public <T> WebRequestBody.Decoder<T> createDecoder(RequestBody.Raw raw, String mediaType, Class<T> type) {
		return this.getConverter(mediaType).map(converter -> new GenericRequestBodyDecoder<>(raw, converter, type)).orElseThrow(() -> new InternalServerErrorException("No decoder found for media type: " + mediaType));
	}
	
	private static class GenericRequestBodyDecoder<A> implements WebRequestBody.Decoder<A> {

		private RequestBody.Raw raw;
		
		private MediaTypeConverter<ByteBuf, Object> converter;
		
		private Class<A> type;
		
		public GenericRequestBodyDecoder(RequestBody.Raw raw, MediaTypeConverter<ByteBuf, Object> converter, Class<A> type) {
			this.raw = raw;
			this.converter = converter;
			this.type = type;
		}
		
		@Override
		public Mono<A> one() {
			return this.converter.decodeOne(this.raw.data(), this.type);
		}

		@Override
		public Flux<A> many() {
			return this.converter.decodeMany(this.raw.data(), this.type);
		}
	}
	
	private static class GenericRequestBodyEncoder implements WebResponseBody.Encoder {

		private ResponseBody.Raw raw;
		
		private MediaTypeConverter<ByteBuf, Object> converter;
		
		public GenericRequestBodyEncoder(ResponseBody.Raw raw, MediaTypeConverter<ByteBuf, Object> converter) {
			this.raw = raw;
			this.converter = converter;
		}

		@Override
		public <A> void data(Flux<A> data) {
			this.raw.data(this.converter.encodeMany(data));
		}

		@Override
		public <A> void data(Mono<A> data) {
			this.raw.data(this.converter.encodeOne(data));
		}
		
		@Override
		public <A> void data(A data) {
			this.raw.data(Mono.fromSupplier(() -> this.converter.encode(data)));
		}
	}
	
	@Bean( name = "MediaTypeConverters")
	public static interface MediaTypeConvertersSocket extends Supplier<List<MediaTypeConverter<ByteBuf, Object>>> {}
}

/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.boot.converter;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.ReactiveConverter;
import io.inverno.mod.base.resource.MediaTypes;
import java.lang.reflect.Type;
import java.util.Iterator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * String {@code application/json} media type converter.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see MediaTypeConverter
 */
@Bean( name = "jsonStringMediaTypeConverter" )
public class JsonStringMediaTypeConverter implements @Provide MediaTypeConverter<String> {

	private static final String JSON_ARRAY_START = "[";
	private static final String JSON_ARRAY_SEPARATOR = ",";
	private static final String JSON_ARRAY_END = "]";

	private static final Mono<String> JSON_ARRAY_START_MONO = Mono.just(JSON_ARRAY_START);
	
	private final ReactiveConverter<String, Object> jsonStringConverter;
	
	/**
	 * <p>
	 * Create an {@code application/json} media type converter.
	 * </p>
	 * 
	 * @param jsonStringConverter the underlying JSON String converter
	 */
	public JsonStringMediaTypeConverter(ReactiveConverter<String, Object> jsonStringConverter) {
		this.jsonStringConverter = jsonStringConverter;
	}

	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_JSON);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Class<T> type) {
		return jsonStringConverter.decodeOne(value, type);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Type type) {
		return jsonStringConverter.decodeOne(value, type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Class<T> type) {
		return jsonStringConverter.decodeMany(value, type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Type type) {
		return jsonStringConverter.decodeMany(value, type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value) {
		return jsonStringConverter.encodeOne(value);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Class<T> type) {
		return jsonStringConverter.encodeOne(value, type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Type type) {
		return jsonStringConverter.encodeOne(value, type);
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<String>)this.jsonStringConverter.encodeMany(value)).zipWithIterable(new Separators(), (element, separator) -> element + separator)).concatWithValues(JSON_ARRAY_END);
	}
	
	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Class<T> type) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<String>)this.jsonStringConverter.encodeMany(value, type)).zipWithIterable(new Separators(), (element, separator) -> element + separator)).concatWithValues(JSON_ARRAY_END);
	}
	
	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Type type) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<String>)this.jsonStringConverter.encodeMany(value, type)).zipWithIterable(new Separators(), (element, separator) -> element + separator)).concatWithValues(JSON_ARRAY_END);
	}

	@Override
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		return jsonStringConverter.decode(value, type);
	}

	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
		return jsonStringConverter.decode(value, type);
	}

	@Override
	public <T> String encode(T value) throws ConverterException {
		return jsonStringConverter.encode(value);
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		return jsonStringConverter.encode(value, type);
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		return jsonStringConverter.encode(value, type);
	}

	private static class Separators implements Iterable<String> {
		
		@Override
		public Iterator<String> iterator() {
			
			return new Iterator<String>() {
				
				private String current = "";

				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public String next() {
					String next = this.current;
					this.current = JSON_ARRAY_SEPARATOR;
					return next;
				}
			};
		}
	}
}

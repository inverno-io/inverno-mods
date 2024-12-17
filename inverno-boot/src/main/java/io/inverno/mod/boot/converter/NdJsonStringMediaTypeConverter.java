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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * String {@code application/x-ndjson} media type converter as defined by <a href="http://ndjson.org/">Newline Delimited JSON</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see MediaTypeConverter
 */
@Bean( name = "ndJsonStringMediaTypeConverter")
public class NdJsonStringMediaTypeConverter implements @Provide MediaTypeConverter<String> {

	private final ReactiveConverter<String, Object> jsonStringConverter;
	
	/**
	 * <p>
	 * Creates an {@code application/x-ndjson} media type converter.
	 * </p>
	 * 
	 * @param jsonStringConverter the underlying JSON String converter
	 */
	public NdJsonStringMediaTypeConverter(ReactiveConverter<String, Object> jsonStringConverter) {
		this.jsonStringConverter = jsonStringConverter;
	}

	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_X_NDJSON);
	}
	
	@Override
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		return this.jsonStringConverter.decode(value, type);
	}

	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
		return this.jsonStringConverter.decode(value, type);
	}

	@Override
	public <T> String encode(T value) throws ConverterException {
		return this.jsonStringConverter.encode(value) + "\n";
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		return this.jsonStringConverter.encode(value, type) + "\n";
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		return this.jsonStringConverter.encode(value, type) + "\n";
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Class<T> type) {
		return this.jsonStringConverter.decodeOne(value, type);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Type type) {
		return this.jsonStringConverter.decodeOne(value, type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Class<T> type) {
		return this.jsonStringConverter.decodeMany(value, type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Type type) {
		return this.jsonStringConverter.decodeMany(value, type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value) {
		return ((Mono<String>)this.jsonStringConverter.encodeOne(value)).map(v -> v + "\n");
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Class<T> type) {
		return ((Mono<String>)this.jsonStringConverter.encodeOne(value, type)).map(v -> v + "\n");
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Type type) {
		return ((Mono<String>)this.jsonStringConverter.encodeOne(value, type)).map(v -> v + "\n");
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value) {
		return ((Flux<String>)this.jsonStringConverter.encodeMany(value)).map(v -> v + "\n");
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Class<T> type) {
		return ((Flux<String>)this.jsonStringConverter.encodeMany(value, type)).map(v -> v + "\n");
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Type type) {
		return ((Flux<String>)this.jsonStringConverter.encodeMany(value, type)).map(v -> v + "\n");
	}
}

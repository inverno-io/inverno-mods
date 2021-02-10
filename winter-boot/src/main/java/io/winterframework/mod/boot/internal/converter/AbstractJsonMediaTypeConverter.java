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
package io.winterframework.mod.boot.internal.converter;

import java.lang.reflect.Type;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.converter.ConverterException;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.base.converter.ReactiveConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
abstract class AbstractJsonMediaTypeConverter implements MediaTypeConverter<ByteBuf> {

	private ReactiveConverter<ByteBuf, Object> jsonByteBufConverter;
	
	public AbstractJsonMediaTypeConverter(ReactiveConverter<ByteBuf, Object> jsonByteBufConverter) {
		this.jsonByteBufConverter = jsonByteBufConverter;
	}
	
	@Override
	public <T> Mono<T> decodeOne(Publisher<ByteBuf> value, Class<T> type) {
		return this.jsonByteBufConverter.decodeOne(value, type);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<ByteBuf> value, Type type) {
		return this.jsonByteBufConverter.decodeOne(value, type);
	}
	
	@Override
	public <T> Flux<T> decodeMany(Publisher<ByteBuf> value, Class<T> type) {
		return this.jsonByteBufConverter.decodeMany(value, type);
	}
	
	@Override
	public <T> Flux<T> decodeMany(Publisher<ByteBuf> value, Type type) {
		return this.jsonByteBufConverter.decodeMany(value, type);
	}
	
	@Override
	public <T> T decode(ByteBuf value, Class<T> type) {
		return this.jsonByteBufConverter.decode(value, type);
	}
	
	@Override
	public <T> T decode(ByteBuf value, Type type) throws ConverterException {
		return this.jsonByteBufConverter.decode(value, type);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value) {
		return this.jsonByteBufConverter.encodeOne(value);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value, Class<T> type) {
		return this.jsonByteBufConverter.encodeOne(value, type);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value, Type type) {
		return this.jsonByteBufConverter.encodeOne(value, type);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value) {
		return this.jsonByteBufConverter.encodeMany(value);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Class<T> type) {
		return this.jsonByteBufConverter.encodeMany(value, type);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Type type) {
		return this.jsonByteBufConverter.encodeMany(value, type);
	}
	
	@Override
	public <T> ByteBuf encode(T value) {
		return this.jsonByteBufConverter.encode(value);
	}
	
	@Override
	public <T> ByteBuf encode(T value, Class<T> type) throws ConverterException {
		return this.jsonByteBufConverter.encode(value, type);
	}
	
	@Override
	public <T> ByteBuf encode(T value, Type type) throws ConverterException {
		return this.jsonByteBufConverter.encode(value, type);
	}

}

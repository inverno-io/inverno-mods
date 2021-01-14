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

import java.util.List;
import java.util.Set;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.converter.Converter;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
abstract class AbstractJsonMediaTypeConverter implements MediaTypeConverter<ByteBuf, Object> {

	private Converter<ByteBuf, Object> jsonByteBufConverter;
	
	public AbstractJsonMediaTypeConverter(Converter<ByteBuf, Object> jsonByteBufConverter) {
		this.jsonByteBufConverter = jsonByteBufConverter;
	}
	
	@Override
	public <T> Mono<T> decodeOne(Publisher<ByteBuf> data, Class<T> type) {
		return this.jsonByteBufConverter.decodeOne(data, type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<ByteBuf> data, Class<T> type) {
		return this.jsonByteBufConverter.decodeMany(data, type);
	}

	@Override
	public <T> T decode(ByteBuf data, Class<T> type) {
		return this.jsonByteBufConverter.decode(data, type);
	}

	@Override
	public <T> List<T> decodeToList(ByteBuf data, Class<T> type) {
		return this.jsonByteBufConverter.decodeToList(data, type);
	}

	@Override
	public <T> Set<T> decodeToSet(ByteBuf data, Class<T> type) {
		return this.jsonByteBufConverter.decodeToSet(data, type);
	}

	@Override
	public <T> T[] decodeToArray(ByteBuf data, Class<T> type) {
		return this.jsonByteBufConverter.decodeToArray(data, type);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> data) {
		return this.jsonByteBufConverter.encodeOne(data);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> data) {
		return this.jsonByteBufConverter.encodeMany(data);
	}

	@Override
	public <T> ByteBuf encode(T data) {
		return this.jsonByteBufConverter.encode(data);
	}

	@Override
	public <T> ByteBuf encodeList(List<T> data) {
		return this.jsonByteBufConverter.encodeList(data);
	}

	@Override
	public <T> ByteBuf encodeSet(Set<T> data) {
		return this.jsonByteBufConverter.encodeSet(data);
	}

	@Override
	public <T> ByteBuf encodeArray(T[] data) {
		return this.jsonByteBufConverter.encodeArray(data);
	}

}

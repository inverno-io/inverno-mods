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

import java.util.Set;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Provide;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.base.converter.ReactiveConverter;
import io.winterframework.mod.base.resource.MediaTypes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
@Bean( name = "ndjsonMediaTypeConverter")
public class NdJsonMediaTypeConverter extends AbstractJsonMediaTypeConverter implements @Provide MediaTypeConverter<ByteBuf> {

	private static final ByteBuf NEW_LINE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(new byte[] {'\n'}));
	
	public NdJsonMediaTypeConverter(ReactiveConverter<ByteBuf, Object> jsonByteBufConverter) {
		super(jsonByteBufConverter);
	}
	
	@Override
	public Set<String> getSupportedMediaTypes() {
		return Set.of(MediaTypes.APPLICATION_X_NDJSON);
	}

	private ByteBuf concatNewLine(ByteBuf data) {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(data, NEW_LINE));
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> data) {
		return ((Flux<ByteBuf>)super.encodeMany(data)).map(this::concatNewLine);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> data) {
		return ((Mono<ByteBuf>)super.encodeOne(data)).map(this::concatNewLine);
	}
	
	@Override
	public <T> ByteBuf encode(T data) {
		return this.concatNewLine(super.encode(data));
	}
	
	/*@Override
	public <T> ByteBuf encodeArray(T[] data) {
		return this.concatNewLine(super.encodeArray(data));
	}
	
	@Override
	public <T> ByteBuf encodeList(List<T> data) {
		return this.concatNewLine(super.encodeList(data));
	}
	
	@Override
	public <T> ByteBuf encodeSet(Set<T> data) {
		return this.concatNewLine(super.encodeSet(data));
	}*/
}

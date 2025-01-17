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
package io.inverno.mod.boot.converter;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.ReactiveConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * {@code ByteBuf} {@code application/x-ndjson} media type converter as defined by <a href="http://ndjson.org/">Newline Delimited JSON</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see MediaTypeConverter
 */
@Bean( name = "ndJsonByteBufMediaTypeConverter")
public class NdJsonByteBufMediaTypeConverter extends AbstractJsonByteBufMediaTypeConverter implements @Provide MediaTypeConverter<ByteBuf> {

	private static final ByteBuf NEW_LINE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(new byte[] {'\n'}));
	
	/**
	 * <p>
	 * Creates an {@code application/x-ndjson} media type converter.
	 * </p>
	 * 
	 * @param jsonByteBufConverter the underlying JSON ByteBuf converter
	 */
	public NdJsonByteBufMediaTypeConverter(ReactiveConverter<ByteBuf, Object> jsonByteBufConverter) {
		super(jsonByteBufConverter);
	}
	
	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_X_NDJSON);
	}

	private ByteBuf concatNewLine(ByteBuf value) {
		return Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value, NEW_LINE));
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value) {
		return ((Mono<ByteBuf>)super.encodeOne(value)).map(this::concatNewLine);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value, Class<T> type) {
		return ((Mono<ByteBuf>)super.encodeOne(value, type)).map(this::concatNewLine);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeOne(Mono<T> value, Type type) {
		return ((Mono<ByteBuf>)super.encodeOne(value, type)).map(this::concatNewLine);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value) {
		return ((Flux<ByteBuf>)super.encodeMany(value)).map(this::concatNewLine);
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Class<T> type) {
		return ((Flux<ByteBuf>)super.encodeMany(value, type)).map(this::concatNewLine);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Type type) {
		return ((Flux<ByteBuf>)super.encodeMany(value, type)).map(this::concatNewLine);
	}
	
	@Override
	public <T> ByteBuf encode(T value) {
		return this.concatNewLine(super.encode(value));
	}
	
	@Override
	public <T> ByteBuf encode(T value, Class<T> type) throws ConverterException {
		return this.concatNewLine(super.encode(value, type));
	}
	
	@Override
	public <T> ByteBuf encode(T value, Type type) throws ConverterException {
		return this.concatNewLine(super.encode(value, type));
	}
}

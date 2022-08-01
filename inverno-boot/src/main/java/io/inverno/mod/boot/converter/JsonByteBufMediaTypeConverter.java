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
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.ReactiveConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Type;
import java.util.Iterator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * ByteBuf {@code application/json} media type converter.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see MediaTypeConverter
 */
@Bean( name = "jsonByteBufMediaTypeConverter" )
public class JsonByteBufMediaTypeConverter extends AbstractJsonByteBufMediaTypeConverter implements @Provide MediaTypeConverter<ByteBuf> {
	
	private static final ByteBuf JSON_ARRAY_START = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[] {'['}));
	private static final ByteBuf JSON_ARRAY_SEPARATOR = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[] {','}));
	private static final ByteBuf JSON_ARRAY_END = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[] {']'}));

	private static final Mono<ByteBuf> JSON_ARRAY_START_MONO = Mono.fromSupplier(() -> JSON_ARRAY_START.duplicate());
	
	/**
	 * <p>
	 * Create an {@code application/json} media type converter.
	 * </p>
	 * 
	 * @param jsonByteBufConverter the underlying JSON ByteBuf converter
	 */
	public JsonByteBufMediaTypeConverter(ReactiveConverter<ByteBuf, Object> jsonByteBufConverter) {
		super(jsonByteBufConverter);
	}
	
	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_JSON);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<ByteBuf>)super.encodeMany(value)).zipWithIterable(new Separators(), (element, separator) -> Unpooled.wrappedBuffer(separator, element))).concatWithValues(JSON_ARRAY_END.duplicate());
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Class<T> type) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<ByteBuf>)super.encodeMany(value, type)).zipWithIterable(new Separators(), (element, separator) -> Unpooled.wrappedBuffer(separator, element))).concatWithValues(JSON_ARRAY_END.duplicate());
	}
	
	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> value, Type type) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<ByteBuf>)super.encodeMany(value, type)).zipWithIterable(new Separators(), (element, separator) -> Unpooled.wrappedBuffer(separator, element))).concatWithValues(JSON_ARRAY_END.duplicate());
	}
	
	private static class Separators implements Iterable<ByteBuf> {
		
		@Override
		public Iterator<ByteBuf> iterator() {
			
			return new Iterator<ByteBuf>() {
				
				private ByteBuf current = Unpooled.EMPTY_BUFFER;

				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public ByteBuf next() {
					ByteBuf next = this.current;
					this.current = JSON_ARRAY_SEPARATOR;
					return next;
				}
			};
		}
	}
}

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

import java.util.Iterator;
import java.util.Set;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Provide;
import io.winterframework.mod.base.converter.ReactiveConverter;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.base.resource.MediaTypes;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
@Bean( name = "jsonMediaTypeConverter")
public class JsonMediaTypeConverter extends AbstractJsonMediaTypeConverter implements @Provide MediaTypeConverter<ByteBuf> {
	
	private static final ByteBuf JSON_ARRAY_START = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[] {'['}));
	private static final ByteBuf JSON_ARRAY_SEPARATOR = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[] {','}));
	private static final ByteBuf JSON_ARRAY_END = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(new byte[] {']'}));
	
	public JsonMediaTypeConverter(ReactiveConverter<ByteBuf, Object> jsonByteBufConverter) {
		super(jsonByteBufConverter);
	}
	
	@Override
	public Set<String> getSupportedMediaTypes() {
		return Set.of(MediaTypes.APPLICATION_JSON);
	}

	@Override
	public <T> Publisher<ByteBuf> encodeMany(Flux<T> data) {
		Iterable<ByteBuf> separators = new Iterable<ByteBuf>() {
			
			@Override
			public Iterator<ByteBuf> iterator() {
				
				return new Iterator<ByteBuf>() {
					
					private ByteBuf current = JSON_ARRAY_START;

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
		};
		return ((Flux<ByteBuf>)super.encodeMany(data)).zipWithIterable(separators, (value, separator) -> Unpooled.wrappedBuffer(separator, value)).concatWithValues(JSON_ARRAY_END.duplicate());
	}
}

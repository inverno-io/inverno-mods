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
package io.inverno.mod.http.client.internal.multipart;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link Part} implementation for representing part's with string data.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class StringPart<T extends CharSequence> extends AbstractDataPart<T> {

	/**
	 * <p>
	 * Creates a string part.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public StringPart(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(headerService, parameterConverter);
	}

	@Override
	public <U extends T> void stream(Publisher<U> value) throws IllegalStateException {
		Publisher<ByteBuf> data;
		if(value instanceof Mono) {
			data = ((Mono<U>)value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
		}
		else {
			data = Flux.from(value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
		}
		this.setData(data);
	}

	@Override
	public <U extends T> void value(U value) throws IllegalStateException {
		this.setData(value != null ? Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value, Charsets.DEFAULT))) : Mono.empty());
	}
}
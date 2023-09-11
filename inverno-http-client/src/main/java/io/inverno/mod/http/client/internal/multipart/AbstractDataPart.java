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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base {@link Part} implementation for sending data.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the type of data sent in the part
 */
public abstract class AbstractDataPart<A> extends AbstractPart<A> {
	
	private Publisher<ByteBuf> data;

	/**
	 * <p>
	 * Creates a data part.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	protected AbstractDataPart(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(headerService, parameterConverter);
	}
	
	/**
	 * <p>
	 * Creates a data part.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headers            the part headers
	 */
	protected AbstractDataPart(HeaderService headerService, ObjectConverter<String> parameterConverter, PartHeaders headers) {
		super(headerService, parameterConverter, headers);
	}
	
	/**
	 * <p>
	 * Sets the part data.
	 * </p>
	 * 
	 * @param data a data publisher
	 */
	protected final void setData(Publisher<ByteBuf> data) {
		this.data = data;
	}

	/**
	 * <p>
	 * Returns the part's data publisher.
	 * </p>
	 * 
	 * @return a data publisher
	 */
	public Publisher<ByteBuf> getData() {
		return data != null ? data : Mono.empty();
	}
}

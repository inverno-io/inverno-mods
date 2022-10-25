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
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public abstract class AbstractDataPart<A> extends AbstractPart<A> {
	
	private Publisher<ByteBuf> data;

	protected AbstractDataPart(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(headerService, parameterConverter);
	}
	
	protected AbstractDataPart(HeaderService headerService, ObjectConverter<String> parameterConverter, PartHeaders headers) {
		super(headerService, parameterConverter, headers);
	}
	
	protected final void setData(Publisher<ByteBuf> data) {
		this.data = data;
	}

	public Publisher<ByteBuf> getData() {
		return data != null ? data : Mono.empty();
	}
}

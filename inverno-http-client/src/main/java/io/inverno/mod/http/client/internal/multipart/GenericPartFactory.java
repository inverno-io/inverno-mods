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

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Part;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class GenericPartFactory implements Part.Factory {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;

	public GenericPartFactory(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	@Override
	public Part<ByteBuf> raw(Consumer<Part<ByteBuf>> partConfigurer) {
		Part<ByteBuf> part = new RawPart(this.headerService, this.parameterConverter);
		partConfigurer.accept(part);
		return part;
	}

	@Override
	public <T extends CharSequence> Part<T> string(Consumer<Part<T>> partConfigurer) {
		Part<T> part = new StringPart<>(this.headerService, this.parameterConverter);
		partConfigurer.accept(part);
		return part;
	}

	@Override
	public Part<Resource> resource(Consumer<Part<Resource>> partConfigurer) {
		Part<Resource> part = new ResourcePart(this.headerService, this.parameterConverter);
		partConfigurer.accept(part);
		return part;
	}
}

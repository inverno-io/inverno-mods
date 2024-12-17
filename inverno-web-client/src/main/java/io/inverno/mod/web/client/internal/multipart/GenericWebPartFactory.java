/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.client.internal.multipart;

import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.client.WebPart;
import io.inverno.mod.web.client.WebPartFactory;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link WebPartFactory} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebPartFactory implements WebPartFactory {

	private final DataConversionService dataConversionService;
	private final Part.Factory partFactory;

	/**
	 * <p>
	 * Creates a generic Web part factory.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param partFactory           the originating part factory
	 */
	public GenericWebPartFactory(DataConversionService dataConversionService, Part.Factory partFactory) {
		this.dataConversionService = dataConversionService;
		this.partFactory = partFactory;
	}

	@Override
	public Part<ByteBuf> raw(Consumer<Part<ByteBuf>> partConfigurer) {
		return this.partFactory.raw(partConfigurer);
	}

	@Override
	public <T extends CharSequence> Part<T> string(Consumer<Part<T>> partConfigurer) {
		return this.partFactory.string(partConfigurer);
	}

	@Override
	public Part<Resource> resource(Consumer<Part<Resource>> partConfigurer) {
		return this.partFactory.resource(partConfigurer);
	}

	@Override
	public <T> WebPart<T> encoded(Consumer<WebPart<T>> partConfigurer) {
		WebPart<T> part = new GenericWebPart<>(this.dataConversionService, this.partFactory.raw(ign -> {}));
		partConfigurer.accept(part);
		return part;
	}

	@Override
	public <T> WebPart<T> encoded(Consumer<WebPart<T>> partConfigurer, Class<T> type) {
		return this.encoded(partConfigurer, (Type)type);
	}

	@Override
	public <T> WebPart<T> encoded(Consumer<WebPart<T>> partConfigurer, Type type) {
		WebPart<T> part = new GenericWebPart<>(this.dataConversionService, this.partFactory.raw(ign -> {}), type);
		partConfigurer.accept(part);
		return part;
	}
}

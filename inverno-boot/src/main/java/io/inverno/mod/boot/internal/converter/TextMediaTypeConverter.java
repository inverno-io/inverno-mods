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
package io.inverno.mod.boot.internal.converter;

import io.netty.buffer.ByteBuf;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ByteBufConverter;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.MediaTypes;

/**
 * <p>
 * ByteBuf text/plain media type converter.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see MediaTypeConverter
 */
@Bean( name = "textPlainMediaTypeConverter" )
public class TextMediaTypeConverter extends ByteBufConverter implements @Provide MediaTypeConverter<ByteBuf> {

	public TextMediaTypeConverter(ObjectConverter<String> parameterConverter) {
		super(parameterConverter);
	}
	
	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.TEXT_PLAIN);
	}
}

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
package io.inverno.mod.security.jose.internal.converter;

import io.inverno.mod.base.converter.MediaTypeConverter;
import java.util.Optional;

/**
 * <p>
 * A data conversion service is used to resolve media type converters from JOSE header content types in order to serialize/deserialize JOSE object payload.
 * </p>
 * 
 * <p>
 * To keep messages compact in common situations, the {@code application/} prefix of media types can be omitted when no other '/' appears in the value, implementations can handle this optimization by
 * relying on normalized media types.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface DataConversionService {
	
	/**
	 * <p>
	 * Returns a media type converter for converting the specified media type.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return an optional containing the matching media type converter or an empty optional
	 */
	Optional<MediaTypeConverter<String>> getConverter(String mediaType);
}

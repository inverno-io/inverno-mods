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
package io.inverno.mod.security.jose.internal.jwt;

import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import java.util.Optional;

/**
 * <p>
 * A {@code DataConversionService} wrapper that only provides an {@code application/json} media type converter.
 * </p>
 * 
 * <p>
 * This implementation should be used exclusively within the scope of JWT for which payloads are always JWT Claims Set serialized as JSON.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class JWTDataConversionService implements DataConversionService {

	private final DataConversionService dataConversionService;
	
	private Optional<MediaTypeConverter<String>> jsonMediaTypeConverter;

	/**
	 * <p>
	 * Creates a JWT data conversion service that wraps the specified data conversion service.
	 * </p>
	 * 
	 * @param dataConversionService a data conversion service
	 */
	public JWTDataConversionService(DataConversionService dataConversionService) {
		this.dataConversionService = dataConversionService;
	}
	
	@Override
	public Optional<MediaTypeConverter<String>> getConverter(String mediaType) {
		String normalizedMediaType = MediaTypes.normalizeApplicationMediaType(mediaType);
		if(!normalizedMediaType.equals(MediaTypes.APPLICATION_JSON)) {
			// It must be application/json so we can rely on a JSON media type converter, this is basically hardcoded in the JWTJWSBuilder and JWTJWEBuilder but we never know
			// for nested JWT, the correct way to do it is to create a JWS or a JWE with content type JWT that embeds the compact or JSON representation of a previously created JWT
			return Optional.empty();
		}
		if(this.jsonMediaTypeConverter == null) {
			this.jsonMediaTypeConverter = this.dataConversionService.getConverter(normalizedMediaType);
		}
		return this.jsonMediaTypeConverter;
	}
}

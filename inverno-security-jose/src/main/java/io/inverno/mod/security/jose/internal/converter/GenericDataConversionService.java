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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>
 * Generic data conversion service which relies on a set of {@link MediaTypeConverter}.
 * </p>
 * 
 * <p>
 * Media type converters are injected when building the JOSE module.
 * </p>
 * 
 * <p>
 * The service also exposes built-in media type converters to support JOSE media types: {@code application/jose}, {@code application/jose+json}, {@code application/jwk+json},
 * {@code application/jwk-set+json} and {@code application/jwt} as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-9.2.1">RFC7515 Section 9.2.1</a>,
 * <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-8.5.1">RFC7517 Section 8.5.1</a> and
 * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-10.3.1">RFC7519 Section 10.3.1</a>
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "dataConversionService", visibility = Bean.Visibility.PRIVATE )
public class GenericDataConversionService implements DataConversionService {
	
	private final List<MediaTypeConverter<String>> converters;
	private final JWKService jwkService;
	private final ObjectMapper mapper;
	
	private final Map<String, MediaTypeConverter<String>> convertersCache;
	
	private JWEService jweService;
	private JWSService jwsService;
	private JWTService jwtService;
	
	/**
	 * <p>
	 * Creates a generic data conversion service.
	 * </p>
	 *
	 * @param converters the set of media type converters
	 * @param jwkService the JWK service
	 * @param mapper     an object mapper
	 */
	public GenericDataConversionService(List<MediaTypeConverter<String>> converters, JWKService jwkService, ObjectMapper mapper) {
		this.converters = converters;
		this.jwkService = jwkService;
		this.mapper = mapper;
		this.convertersCache = new HashMap<>();
	}
	
	/**
	 * <p>
	 * Injects the JWE service into the data conversion service.
	 * </p>
	 * 
	 * <p>
	 * The JWE service also depends on the data conversion service, so we can't rely on IoC/DI to inject it since it would introduce a dependency cycle. As a result, it has to be done explicitly when
	 * the JWE service instance is initialized.
	 * </p>
	 * 
	 * @param jweService the JWE service
	 */
	public void injectJWEService(JWEService jweService) {
		this.jweService = jweService;
	}
	
	/**
	 * <p>
	 * Injects the JWS service into the data conversion service.
	 * </p>
	 * 
	 * <p>
	 * The JWS service also depends on the data conversion service, so we can't rely on IoC/DI to inject it since it would introduce a dependency cycle. As a result, it has to be done explicitly when
	 * the JWS service instance is initialized.
	 * </p>
	 * 
	 * @param jwsService the JWS service
	 */
	public void injectJWSService(JWSService jwsService) {
		this.jwsService = jwsService;
	}
	
	/**
	 * <p>
	 * Injects the JWT service into the data conversion service.
	 * </p>
	 * 
	 * <p>
	 * The JWT service also depends on the data conversion service, so we can't rely on IoC/DI to inject it since it would introduce a dependency cycle. As a result, it has to be done explicitly when
	 * the JWT service instance is initialized.
	 * </p>
	 * 
	 * @param jwtService the JWT service
	 */
	public void injectJWTService(JWTService jwtService) {
		this.jwtService = jwtService;
	}
	
	@Override
	public Optional<MediaTypeConverter<String>> getConverter(String mediaType) {
		String normalizedMediaType = DataConversionService.normalizeMediaType(mediaType);
		MediaTypeConverter<String> result = this.convertersCache.get(normalizedMediaType);
		if (result == null && !this.convertersCache.containsKey(normalizedMediaType)) {
			for (MediaTypeConverter<String> converter : this.converters) {
				if (converter.canConvert(normalizedMediaType)) {
					this.convertersCache.put(normalizedMediaType, converter);
					result = converter;
					break;
				}
			}
			
			if(result == null) {
				if(MediaTypes.APPLICATION_JOSE.equals(mediaType)) {
					result = new JOSEStringMediaTypeConverter(this.jwsService, this.jweService);
				}
				else if(MediaTypes.APPLICATION_JOSE_JSON.endsWith(mediaType)) {
					result = new JOSEJsonStringMediaTypeConverter(this.jwsService, this.jweService);
				}
				else if(MediaTypes.APPLICATION_JWT.equals(normalizedMediaType)) {
					result = this.jwtService != null ? new JWTStringMediaTypeConverter(this.jwtService) : null;
				}
				else if(MediaTypes.APPLICATION_JWK_JSON.equals(normalizedMediaType)) {
					result = this.jwkService != null ? new JWKJsonMediaTypeConverter(this.jwkService, this.mapper) : null;
				}
				else if(MediaTypes.APPLICATION_JWK_SET_JSON.equals(normalizedMediaType)) {
					result = this.jwkService != null ? new JWKSetJsonMediaTypeConverter(this.jwkService, this.mapper) : null;
				}
			}
			
			if (result == null) {
				this.convertersCache.put(normalizedMediaType, null);
			}
		}
		return Optional.ofNullable(result);
	}
	
	/**
	 * <p>
	 * Media type converters socket used to inject media type converters when building the JOSE module.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see GenericDataConversionService
	 */
	@Bean(name = "mediaTypeConverters")
	public static interface MediaTypeConvertersSocket extends Supplier<List<MediaTypeConverter<String>>> {
	}
}

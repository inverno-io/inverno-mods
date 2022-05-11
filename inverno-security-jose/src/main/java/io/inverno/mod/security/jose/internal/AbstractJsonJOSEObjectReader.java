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
package io.inverno.mod.security.jose.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.JsonJOSEObject;
import io.inverno.mod.security.jose.JsonJOSEObjectReader;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.internal.jwe.GenericJsonJWEReader;
import io.inverno.mod.security.jose.internal.jws.GenericJsonJWSReader;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base JSON JOSE object reader.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GenericJsonJWSReader
 * @see GenericJsonJWEReader
 * 
 * @param <A> the payload type
 * @param <B> the JSON JOSE object type
 * @param <C> the JSON JOSE object reader type
 */
public abstract class AbstractJsonJOSEObjectReader<A, B extends JsonJOSEObject<A>, C extends AbstractJsonJOSEObjectReader<A, B, C>> implements JsonJOSEObjectReader<A, B, C> {
	
	private static final Logger LOGGER = LogManager.getLogger(AbstractJsonJOSEObjectReader.class);
	
	/**
	 * The object mapper.
	 */
	protected final ObjectMapper mapper;
	
	/**
	 * The data conversion service.
	 */
	protected final DataConversionService dataConversionService;
	
	/**
	 * The JWK service.
	 */
	protected final JWKService jwkService;
	
	/**
	 * The payload type.
	 */
	protected final Type type;
	
	/**
	 * The set of custom parameters processed by the application.
	 */
	protected Set<String> applicationProcessedParameters;
	
	/**
	 * <p>
	 * Creates a JSON JOSE object reader.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the expected payload type
	 */
	@SuppressWarnings("exports")
	public AbstractJsonJOSEObjectReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type) {
		this.mapper = mapper;
		this.dataConversionService = dataConversionService;
		this.jwkService = jwkService;
		this.type = type;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public C processedParameters(String... parameters) {
		if(parameters == null || parameters.length == 0) {
			this.applicationProcessedParameters = null;
		}
		else {
			this.applicationProcessedParameters = Arrays.stream(parameters).filter(Objects::nonNull).collect(Collectors.toSet());
		}
		return (C)this;
	}
	
	/**
	 * <p>
	 * Returns the payload decoder to use to deserialize the payload.
	 * </p>
	 *
	 * <p>
	 * This method first considers the overriding payload decoder which is returned if present. Then the overriding content type is used if present to resolve the media type converter to use. Finally
	 * the payload content type specified in the parsed JOSE headers is considered to resolve the media type converter to use.
	 * </p>
	 *
	 * @param overridingPayloadDecoder the overriding payload decoder
	 * @param overridingContentType    the overriding payload media type
	 * @param cty                      the payload content type specified in the parsed JOSE headers
	 *
	 * @return a payload decoder
	 *
	 * @throws JOSEObjectReadException if no payload encoder could have been resolved
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected Function<String, Mono<A>> getPayloadDecoder(Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType, String cty) throws JOSEObjectReadException, JOSEProcessingException {
		if(overridingPayloadDecoder != null) {
			return overridingPayloadDecoder;
		}
		
		String resolvedContentType;
		if(StringUtils.isNotBlank(overridingContentType)) {
			if(LOGGER.isDebugEnabled() && StringUtils.isNotBlank(cty) && DataConversionService.normalizeMediaType(cty).equals(DataConversionService.normalizeMediaType(overridingContentType))) {
				// We just log a debug here since we want to be able to override the JWS header content type
				LOGGER.debug("The overriding content type differs from the JOSE header content type");
			}
			resolvedContentType = overridingContentType;
		}
		else if(StringUtils.isNotBlank(cty)) {
			resolvedContentType = cty;
		}
		else {
			throw new JOSEObjectReadException("Content type is blank and no overriding content type was provided");
		}
		
		MediaTypeConverter<String> payloadConverter = this.dataConversionService.getConverter(resolvedContentType).orElseThrow(() -> new JOSEObjectReadException("No converter found for content type: " + resolvedContentType));
		return payloadRaw -> payloadConverter.decodeOne(Mono.just(payloadRaw), this.type);
	}
}

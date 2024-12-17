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
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.jose.JOSEHeaderConfigurator;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.JsonJOSEObject;
import io.inverno.mod.security.jose.JsonJOSEObjectBuilder;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.internal.jwe.GenericJsonJWEBuilder;
import io.inverno.mod.security.jose.internal.jws.GenericJsonJWSBuilder;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base JSON JOSE object builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GenericJsonJWSBuilder
 * @see GenericJsonJWEBuilder
 * 
 * @param <A> the payload type
 * @param <B> the JSON JOSE object type
 * @param <C> the JOSE header configurator type
 * @param <D> the JSON JOSE object builder type
 */
public abstract class AbstractJsonJOSEObjectBuilder<A, B extends JsonJOSEObject<A>, C extends JOSEHeaderConfigurator<C>, D extends AbstractJsonJOSEObjectBuilder<A, B, C, D>> implements JsonJOSEObjectBuilder<A, B, C, D> {
	
	private static final Logger LOGGER = LogManager.getLogger(AbstractJsonJOSEObjectBuilder.class);
	
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
	 * The protected JOSE header configurer.
	 */
	protected Consumer<C> protectedHeaderConfigurer;
	
	/**
	 * The unprotected JOSE header configurer.
	 */
	protected Consumer<C> unprotectedHeaderConfigurer;
	
	/**
	 * The payload.
	 */
	protected A payload;

	/**
	 * <p>
	 * Creates a JSON JOSE object builder.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the payload type
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public AbstractJsonJOSEObjectBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type) {
		this.mapper = mapper;
		this.dataConversionService = dataConversionService;
		this.jwkService = jwkService;
		this.type = type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D headers(Consumer<C> protectedHeaderConfigurer, Consumer<C> unprotectedHeaderConfigurer) {
		this.protectedHeaderConfigurer = protectedHeaderConfigurer;
		this.unprotectedHeaderConfigurer = unprotectedHeaderConfigurer;
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D payload(A payload) {
		this.payload = payload;
		return (D)this;
	}
	
	/**
	 * <p>
	 * Verifies that the payload is valid.
	 * </p>
	 * 
	 * <p>
	 * This basically checks that the payload is not null.
	 * </p>
	 * 
	 * @throws JOSEObjectBuildException if the payload is invalid
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected void checkPayload() throws JOSEObjectBuildException, JOSEProcessingException {
		if(this.payload == null) {
			throw new JOSEObjectBuildException("Payload is null");
		}
	}
	
	/**
	 * <p>
	 * Returns the payload encoder to use to serialize the payload.
	 * </p>
	 *
	 * <p>
	 * This method first considers the overriding payload encoder which is returned if present. Then the overriding content type is used if present to resolve the media type converter to use. Finally
	 * the payload content type defined in JOSE headers is considered to resolve the media type converter to use.
	 * </p>
	 *
	 * @param overridingPayloadEncoder the overriding payload encoder
	 * @param overridingContentType    the overriding payload media type
	 * @param cty                      the payload content type defined in JOSE headers
	 *
	 * @return a payload encoder
	 *
	 * @throws JOSEObjectBuildException if no payload encoder could have been resolved
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected Function<A, Mono<String>> getPayloadEncoder(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, String cty) throws JOSEObjectBuildException, JOSEProcessingException {
		if(overridingPayloadEncoder != null) {
			return overridingPayloadEncoder;
		}
		
		String resolvedContentType;
		if(StringUtils.isNotBlank(overridingContentType)) {
			if(LOGGER.isDebugEnabled() && StringUtils.isNotBlank(cty) && MediaTypes.normalizeApplicationMediaType(cty).equals(overridingContentType)) {
				// We just log a debug here since we want to be able to override the JWS header content type (e.g. application/json to encode application/jwt)
				LOGGER.debug("The overriding content type differs from the JOSE header content type");
			}
			resolvedContentType = overridingContentType;
		}
		else if(StringUtils.isNotBlank(cty)) {
			resolvedContentType = cty;
		}
		else {
			throw new JOSEObjectBuildException("Content type is blank and no overriding content type was provided");
		}
		
		MediaTypeConverter<String> payloadConverter = this.dataConversionService.getConverter(resolvedContentType).orElseThrow(() -> new JOSEObjectBuildException("No converter found for content type: " + resolvedContentType));
		if(this.type != null) {
			return p -> Flux.from(payloadConverter.encodeOne(Mono.just(p), this.type)).reduceWith(StringBuilder::new, StringBuilder::append).map(StringBuilder::toString);
		}
		else {
			return p -> Flux.from(payloadConverter.encodeOne(Mono.just(p))).reduceWith(StringBuilder::new, StringBuilder::append).map(StringBuilder::toString);
		}
	}
}

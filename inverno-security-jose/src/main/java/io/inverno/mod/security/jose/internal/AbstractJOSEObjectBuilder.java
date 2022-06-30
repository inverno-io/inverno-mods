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
import io.inverno.mod.security.jose.JOSEHeader;
import io.inverno.mod.security.jose.JOSEHeaderConfigurator;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectBuilder;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.internal.jwe.GenericJWEBuilder;
import io.inverno.mod.security.jose.internal.jws.GenericJWSBuilder;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKStore;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base JOSE object builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GenericJWSBuilder
 * @see GenericJWEBuilder
 * 
 * @param <A> the payload type
 * @param <B> the JOSE header type
 * @param <C> the JOSE object type
 * @param <D> the JOSE header configurator type
 * @param <E> the JOSE object builder type
 */
public abstract class AbstractJOSEObjectBuilder<A, B extends JOSEHeader, C extends JOSEObject<A, B>, D extends JOSEHeaderConfigurator<D>, E extends AbstractJOSEObjectBuilder<A, B, C, D, E>> implements JOSEObjectBuilder<A, B, C, D, E> {

	private static final Logger LOGGER = LogManager.getLogger(AbstractJOSEObjectBuilder.class);
	
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
	 * The keys to consider to secure JOSE objects.
	 */
	protected final Publisher<? extends JWK> keys;

	/**
	 * The payload.
	 */
	protected A payload;
	
	/**
	 * <p>
	 * Creates a JOSE object builder.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the payload type
	 * @param keys                  the keys to consider to secure JOSE objects
	 */
	@SuppressWarnings("exports")
	public AbstractJOSEObjectBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys) {
		this.mapper = mapper;
		this.dataConversionService = dataConversionService;
		this.jwkService = jwkService;
		this.type = type;
		this.keys = keys;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public E payload(A payload) {
		this.payload = payload;
		return (E)this;
	}
	
	/**
	 * <p>
	 * Returns the set of parameters processed by the builder.
	 * </p>
	 * 
	 * <p>
	 * These basically corresponds to the registered parameters processed and understood by the JOSE header.
	 * </p>
	 * 
	 * @return a set of parameters
	 */
	protected Set<String> getProcessedParameters() {
		return AbstractJOSEHeader.PROCESSED_PARAMETERS;
	}
	
	/**
	 * <p>
	 * Returns the keys to consider to secure JOSE objects.
	 * </p>
	 * 
	 * <p>
	 * Keys can be provided explicitly to the builder, in which case, only the keys that supports the specified algorithm will be retained to secure JOSE objects otherwise the builder will try to
	 * resolve keys based on the JOSE header built from the builder's parameters and only retain the trusted keys, typically those resolved from the {@link JWKStore} or from a trusted
	 * {@link KeyStore}.
	 * </p>
	 * 
	 * <p>
	 * The resulting publisher will fail if no key could be found.
	 * </p>
	 * 
	 * @param header the JOSE header
	 * 
	 * @return a publisher of keys
	 * 
	 * @throws JOSEObjectBuildException if no suitable key could be found
	 */
	protected Flux<? extends JWK> getKeys(JOSEHeader header) throws JOSEObjectBuildException {
		if(this.keys != null) {
			return Flux.from(this.keys)
				.filter(key -> {
					if(!key.supportsAlgorithm(header.getAlgorithm())) {
						LOGGER.warn(() -> "Ignoring key " + key + " which does not support algorithm " + header.getAlgorithm());
						return false;
					}
					return true;
				})
				.switchIfEmpty(Mono.error(() -> new JOSEObjectBuildException("No suitable key found")));
		}
		else {
			return Flux.from(this.jwkService.read(header))
				.filter(key -> {
					if(!key.isTrusted()) {
						LOGGER.warn(() -> "Skipping untrusted key: " + key);
						return false;
					}
					return true;
				})
				.switchIfEmpty(Mono.error(() -> new JOSEObjectBuildException("No suitable key found")));
		}
	}
	
	/**
	 * <p>
	 * Verifies that the JOSE header is valid.
	 * </p>
	 *
	 * <p>
	 * This basically checks that:
	 * </p>
	 *
	 * <ul>
	 * <li>the algorithm is not blank</li>
	 * <li>Custom parameters do not include registered parameters (see {@link #getProcessedParameters()})</li>
	 * <li>Critical parameters set does not include undefined parameters</li>
	 * </ul>
	 *
	 * @param header the JOSE header
	 *
	 * @throws JOSEObjectBuildException if the header is invalid
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected void checkHeader(B header) throws JOSEObjectBuildException, JOSEProcessingException {
		if(StringUtils.isBlank(header.getAlgorithm())) {
			throw new JOSEObjectBuildException("Algorithm is blank");
		}
		
		Map<String, Object> customParameters = header.getCustomParameters();
		if(customParameters != null) {
			Set<String> invalidCustomParameters = new HashSet<>(customParameters.keySet());
			invalidCustomParameters.retainAll(this.getProcessedParameters());
			if(!invalidCustomParameters.isEmpty()) {
				throw new JOSEObjectBuildException("Custom parameters must not include registered parameters: " + invalidCustomParameters.stream().collect(Collectors.joining(", ")));
			}
		}
		if(header.getCritical() != null) {
			Set<String> remainingCrit = new HashSet<>(header.getCritical());
			if(customParameters != null) {
				remainingCrit.removeAll(customParameters.keySet());
			}
			if(!remainingCrit.isEmpty()) {
				throw new JOSEObjectBuildException("Critical parameters must not include parameters not defined as custom parameters: " + remainingCrit.stream().collect(Collectors.joining(", ")));
			}
		}
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
	 * the payload content type defined in the JOSE header is considered to resolve the media type converter to use.
	 * </p>
	 *
	 * @param overridingPayloadEncoder the overriding payload encoder
	 * @param overridingContentType    the overriding payload media type
	 * @param header                   the JOSE header
	 *
	 * @return a payload encoder
	 *
	 * @throws JOSEObjectBuildException if no payload encoder could have been resolved
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected Function<A, Mono<String>> getPayloadEncoder(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, JOSEHeader header) throws JOSEObjectBuildException, JOSEProcessingException {
		if(overridingPayloadEncoder != null) {
			return overridingPayloadEncoder;
		}
		
		String cty = header.getContentType();
		
		String resolvedContentType;
		if(StringUtils.isNotBlank(overridingContentType)) {
			if(LOGGER.isDebugEnabled() && StringUtils.isNotBlank(cty) && MediaTypes.normalizeApplicationMediaType(cty).equals(MediaTypes.normalizeApplicationMediaType(overridingContentType))) {
				// We just log a debug here since we want to be able to override the JWS header content type (eg. application/json to encode application/jwt)
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
			return p -> Flux.from(payloadConverter.encodeOne(Mono.just(p), this.type)).reduceWith(() -> new StringBuilder(), (acc, v) -> acc.append(v)).map(StringBuilder::toString);
		}
		else {
			return p -> Flux.from(payloadConverter.encodeOne(Mono.just(p))).reduceWith(() -> new StringBuilder(), (acc, v) -> acc.append(v)).map(StringBuilder::toString);
		}
	}
}

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
import io.inverno.mod.security.jose.JOSEHeader;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEObjectReader;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwa.JWA;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKStore;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
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
 * Base JOSE object reader implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the JOSE header type
 * @param <C> the JOSE object type
 * @param <D> the JOSE object reader type
 */
public abstract class AbstractJOSEObjectReader<A, B extends JOSEHeader, C extends JOSEObject<A, B>, D extends AbstractJOSEObjectReader<A, B, C, D>> implements JOSEObjectReader<A, B, C, D> {
	
	private static final Logger LOGGER = LogManager.getLogger(AbstractJOSEObjectReader.class);
	
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
	 * The keys to consider to verify or decrypt JOSE objects
	 */
	protected final Publisher<? extends JWK> keys;

	/**
	 * The set of custom parameters processed by the application.
	 */
	protected Set<String> applicationProcessedParameters;
	
	/**
	 * <p>
	 * Creates a JOSE object reader.
	 * </p>
	 * 
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the expected payload type
	 * @param keys                  the keys to consider to verify or decrypt JOSE objects
	 */
	@SuppressWarnings("exports")
	public AbstractJOSEObjectReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys) {
		this.mapper = mapper;
		this.dataConversionService = dataConversionService;
		this.jwkService = jwkService;
		this.type = type;
		this.keys = keys;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public D processedParameters(String... parameters) {
		if(parameters == null || parameters.length == 0) {
			this.applicationProcessedParameters = null;
		}
		else {
			this.applicationProcessedParameters = Arrays.stream(parameters).filter(Objects::nonNull).collect(Collectors.toSet());
		}
		return (D)this;
	}
	
	/**
	 * <p>
	 * Returns the set of parameters processed by the reader.
	 * </p>
	 * 
	 * <p>
	 * These basically corresponds to the registered parameters processed and understood by the expected JOSE header.
	 * </p>
	 * 
	 * @return a set of parameters
	 */
	protected Set<String> getProcessedParameters() {
		return AbstractJOSEHeader.PROCESSED_PARAMETERS;
	}
	
	/**
	 * <p>
	 * Returns the keys to consider to verify or decrypt JOSE objects.
	 * </p>
	 * 
	 * <p>
	 * Keys can be provided explicitly to the reader, in which case, only the keys that supports the algorithm specified in the parsed JOSE header will be retained to verify or decrypt JOSE objects
	 * otherwise the reader will try to resolve keys based on the parsed JOSE header and only retain the trusted keys, typically those resolved from the {@link JWKStore}, from a trusted
	 * {@link KeyStore} or from a valid certificate chain.
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
	 * @throws JOSEObjectReadException if no suitable key could be found
	 */
	protected Flux<? extends JWK> getKeys(JOSEHeader header) throws JOSEObjectReadException {
		if(this.keys != null) {
			return Flux.from(this.keys)
				.filter(key -> {
					if(!key.supportsAlgorithm(header.getAlgorithm())) {
						LOGGER.debug(() -> "Ignoring key " + key + " which does not support algorithm " + header.getAlgorithm());
						return false;
					}
					return true;
				})
				.switchIfEmpty(Mono.error(() -> new JOSEObjectReadException("No suitable key found")));
		}
		return Flux.from(this.jwkService.read(header))
			.filter(key -> {
				if(!key.isTrusted()) {
					LOGGER.debug(() -> "Skipping untrusted key: " + key);
					return false;
				}
				return true;
			})
			.switchIfEmpty(Mono.error(() -> new JOSEObjectReadException("No suitable key found")));
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
	 * <li>Critical parameters set does not include registered parameters (see {@link #getProcessedParameters()})</li>
	 * <li>Critical parameters set does not include parameters not present in the custom parameters</li>
	 * </ul>
	 *
	 * @param header the JOSE header
	 *
	 * @throws JOSEObjectReadException if the header is invalid
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected void checkHeader(B header) throws JOSEObjectReadException, JOSEProcessingException {
		if(StringUtils.isBlank(header.getAlgorithm())) {
			throw new JOSEObjectReadException("Algorithm is blank");
		}

		if(header.getCritical() != null) {
			Set<String> remainingCrit = new HashSet<>(header.getCritical());

			Set<String> processedParameters = this.getProcessedParameters();
			if(remainingCrit.removeAll(processedParameters)) {
				throw new JOSEObjectReadException("Critical parameters must not include registered parameters: " + header.getCritical().stream().filter(processedParameters::contains).collect(Collectors.joining(", ")));
			}

			remainingCrit.removeAll(header.getCustomParameters().keySet());
			if(!remainingCrit.isEmpty()) {
				throw new JOSEObjectReadException("Critical parameters must not include parameters not defined as custom parameters: " + remainingCrit.stream().collect(Collectors.joining(", ")));
			}
		}
	}
	
	/**
	 * <p>
	 * Verifies that all parameters in the critical parameters set are understood and processed either by the reader (see {@link #getProcessedParameters()}) or the application (see
	 * {@link #processedParameters(java.lang.String...)}).
	 * </p>
	 * 
	 * <p>
	 * The set of processed parameters is composed of the parameters processed by the reader (see {@link #getProcessedParameters()}, the parameters processed by the JSON Web Algorithms used to verify
	 * or decrypt JOSE objects and the parameters processed by the application (see {@link #processedParameters(java.lang.String...)}). 
	 * </p>
	 * 
	 * @param crit the critical parameters set
	 * @param jwas the list of JSON Web Algorithms used to verify or decrypt the JOSE object
	 * 
	 * @throws JOSEObjectReadException if critical parameters set is invalid (i.e. it contains ununderstood and unprocessed parameters)
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected void checkCriticalParameters(Set<String> crit, JWA... jwas) throws JOSEObjectReadException, JOSEProcessingException {
		if(crit != null) {
			Set<String> remainingParameters = new HashSet<>(crit);
			if(jwas != null) {
				for(JWA jwa : jwas) {
					if(jwa != null) {
						remainingParameters.removeAll(jwa.getProcessedParameters());
					}
				}
			}
			if(this.applicationProcessedParameters != null) {
				remainingParameters.removeAll(this.applicationProcessedParameters);
			}
			if(!remainingParameters.isEmpty()) {
				throw new JOSEObjectReadException("Unsupported critical parameters: " + remainingParameters.stream().collect(Collectors.joining(", ")));
			}
		}
	}
	
	/**
	 * <p>
	 * Returns the payload decoder to use to deserialize the payload.
	 * </p>
	 *
	 * <p>
	 * This method first considers the overriding payload decoder which is returned if present. Then the overriding content type is used if present to resolve the media type converter to use. Finally
	 * the payload content type in the parsed JOSE header is considered to resolve the media type converter to use.
	 * </p>
	 *
	 * @param overridingPayloadDecoder the overriding payload decoder
	 * @param overridingContentType    the overriding payload media type
	 * @param header                   the JOSE header
	 *
	 * @return a payload decoder
	 *
	 * @throws JOSEObjectReadException if no payload encoder could have been resolved
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected Function<String, Mono<A>> getPayloadDecoder(Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType, JOSEHeader header) throws JOSEObjectReadException, JOSEProcessingException {
		if(overridingPayloadDecoder != null) {
			return overridingPayloadDecoder;
		}
		
		String cty = header.getContentType();
		
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

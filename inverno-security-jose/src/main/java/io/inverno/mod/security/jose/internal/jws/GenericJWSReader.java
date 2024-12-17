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
package io.inverno.mod.security.jose.internal.jws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJOSEObjectReader;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwa.NoAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSBuildException;
import io.inverno.mod.security.jose.jws.JWSHeader;
import io.inverno.mod.security.jose.jws.JWSReadException;
import io.inverno.mod.security.jose.jws.JWSReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON Web Signature compact reader implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class GenericJWSReader<A> extends AbstractJOSEObjectReader<A, JWSHeader, JWS<A>, GenericJWSReader<A>> implements JWSReader<A, GenericJWSReader<A>> {

	private static final Logger LOGGER = LogManager.getLogger(GenericJWSReader.class);

	/**
	 * <p>
	 * Creates a generic JWS reader.
	 * </p>
	 * 
	 * @param mapper an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService a JWK service
	 * @param type the payload type
	 * @param keys the keys to consider to verify the JWS
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public GenericJWSReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys) {
		super(mapper, dataConversionService, jwkService, type, keys);
	}
	
	@Override
	public Mono<JWS<A>> read(String compact, String contentType) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(compact, null, contentType);
	}

	@Override
	public Mono<JWS<A>> read(String compact, Function<String, Mono<A>> payloadDecoder) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(compact, payloadDecoder, null);
	}
	
	/**
	 * <p>
	 * Reads a compact JWS representation.
	 * </p>
	 *
	 * @param compact                  a JWS compact representation
	 * @param overridingPayloadDecoder an overriding payload decoder
	 * @param overridingContentType    an overriding payload content type
	 *
	 * @return a single JWS publisher
	 *
	 * @throws JWSReadException         if there was an error reading the JWS
	 * @throws JWSBuildException        if there was an error building the JWS
	 * @throws JOSEObjectReadException  if there was an error reading the JWS
	 * @throws JOSEObjectBuildException if there was an error building the JWS
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWS<A>> read(String compact, Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		String[] jwsParts = compact.split("\\.", -1);
		if(jwsParts.length != 3) {
			throw new JWSReadException("Invalid compact JWS");
		}
		GenericJWSHeader jwsHeader = this.readJWSHeader(jwsParts[0]);
		return this.readJWSPayload(jwsParts[1], overridingPayloadDecoder, overridingContentType, jwsHeader)
			.flatMap(jwsPayload -> this.read(jwsHeader, jwsPayload, jwsParts[2], this.getKeys(jwsHeader)));
	}
	
	/**
	 * <p>
	 * Reads a compact JWS representation.
	 * </p>
	 * 
	 * @param jwsHeader the JWS header
	 * @param jwsPayload the JWS payload
	 * @param encodedSignature the Base64URL encoded signature
	 * @param keys the keys to consider to verify the JWS
	 * 
	 * @return a single JWS publisher
	 * 
	 * @throws JWSReadException         if there was an error reading the JWS
	 * @throws JWSBuildException        if there was an error building the JWS
	 * @throws JOSEObjectReadException  if there was an error reading the JWS
	 * @throws JOSEObjectBuildException if there was an error building the JWS
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWS<A>> read(GenericJWSHeader jwsHeader, GenericJWSPayload<A> jwsPayload, String encodedSignature, Flux<? extends JWK> keys) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		// 1. Decode payload
		if(jwsHeader.getAlgorithm().equals(NoAlgorithm.NONE.getAlgorithm())) {
			// Unsecured JWS
			if(StringUtils.isNotBlank(encodedSignature)) {
				throw new JWSReadException("Unsecured JWS has a non-blank signature");
			}
			this.checkCriticalParameters(jwsHeader.getCritical());
			return Mono.just(new GenericJWS<>(jwsHeader, jwsPayload, null));
		}
		else {
			final byte[] signingInput = (jwsHeader.getEncoded() + "." + jwsPayload.getEncoded()).getBytes();
			final byte[] signature = Base64.getUrlDecoder().decode(encodedSignature);

			return Mono.defer(() -> {
				JWSReadException error = new JWSReadException("Failed to read JWS");
				
				// 2. Get the jwk
				return keys
					.onErrorStop()
					.map(key -> {
						// 3. Get signer
						JWASigner signer = key.signer(jwsHeader.getAlgorithm());

						// 4. Check critical parameters
						this.checkCriticalParameters(jwsHeader.getCritical(), signer);

						// 5. Verify signature
						if(signer.verify(signingInput, signature)) {
							jwsHeader.setKey(key);
							return (JWS<A>)new GenericJWS<>(jwsHeader, jwsPayload, encodedSignature);
						}
						else {
							throw new JWSReadException("JWS signature is invalid with key: " + key);
						}
					})
					.onErrorContinue((e, key) -> {
						error.addSuppressed(e);
						LOGGER.debug(() -> "Failed to read JWS with key: " + key, e);
					})
					.next()
					.switchIfEmpty(Mono.error(error));
			});
		}
	}
	
	/**
	 * <p>
	 * Reads the JWS header.
	 * </p>
	 * 
	 * @param encodedHeader the Base64URL encoded JWS header
	 * 
	 * @return the JWS header
	 * 
	 * @throws JWSReadException        if there was an error reading the JWS header
	 * @throws JOSEObjectReadException if there was an error reading the JWS header
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected GenericJWSHeader readJWSHeader(String encodedHeader) throws JWSReadException, JOSEObjectReadException, JOSEProcessingException {
		try {
			GenericJWSHeader jwsHeader = this.mapper.readValue(Base64.getUrlDecoder().decode(encodedHeader), GenericJWSHeader.class);
			this.checkHeader(jwsHeader);
			jwsHeader.setEncoded(encodedHeader);
			
			return jwsHeader;
		} 
		catch(IOException e) {
			throw new JWSReadException("Error decoding JOSE header", e);
		}
	}
	
	/**
	 * <p>
	 * Reads the JWS payload.
	 * </p>
	 * 
	 * @param encodedPayload the Base64URL encoded payload
	 * @param overridingPayloadDecoder an overriding payload decoder
	 * @param overridingContentType    an overriding payload content type
	 * @param jwsHeader the JWS header
	 * 
	 * @return a single JWS payload publisher
	 * 
	 * @throws JWSReadException        if there was an error reading the JWS payload
	 * @throws JOSEObjectReadException if there was an error reading the JWS payload
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected Mono<GenericJWSPayload<A>> readJWSPayload(String encodedPayload, Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType, GenericJWSHeader jwsHeader) throws JWSReadException, JOSEObjectReadException, JOSEProcessingException {
		if(StringUtils.isBlank(encodedPayload)) {
			throw new JWSReadException("Payload is blank");
		}
		
		Function<String, Mono<A>> payloadDecoder = this.getPayloadDecoder(overridingPayloadDecoder, overridingContentType, jwsHeader);
		
		return Mono.defer(() -> {
				if(jwsHeader.isBase64EncodePayload() != null && !jwsHeader.isBase64EncodePayload()) {
					return payloadDecoder.apply(encodedPayload)
						.map(payload -> {
							GenericJWSPayload<A> jwsPayload = new GenericJWSPayload<>(payload);
							jwsPayload.setRaw(encodedPayload);
	
							return jwsPayload;
						});
				}
				else {
					return payloadDecoder.apply(new String(Base64.getUrlDecoder().decode(encodedPayload)))
						.map(payload -> {
							GenericJWSPayload<A> jwsPayload = new GenericJWSPayload<>(payload);
							jwsPayload.setEncoded(encodedPayload);
							jwsPayload.setRaw(new String(Base64.getUrlDecoder().decode(encodedPayload)));
	
							return jwsPayload;
						});
				}
			});
	}
}

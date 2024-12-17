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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJOSEObjectBuilder;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwa.NoAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSBuildException;
import io.inverno.mod.security.jose.jws.JWSBuilder;
import io.inverno.mod.security.jose.jws.JWSHeader;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON Web Signature builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class GenericJWSBuilder<A> extends AbstractJOSEObjectBuilder<A, JWSHeader, JWS<A>, GenericJWSHeader, GenericJWSBuilder<A>> implements JWSBuilder<A, GenericJWSHeader, GenericJWSBuilder<A>> {

	private static final Logger LOGGER = LogManager.getLogger(GenericJWSBuilder.class);
	
	/**
	 * The JWS header configurer.
	 */
	protected Consumer<GenericJWSHeader> headerConfigurer;

	/**
	 * <p>
	 * Creates a generic JWS builder.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the payload type
	 * @param keys                  the keys to consider to sign the JWS
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public GenericJWSBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys) {
		super(mapper, dataConversionService, jwkService, type, keys);
	}

	@Override
	public GenericJWSBuilder<A> header(Consumer<GenericJWSHeader> configurer) {
		this.headerConfigurer = configurer;
		return this;
	}

	@Override
	public Mono<JWS<A>> build(String contentType) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(null, contentType);
	}

	@Override
	public Mono<JWS<A>> build(Function<A, Mono<String>> payloadEncoder) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(payloadEncoder, null);
	}
	
	/**
	 * <p>
	 * Builds the JWS.
	 * </p>
	 *
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 *
	 * @return a single JWS publisher
	 *
	 * @throws JWSBuildException        if there was an error building the JWS
	 * @throws JOSEObjectBuildException if there was an error building the JWS
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWS<A>> build(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		GenericJWSHeader jwsHeader = this.buildJWSHeader();
		return this.buildJWSPayload(overridingPayloadEncoder, overridingContentType, jwsHeader).flatMap(jwsPayload -> this.build(
			jwsHeader, 
			jwsPayload, 
			this.getKeys(jwsHeader)
		));
	}
	
	/**
	 * <p>
	 * Builds the JWS.
	 * </p>
	 * 
	 * @param jwsHeader the JWS header
	 * @param jwsPayload the JWS payload
	 * @param keys the resolved keys to consider to sign the JWS
	 * 
	 * @return a single JWS publisher
	 * 
	 * @throws JWSBuildException        if there was an error building the JWS
	 * @throws JOSEObjectBuildException if there was an error building the JWS
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWS<A>> build(GenericJWSHeader jwsHeader, GenericJWSPayload<A> jwsPayload, Flux<? extends JWK> keys) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		String signingHeader = jwsHeader.getEncoded();
		int signingHeaderLength = signingHeader.length();
		String signingPayload = jwsHeader.isBase64EncodePayload() == null || jwsHeader.isBase64EncodePayload() ? jwsPayload.getEncoded() : jwsPayload.getRaw();
		int signingPayloadLength = signingPayload.length();
		final byte[] signingInput = new byte[signingHeaderLength + signingPayloadLength + 1];
		System.arraycopy(signingHeader.getBytes(), 0, signingInput, 0, signingHeaderLength);
		signingInput[signingHeaderLength] = '.';
		System.arraycopy(signingPayload.getBytes(), 0, signingInput, signingHeaderLength + 1, signingPayloadLength);
		
		if(jwsHeader.getAlgorithm().equals(NoAlgorithm.NONE.getAlgorithm())) {
			// Unsecured JWS
			return Mono.just(new GenericJWS<>(jwsHeader, jwsPayload, null));
		}
		else {
			return Mono.defer(() -> {
				JWSBuildException error = new JWSBuildException("Failed to build JWS");
				return keys
					.onErrorStop()
					.map(key -> {
						String joseSignature = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(key.signer(jwsHeader.getAlgorithm()).sign(signingInput));
						jwsHeader.setKey(key);
						return (JWS<A>)new GenericJWS<>(jwsHeader, jwsPayload, joseSignature);
					})
					.onErrorContinue((e, key) -> {
						error.addSuppressed(e);
						LOGGER.debug(() -> "Failed to build JWS with key: " + key, e);
					})
					.next()
					.switchIfEmpty(Mono.error(error));
			});
		}
	}
	
	/**
	 * <p>
	 * Builds the JWS header.
	 * </p>
	 *
	 * @return the JWS header
	 *
	 * @throws JWSBuildException        if there was an error building the JWS header
	 * @throws JOSEObjectBuildException if there was an error building the JWS header
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected GenericJWSHeader buildJWSHeader() throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		GenericJWSHeader jwsHeader = new GenericJWSHeader();
		
		if(this.headerConfigurer != null) {
			this.headerConfigurer.accept(jwsHeader);
		}
		
		this.checkHeader(jwsHeader);
		
		if(jwsHeader.isBase64EncodePayload() != null) {
			if(jwsHeader.getCritical() == null) {
				jwsHeader.critical("b64");
			}
			jwsHeader.getCritical().add("b64");
		}
		
		try {
			jwsHeader.setEncoded(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(this.mapper.writeValueAsBytes(jwsHeader)));
		} 
		catch(JsonProcessingException e) {
			throw new JWSBuildException("Failed to encode JWS header", e);
		}

		return jwsHeader;
	}
	
	/**
	 * <p>
	 * Builds the JWS payload.
	 * </p>
	 * 
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 * @param jwsHeader the JWS header
	 *
	 * @return a single JWS payload publisher
	 *
	 * @throws JWSBuildException        if there was an error building the JWS payload
	 * @throws JOSEObjectBuildException if there was an error building the JWS payload
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected Mono<GenericJWSPayload<A>> buildJWSPayload(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, GenericJWSHeader jwsHeader) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		this.checkPayload();
		
		return this.getPayloadEncoder(overridingPayloadEncoder, overridingContentType, jwsHeader).apply(this.payload)
			.onErrorMap(e -> new JWSBuildException("Failed to encode JWS payload", e))
			.map(rawPayload -> {
				GenericJWSPayload<A> jwsPayload = new GenericJWSPayload<>(this.payload);
				jwsPayload.setRaw(rawPayload);
				if(jwsHeader.isBase64EncodePayload() == null || jwsHeader.isBase64EncodePayload()) {
					jwsPayload.setEncoded(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(rawPayload.getBytes()));
				}

				return jwsPayload;
			});
	}
}

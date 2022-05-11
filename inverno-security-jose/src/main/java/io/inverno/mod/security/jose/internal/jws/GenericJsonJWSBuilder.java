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
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJsonJOSEObjectBuilder;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWSBuildException;
import io.inverno.mod.security.jose.jws.JsonJWS;
import io.inverno.mod.security.jose.jws.JsonJWSBuilder;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON JWS builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class GenericJsonJWSBuilder<A> extends AbstractJsonJOSEObjectBuilder<A, JsonJWS<A, JsonJWS.BuiltSignature<A>>, GenericJWSHeader, GenericJsonJWSBuilder<A>> implements JsonJWSBuilder<A, GenericJWSHeader, GenericJsonJWSBuilder<A>> {

	private final List<SignatureInfo> signatureInfos;
	
	/**
	 * <p>
	 * Creates a generic JSON JWS builder.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the payload type
	 */
	@SuppressWarnings("exports")
	public GenericJsonJWSBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type) {
		super(mapper, dataConversionService, jwkService, type);
		this.signatureInfos = new LinkedList<>();
	}
	
	@Override
	public GenericJsonJWSBuilder<A> signature(Consumer<GenericJWSHeader> protectedHeaderConfigurer, Consumer<GenericJWSHeader> unprotectedHeaderConfigurer, Publisher<? extends JWK> keys) {
		this.signatureInfos.add(new SignatureInfo(protectedHeaderConfigurer, unprotectedHeaderConfigurer, keys));
		return this;
	}
	
	@Override
	public Mono<JsonJWS<A, JsonJWS.BuiltSignature<A>>> build(String contentType) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(null, contentType);
	}

	@Override
	public Mono<JsonJWS<A, JsonJWS.BuiltSignature<A>>> build(Function<A, Mono<String>> payloadEncoder) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(payloadEncoder, null);
	}
	
	/**
	 * <p>
	 * Builds the JSON JWS.
	 * </p>
	 * 
	 * <p>
	 * This method checks that the {@code typ}, {@code cty} and {@code b64} header parameters are consistent for all JSON JWS signatures.
	 * </p>
	 * 
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 * 
	 * @return a single JSON JWS publisher
	 * 
	 * @throws JWSBuildException        if there was an error building the JSON JWS
	 * @throws JOSEObjectBuildException if there was an error building the JSON JWS
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JsonJWS<A, JsonJWS.BuiltSignature<A>>> build(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		String typ = null;		
		String cty = null;
		Boolean b64 = null;
		for(SignatureInfo signatureInfo : this.signatureInfos) {
			signatureInfo.buildJWSHeaders();
			
			if(typ == null) {
				typ = signatureInfo.jwsHeader.getType();
			}
			else if(signatureInfo.jwsHeader.getType() != null && !typ.equals(signatureInfo.jwsHeader.getType())) {
				throw new JWSBuildException("Signatures have inconsistent types");
			}
			
			if(cty == null) {
				cty = signatureInfo.jwsHeader.getContentType();
			}
			else if(signatureInfo.jwsHeader.getContentType() != null && !cty.equals(signatureInfo.jwsHeader.getContentType())) {
				throw new JWSBuildException("Signatures have inconsistent content types");
			}
			
			if(b64 == null) {
				// signature must either null or true
				b64 = signatureInfo.jwsHeader.isBase64EncodePayload() == null || signatureInfo.jwsHeader.isBase64EncodePayload();
			}
			else if( (!b64 && (signatureInfo.jwsHeader.isBase64EncodePayload() == null || signatureInfo.jwsHeader.isBase64EncodePayload())) || (b64 && signatureInfo.jwsHeader.isBase64EncodePayload() != null && !signatureInfo.jwsHeader.isBase64EncodePayload()) ) {
				throw new JWSBuildException("Signatures have inconsistent base64 payload encoding");
			}
		}
		
		return this.buildJWSPayload(overridingPayloadEncoder, overridingContentType, cty, b64)
			.flatMap(jwsPayload -> Flux.fromIterable(this.signatureInfos)
				.flatMap(signature -> new SignatureJWSBuilder<>(
						this.mapper, 
						this.dataConversionService, 
						this.jwkService, 
						this.type, 
						signature.keys, 
						signature.signatureProtectedJWSHeader, 
						signature.jwsHeader, 
						jwsPayload
					)
					// overriding encoder and payload must be ignored since payload is already provided
					.build()
					.map(jws -> (JsonJWS.BuiltSignature<A>)new GenericJsonJWS.GenericBuiltSignature<>(signature.signatureProtectedJWSHeader, signature.signatureUnprotectedJWSHeader, jws))
				)
				.collectList()
				.map(signatures -> new GenericJsonJWS<>(this.mapper, jwsPayload, signatures))
			);
	}
	
	/**
	 * <p>
	 * Builds the JSON JWS.
	 * </p>
	 *
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 * @param cty                      common JSON JWS payload content type
	 * @param b64                      common JSON JWS b64 parameter
	 *
	 * @return a single JSON JWS publisher
	 *
	 * @throws JWSBuildException        if there was an error building the JSON JWS
	 * @throws JOSEObjectBuildException if there was an error building the JSON JWS
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected Mono<GenericJWSPayload<A>> buildJWSPayload(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, String cty, Boolean b64) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		this.checkPayload();
		
		return this.getPayloadEncoder(overridingPayloadEncoder, overridingContentType, cty).apply(this.payload)
			.onErrorMap(e -> new JWSBuildException("Failed to encode JWS payload", e))
			.map(rawPayload -> {
				GenericJWSPayload<A> jwsPayload = new GenericJWSPayload<>(this.payload);
				jwsPayload.setRaw(rawPayload);
				if(b64 == null || b64) {
					jwsPayload.setEncoded(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(rawPayload.getBytes()));
				}

				return jwsPayload;
			});
	}
	
	/**
	 * <p>
	 * Built signature info for building and holding signature specific information.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */	
	protected class SignatureInfo {
		
		final Consumer<GenericJWSHeader> protectedHeaderConfigurer;
		final Consumer<GenericJWSHeader> unprotectedHeaderConfigurer;
		final Publisher<? extends JWK> keys;
		
		JsonJWSHeader signatureProtectedJWSHeader;
		JsonJWSHeader signatureUnprotectedJWSHeader;
		JsonJWSHeader jwsHeader;

		/**
		 * <p>
		 * Creates built signature info.
		 * </p>
		 *
		 * @param protectedHeaderConfigurer   the signature specific protected JWS header configurer
		 * @param unprotectedHeaderConfigurer the signature specific unprotected JWS header configurer
		 * @param keys                        the signature specific keys to consider to sign the JWS
		 */
		public SignatureInfo(Consumer<GenericJWSHeader> protectedHeaderConfigurer, Consumer<GenericJWSHeader> unprotectedHeaderConfigurer, Publisher<? extends JWK> keys) {
			this.protectedHeaderConfigurer = protectedHeaderConfigurer;
			this.unprotectedHeaderConfigurer = unprotectedHeaderConfigurer;
			this.keys = keys;
		}
		
		/**
		 * <p>
		 * Build the JWS headers.
		 * </p>
		 * 
		 * <p>
		 * These includes the signature specific protected JWS header, the signature specific unprotected JWS header and the signature JWS header resulting from the merge of the signature specific
		 * protected JWS header and the signature specific unprotected JWS header.
		 * </p>
		 * 
		 * @throws JWSBuildException if there was an error building the JWS headers
		 */
		public void buildJWSHeaders() throws JWSBuildException {
			this.jwsHeader = new JsonJWSHeader();
			this.jwsHeader.startRecordOverlap();
			if(GenericJsonJWSBuilder.this.protectedHeaderConfigurer != null) {
				this.signatureProtectedJWSHeader = new JsonJWSHeader();
				GenericJsonJWSBuilder.this.protectedHeaderConfigurer.accept(this.signatureProtectedJWSHeader);
				GenericJsonJWSBuilder.this.protectedHeaderConfigurer.accept(this.jwsHeader);
			}
			if(this.protectedHeaderConfigurer != null) {
				if(this.signatureProtectedJWSHeader == null) {
					this.signatureProtectedJWSHeader = new JsonJWSHeader();
				}
				else {
					this.signatureProtectedJWSHeader.startRecordOverlap();
				}
				this.protectedHeaderConfigurer.accept(this.signatureProtectedJWSHeader);
				this.protectedHeaderConfigurer.accept(this.jwsHeader);
				
				Set<String> overlappingParameters = this.signatureProtectedJWSHeader.stopRecordOverlap();
				if(overlappingParameters != null && !overlappingParameters.isEmpty()) {
					throw new JWSBuildException("Signature protected headers and enclosing JSON JWS protected headers must be disjoint: " + overlappingParameters.stream().collect(Collectors.joining(", ")));
				}
			}
			
			if(GenericJsonJWSBuilder.this.unprotectedHeaderConfigurer != null) {
				this.signatureUnprotectedJWSHeader = new JsonJWSHeader();
				GenericJsonJWSBuilder.this.unprotectedHeaderConfigurer.accept(this.signatureUnprotectedJWSHeader);
				GenericJsonJWSBuilder.this.unprotectedHeaderConfigurer.accept(this.jwsHeader);
			}
			if(this.unprotectedHeaderConfigurer != null) {
				if(this.signatureUnprotectedJWSHeader == null) {
					this.signatureUnprotectedJWSHeader = new JsonJWSHeader();
				}
				else {
					this.signatureUnprotectedJWSHeader.startRecordOverlap();
				}
				this.unprotectedHeaderConfigurer.accept(this.signatureUnprotectedJWSHeader);
				this.unprotectedHeaderConfigurer.accept(this.jwsHeader);
				Set<String> overlappingParameters = this.signatureUnprotectedJWSHeader.stopRecordOverlap();
				
				if(overlappingParameters != null && !overlappingParameters.isEmpty()) {
					throw new JWSBuildException("Signature unprotected headers and enclosing JSON JWS unprotected headers must be disjoint: " + overlappingParameters.stream().collect(Collectors.joining(", ")));
				}
			}
			
			Set<String> overlappingParameters = this.jwsHeader.stopRecordOverlap();
			if(!overlappingParameters.isEmpty()) {
				throw new JWSBuildException("Protected and unprotected headers must be disjoint: " + overlappingParameters.stream().collect(Collectors.joining(", ")));
			}
			
			if(this.signatureUnprotectedJWSHeader != null) {
				if(this.signatureUnprotectedJWSHeader.getAlgorithm() != null) {
					throw new JWSBuildException("Algorithm must be set in protected header");
				}
				if(this.signatureUnprotectedJWSHeader.getCritical() != null) {
					throw new JWSBuildException("Critical must be set in protected header");
				}
			}

			if(this.jwsHeader.isBase64EncodePayload() != null) {
				if(this.signatureProtectedJWSHeader == null) {
					this.signatureProtectedJWSHeader = new JsonJWSHeader(); 
				}
				if(this.signatureProtectedJWSHeader.getCritical() == null) {
					this.signatureProtectedJWSHeader.critical("b64");
				}
				this.signatureProtectedJWSHeader.getCritical().add("b64");
			}
		}
	}
}

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
package io.inverno.mod.security.jose.internal.jwe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJsonJOSEObjectBuilder;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.internal.jwa.GenericEncryptedData;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwe.JsonJWE;
import io.inverno.mod.security.jose.jwe.JsonJWEBuilder;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON JWE builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class GenericJsonJWEBuilder<A> extends AbstractJsonJOSEObjectBuilder<A, JsonJWE<A, JsonJWE.BuiltRecipient<A>>, GenericJWEHeader, GenericJsonJWEBuilder<A>> implements JsonJWEBuilder<A, GenericJWEHeader, GenericJsonJWEBuilder<A>> {

	private final List<JWEZip> zips;
	
	private String aad;
	private SecureRandom secureRandom = JOSEUtils.DEFAULT_SECURE_RANDOM;
	
	private final List<RecipientInfo> recipientInfos;
	
	/**
	 * <p>
	 * Creates a generic JSON JWE builder.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the payload type
	 * @param zips                  a list of supported JWE compression algorithms
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public GenericJsonJWEBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, List<JWEZip> zips) {
		super(mapper, dataConversionService, jwkService, type);
		this.zips = zips;
		this.recipientInfos = new LinkedList<>();
	}

	@Override
	public GenericJsonJWEBuilder<A> additionalAuthenticationData(String aad) {
		this.aad = aad;
		return this;
	}

	@Override
	public GenericJsonJWEBuilder<A> secureRandom(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
		return this;
	}
	
	@Override
	public GenericJsonJWEBuilder<A> recipient(Consumer<GenericJWEHeader> headerConfigurer, Publisher<? extends JWK> keys) {
		this.recipientInfos.add(new RecipientInfo(headerConfigurer, keys));
		return this;
	}

	@Override
	public Mono<JsonJWE<A, JsonJWE.BuiltRecipient<A>>> build(String contentType) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(null, contentType);
	}

	@Override
	public Mono<JsonJWE<A, JsonJWE.BuiltRecipient<A>>> build(Function<A, Mono<String>> payloadEncoder) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(payloadEncoder, null);
	}
	
	/**
	 * <p>
	 * Builds a JSON JWE.
	 * </p>
	 * 
	 * <p>
	 * This method checks that the {@code typ}, {@code cty}, {@code enc} and {@code zip} header parameters are consistent for all JSON JWS signatures.
	 * </p>
	 *
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 *
	 * @return a single JSON JWE publisher
	 *
	 * @throws JWEBuildException        if there was an error building the JSON JWE
	 * @throws JOSEObjectBuildException if there was an error building the JSON JWE
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	public Mono<JsonJWE<A, JsonJWE.BuiltRecipient<A>>> build(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		JsonJWEHeader protectedJWEHeader = this.buildProtectedJWEHeader();
		JsonJWEHeader unprotectedJWEHeader = this.buildUnprotectedJWEHeader();
		
		if(protectedJWEHeader != null && unprotectedJWEHeader != null) {
			Set<String> overlappingParameters = protectedJWEHeader.getParametersSet();
			overlappingParameters.retainAll(unprotectedJWEHeader.getParametersSet());
			if(!overlappingParameters.isEmpty()) {
				throw new JWEBuildException("Protected and unprotected headers must be disjoint: " + String.join(", ", overlappingParameters));
			}
		}
		
		String typ = null;		
		String cty = null;
		String enc = null;
		String zip = null;
		for(RecipientInfo recipient : this.recipientInfos) {
			recipient.buildJWEHeaders();
			
			if(typ == null) {
				typ = recipient.jweHeader.getType();
			}
			else if(recipient.jweHeader.getType() != null && !typ.equals(recipient.jweHeader.getType())) {
				throw new JWEBuildException("Recipients have inconsistent types");
			}
			
			if(cty == null) {
				cty = recipient.jweHeader.getContentType();
			}
			else if(recipient.jweHeader.getContentType() != null && !cty.equals(recipient.jweHeader.getContentType())) {
				throw new JWEBuildException("Recipients have inconsistent content types");
			}
			
			if(enc == null) {
				enc = recipient.jweHeader.getEncryptionAlgorithm();
			}
			else if(recipient.jweHeader.getEncryptionAlgorithm() != null && !enc.equals(recipient.jweHeader.getEncryptionAlgorithm())) {
				throw new JWEBuildException("Recipients have inconsistent encryption algorithms");
			}
			
			if(zip == null) {
				zip = recipient.jweHeader.getCompressionAlgorithm();
			}
			else if(recipient.jweHeader.getCompressionAlgorithm() != null && !zip.equals(recipient.jweHeader.getCompressionAlgorithm())) {
				throw new JWEBuildException("Recipients have inconsistent compression algorithms");
			}
		}
		
		return this.buildJWEPayload(overridingPayloadEncoder, overridingContentType, cty)
			.flatMap(jwePayload -> {
				final byte[] raw_aad;
				if(StringUtils.isNotBlank(this.aad)) {
					//  ASCII(Encoded Protected Header || '.' || BASE64URL(JWE AAD))
					byte[] encodedProtectedHeader = protectedJWEHeader != null ? protectedJWEHeader.getEncoded().getBytes(StandardCharsets.US_ASCII) : new byte[0];
					byte[] encodedAAD = this.aad.getBytes(StandardCharsets.US_ASCII);

					raw_aad = new byte[encodedProtectedHeader.length + 1 + encodedAAD.length];
					System.arraycopy(encodedProtectedHeader, 0, raw_aad, 0, encodedProtectedHeader.length);
					raw_aad[encodedProtectedHeader.length] = '.';
					System.arraycopy(encodedAAD, 0, raw_aad, encodedProtectedHeader.length + 1, encodedAAD.length);
				}
				else {
					// ASCII(Encoded Protected Header)
					raw_aad = protectedJWEHeader != null ? protectedJWEHeader.getEncoded().getBytes(StandardCharsets.US_ASCII) : new byte[0];
				}

				RecipientInfo firstRecipient = this.recipientInfos.getFirst();
				Mono<JWE<A>> first = new RecipientJWEBuilder<>(
					this.mapper,
					this.dataConversionService,
					this.jwkService,
					this.type,
					firstRecipient.keys,
					this.zips,
					protectedJWEHeader,
					firstRecipient.recipientJWEHeader,
					firstRecipient.jweHeader,
					jwePayload,
					raw_aad
				)
				.secureRandom(this.secureRandom)
				// overriding encoder and payload must be ignored since payload is already provided
				.build();

				return first.flatMap(firstJWE -> {
					String iv = firstJWE.getInitializationVector();
					String cipherText = firstJWE.getCipherText();
					String tag = firstJWE.getAuthenticationTag();

					if(firstJWE.getCipherText() == null) {
						// We are in the direct use case: algorithm must be consistent and parameters as well
						final String alg = firstRecipient.jweHeader.getAlgorithm();
						final Map<String, Object> processedParameters;
						
						final Set<String> keyAlgProcessedParameters = ((GenericJWEHeader)firstJWE.getHeader()).getProcessedParameters();
						if(keyAlgProcessedParameters != null) {
							if(protectedJWEHeader != null) {
								keyAlgProcessedParameters.removeAll(protectedJWEHeader.getCustomParameters().keySet());
							}
							if(unprotectedJWEHeader != null) {
								keyAlgProcessedParameters.removeAll(unprotectedJWEHeader.getCustomParameters().keySet());
							}
							processedParameters = !keyAlgProcessedParameters.isEmpty() ? keyAlgProcessedParameters.stream().collect(Collectors.toMap(Function.identity(), parameter -> firstJWE.getHeader().getCustomParameters().get(parameter))) : null;
						}
						else {
							processedParameters = null;
						}

						return Flux.concat(
							Mono.just((JsonJWE.BuiltRecipient<A>)new GenericJsonJWE.GenericBuiltRecipient<>(firstRecipient.recipientJWEHeader, firstJWE)),
							Flux.fromIterable(this.recipientInfos)
								.skip(1)
								.map(recipient -> {
									if(recipient.jweHeader.getAlgorithm() != null && !alg.equals(recipient.jweHeader.getAlgorithm())) {
										throw new JWEBuildException("Algorithm must be the same for all recipients when using direct encryption or direct key agreement");
									}
									if(processedParameters != null && processedParameters.entrySet().stream().anyMatch(e -> !e.getValue().equals(recipient.jweHeader.getCustomParameters().get(e.getKey())))) {
										throw new JWEBuildException("Algorithm parameters must be the same for all recipients when using direct encryption or direct key agreement");
									}
									return (JsonJWE.BuiltRecipient<A>)new GenericJsonJWE.GenericBuiltRecipient<>(recipient.recipientJWEHeader, new GenericJWE<>(recipient.jweHeader, jwePayload, iv, tag));
								})
						)
						.collectList()
						.map(recipients -> new GenericJsonJWE<>(protectedJWEHeader, unprotectedJWEHeader, iv, this.aad, cipherText, tag, recipients, this.mapper));
					}
					else {
						GenericEncryptedData zipAndEncryptedPayload = new GenericEncryptedData(Base64.getUrlDecoder().decode(iv), Base64.getUrlDecoder().decode(cipherText), Base64.getUrlDecoder().decode(tag));
						return Flux.concat(
							Mono.just((JsonJWE.BuiltRecipient<A>)new GenericJsonJWE.GenericBuiltRecipient<>(firstRecipient.recipientJWEHeader, firstJWE)),
							Flux.fromIterable(this.recipientInfos)
								.skip(1)
								.flatMap(recipientBuilder -> new RecipientJWEBuilder<>(
										this.mapper,
										this.dataConversionService,
										this.jwkService,
										this.type,
										recipientBuilder.keys,
										this.zips,
										protectedJWEHeader,
										recipientBuilder.recipientJWEHeader,
										recipientBuilder.jweHeader,
										jwePayload,
										raw_aad,
										((GenericJWE<A>)firstJWE).getCEK(),
										zipAndEncryptedPayload
									)
									.secureRandom(this.secureRandom)
									// overriding encoder and payload must be ignored since payload is already provided
									.build()
									.doOnNext(jwe -> {
										// if the cipher text is null, we are mixing direct key management algorithm with encryption/wrapping algorithms which is invalid
										if(jwe.getCipherText() == null) {
											throw new JWEBuildException("Mixing direct encryption or direct key agreement algorithms with encryption or wrapping algorithms");
										}
									})
									.map(jwe -> (JsonJWE.BuiltRecipient<A>)new GenericJsonJWE.GenericBuiltRecipient<>(recipientBuilder.recipientJWEHeader, jwe))
								)
						)
						.collectList()
						.map(recipients -> new GenericJsonJWE<>(protectedJWEHeader, unprotectedJWEHeader, iv, this.aad, cipherText, tag, recipients, this.mapper));
					}
				});
			});
	}
	
	/**
	 * <p>
	 * Builds the protected JSON JWE header.
	 * </p>
	 * 
	 * @return a JSON JWE header
	 * 
	 * @throws JWEBuildException if there was an error building the JWE header 
	 */
	protected JsonJWEHeader buildProtectedJWEHeader() throws JWEBuildException {
		if(this.protectedHeaderConfigurer != null) {
			JsonJWEHeader protectedJWEHeader = new JsonJWEHeader();
			this.protectedHeaderConfigurer.accept(protectedJWEHeader);
			
			try {
				protectedJWEHeader.setEncoded(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(this.mapper.writeValueAsBytes(protectedJWEHeader)));
			} 
			catch(JsonProcessingException e) {
				throw new JWEBuildException("Failed to encode Json JWE protected header", e);
			}
			
			return protectedJWEHeader;
		}
		return null;
	}
	
	/**
	 * <p>
	 * Builds the unprotected JSON JWE header.
	 * </p>
	 * 
	 * @return a JSON JWE header
	 * 
	 * @throws JWEBuildException if there was an error building the JWE header 
	 */
	protected JsonJWEHeader buildUnprotectedJWEHeader() {
		if(this.unprotectedHeaderConfigurer != null) {
			JsonJWEHeader unprotectedJWEHeader = new JsonJWEHeader();
			this.unprotectedHeaderConfigurer.accept(unprotectedJWEHeader);
			return unprotectedJWEHeader;
		}
		return null;
	}
	
	/**
	 * <p>
	 * Builds the JSON JWE payload
	 * </p>
	 * 
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 * @param cty the payload content type defined in JOSE headers
	 * 
	 * @return a single JWE payload publisher
	 * 
	 * @throws JWEBuildException if there was an error building the JWE payload
	 */
	protected Mono<GenericJWEPayload<A>> buildJWEPayload(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, String cty) throws JWEBuildException {
		this.checkPayload();
		return this.getPayloadEncoder(overridingPayloadEncoder, overridingContentType, cty).apply(this.payload)
			.onErrorMap(e -> new JWEBuildException("Failed to encode Json JWE payload", e))
			.map(rawPayload -> {
				GenericJWEPayload<A> jwePayload = new GenericJWEPayload<>(this.payload);
				jwePayload.setRaw(rawPayload);

				return jwePayload;
			});
	}
	
	/**
	 * <p>
	 * Built recipient info for building and holding recipient specific information.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	protected class RecipientInfo {
		
		final Consumer<GenericJWEHeader> headerConfigurer;
		final Publisher<? extends JWK> keys;
		
		JsonJWEHeader recipientJWEHeader;
		JsonJWEHeader jweHeader;
		
		/**
		 * <p>
		 * Creates built recipient info.
		 * </p>
		 *
		 * @param headerConfigurer the recipient specified JWE header configurer
		 * @param keys             the recipient specific keys to consider to secure the CEK
		 */
		public RecipientInfo(Consumer<GenericJWEHeader> headerConfigurer, Publisher<? extends JWK> keys) {
			this.headerConfigurer = headerConfigurer;
			this.keys = keys;
		}
		
		/**
		 * <p>
		 * Builds the JWE headers.
		 * </p>
		 * 
		 * <p>
		 * These includes the recipient specific JWE header and the recipient JWE header resulting from the merge of the JSON JWE protected header, the JSON JWE unprotected header and the recipient
		 * specific JWE header.
		 * </p>
		 * 
		 * @throws JWEBuildException if there was an error building the JWE headers
		 */
		public void buildJWEHeaders() throws JWEBuildException {
			this.jweHeader = new JsonJWEHeader();

			if(GenericJsonJWEBuilder.this.protectedHeaderConfigurer != null) {
				GenericJsonJWEBuilder.this.protectedHeaderConfigurer.accept(this.jweHeader);
			}
			
			if(GenericJsonJWEBuilder.this.unprotectedHeaderConfigurer != null) {
				GenericJsonJWEBuilder.this.unprotectedHeaderConfigurer.accept(this.jweHeader);
			}
			
			if(this.headerConfigurer != null) {
				this.recipientJWEHeader = new JsonJWEHeader();
				this.headerConfigurer.accept(this.recipientJWEHeader);
				
				Set<String> overlappingParameters = this.recipientJWEHeader.getParametersSet();
				overlappingParameters.retainAll(this.jweHeader.getParametersSet());
				if(!overlappingParameters.isEmpty()) {
					throw new JWEBuildException("Recipient, protected and unprotected headers must be disjoint: " + String.join(", ", overlappingParameters));
				}
				this.headerConfigurer.accept(this.jweHeader);
			}
		}
	}
}

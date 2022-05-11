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
import io.inverno.mod.security.jose.internal.AbstractJOSEObjectBuilder;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwa.EncryptingJWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWACipherException;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.NoAlgorithm;
import io.inverno.mod.security.jose.jwa.WrappingJWAKeyManager;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEBuilder;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwe.JWEZipException;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jws.JWSBuildException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON Web Encryption builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class GenericJWEBuilder<A> extends AbstractJOSEObjectBuilder<A, JWEHeader, JWE<A>, GenericJWEHeader, GenericJWEBuilder<A>> implements JWEBuilder<A, GenericJWEHeader, GenericJWEBuilder<A>> {
	
	private static final Logger LOGGER = LogManager.getLogger(GenericJWEBuilder.class);
	
	private final List<JWEZip> zips;
	
	private SecureRandom secureRandom = JOSEUtils.DEFAULT_SECURE_RANDOM;
	
	/**
	 * The JWE header configurer.
	 */
	protected Consumer<GenericJWEHeader> headerConfigurer;

	/**
	 * <p>
	 * Creates a generic JWE builder.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the payload type
	 * @param keys                  the keys to consider to secure the CEK
	 * @param zips                  a list of supported JWE compression algorithms
	 */
	@SuppressWarnings("exports")
	public GenericJWEBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, List<JWEZip> zips) {
		super(mapper, dataConversionService, jwkService, type, keys);
		this.zips = zips;
	}
	
	@Override
	public GenericJWEBuilder<A> header(Consumer<GenericJWEHeader> configurer) {
		this.headerConfigurer = configurer;
		return this;
	}

	@Override
	public GenericJWEBuilder<A> secureRandom(SecureRandom secureRandom) {
		this.secureRandom = secureRandom != null ? secureRandom : JOSEUtils.DEFAULT_SECURE_RANDOM;
		return this;
	}

	@Override
	public Mono<JWE<A>> build(String contentType) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(null, contentType);
	}

	@Override
	public Mono<JWE<A>> build(Function<A, Mono<String>> payloadEncoder) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.build(payloadEncoder, null);
	}
	
	/**
	 * <p>
	 * Builds the JWE.
	 * </p>
	 *
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 *
	 * @return a single JWE publisher
	 *
	 * @throws JWEBuildException        if there was an error building the JWE
	 * @throws JOSEObjectBuildException if there was an error building the JWE
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWE<A>> build(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		GenericJWEHeader jweHeader = this.buildJWEHeader();
		return this.buildJWEPayload(overridingPayloadEncoder, overridingContentType, jweHeader).flatMap(jwePayload -> this.build(
			jweHeader, 
			jwePayload, 
			this.getKeys(jweHeader)
		));
	}

	/**
	 * <p>
	 * Builds the JWE.
	 * </p>
	 * 
	 * @param jweHeader the JWE header
	 * @param jwePayload the JWE payload
	 * @param keys the resolved keys to consider to secure the CEK
	 * 
	 * @return a single JWE publisher
	 * 
	 * @throws JWEBuildException        if there was an error building the JWE
	 * @throws JOSEObjectBuildException if there was an error building the JWE
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWE<A>> build(GenericJWEHeader jweHeader, GenericJWEPayload<A> jwePayload, Flux<? extends JWK> keys) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		final JWEZip payloadZip = this.getPayloadZip(jweHeader);
		if(jweHeader.getAlgorithm().equals(NoAlgorithm.DIR.getAlgorithm())) {
			// Direct encryption
			this.amendJWEHeader(jweHeader, null);
			return keys
				.onErrorStop()
				.map(key -> {
					// 1. Encrypt payload
					// directly encrypt payload with the key
					JWACipher.EncryptedData encryptedData = this.zipAndEncryptPayload(jweHeader, jwePayload, payloadZip, key);
					
					// 2. Assemble JWE
					return (JWE<A>)new GenericJWE<>(
						jweHeader, 
						jwePayload, 
						JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getInitializationVector()), 
						JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getAuthenticationTag())
					);
				})
				.onErrorContinue((e, key) -> LOGGER.warn(() -> "Failed to build JWE with key: " + key, e))
				.next()
				.switchIfEmpty(Mono.error(() -> new JWSBuildException("Failed to build JWE")));
		}
		else {
			Flux<? extends JWK> ceks = this.generateCEK(jweHeader).cache();
			return keys
				.onErrorStop()
				.flatMap(key -> {
					final JWAKeyManager keyManager = key.keyManager(jweHeader.getAlgorithm());
					// We have two cases:
					// - the key manager is a DirectJWAKeyManager
					// - the key manager is a WrappingJWAKeyManager
					if(keyManager instanceof DirectJWAKeyManager) {
						// 1. Derive CEK
						DirectJWAKeyManager.DirectCEK directCEK = ((DirectJWAKeyManager)keyManager).deriveCEK(jweHeader.getEncryptionAlgorithm(), jweHeader.getCustomParameters());
						OCTJWK cek = directCEK.getEncryptionKey();

						// 2. Populate header with CEK encryption custom parameters
						this.amendJWEHeader(jweHeader, directCEK.getMoreHeaderParameters());

						// 3. zip and encrypt payload
						JWACipher.EncryptedData encryptedData = this.zipAndEncryptPayload(jweHeader, jwePayload, payloadZip, cek);

						// 4. Assemble JWE
						return Mono.just((JWE<A>)new GenericJWE<>(
							jweHeader,
							jwePayload,
							JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getInitializationVector()),
							JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getAuthenticationTag()),
							keyManager.getProcessedParameters()
						));
					}
					else if(keyManager instanceof EncryptingJWAKeyManager) {
						return ceks.mapNotNull(cek -> {
							// 1. Encrypt CEK
							EncryptingJWAKeyManager.EncryptedCEK encryptedCEK = ((EncryptingJWAKeyManager)keyManager).encryptCEK(cek, jweHeader.getCustomParameters(), this.secureRandom);

							// 2. Populate header with CEK encryption custom parameters
							this.amendJWEHeader(jweHeader, encryptedCEK.getMoreHeaderParameters());

							// 3. Zip and encrypt payload
							JWACipher.EncryptedData encryptedData = this.zipAndEncryptPayload(jweHeader, jwePayload, payloadZip, cek);

							// 4. Assemble JWE
							return (JWE<A>)new GenericJWE<>(
								jweHeader,
								jwePayload,
								JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getInitializationVector()),
								JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getAuthenticationTag()),
								JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedCEK.getEncryptedKey()),
								cek
							);
						});
					}
					else if(keyManager instanceof WrappingJWAKeyManager) {
						return ceks.mapNotNull(cek -> {
							// 1. Encrypt CEK
							WrappingJWAKeyManager.WrappedCEK wrappedCEK = ((WrappingJWAKeyManager)keyManager).wrapCEK(cek, jweHeader.getCustomParameters(), this.secureRandom);

							// 2. Populate header with CEK encryption custom parameters
							this.amendJWEHeader(jweHeader, wrappedCEK.getMoreHeaderParameters());

							// 3. Zip and encrypt payload
							JWACipher.EncryptedData encryptedData = this.zipAndEncryptPayload(jweHeader, jwePayload, payloadZip, cek);

							// 4. Assemble JWE
							return (JWE<A>)new GenericJWE<>(
								jweHeader,
								jwePayload,
								JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getInitializationVector()),
								JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getAuthenticationTag()),
								JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(wrappedCEK.getWrappedKey()),
								cek
							);
						});
					}
					else {
						throw new IllegalStateException("Key manager must implement " + DirectJWAKeyManager.class + ", " + EncryptingJWAKeyManager.class + " or " + WrappingJWAKeyManager.class);
					}
				})
				.onErrorContinue((e, key) -> LOGGER.warn(() -> "Failed to build JWE with key: " + key, e))
				.next()
				.switchIfEmpty(Mono.error(() -> new JWSBuildException("Failed to build JWE")));
		}
	}
	
	@Override
	protected Set<String> getProcessedParameters() {
		return GenericJWEHeader.PROCESSED_PARAMETERS;
	}
	
	@Override
	protected void checkHeader(JWEHeader header) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		super.checkHeader(header);
		if(StringUtils.isBlank(header.getEncryptionAlgorithm())) {
			throw new JWEBuildException("Encryption algorithm is blank");
		}
	}
	
	/**
	 * <p>
	 * Builds the JWE header.
	 * </p>
	 *
	 * @return the JWE header
	 *
	 * @throws JWEBuildException        if there was an error building the JWE header
	 * @throws JOSEObjectBuildException if there was an error building the JWE header
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected GenericJWEHeader buildJWEHeader() throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		GenericJWEHeader jweHeader = new GenericJWEHeader();
		
		if(this.headerConfigurer != null) {
			this.headerConfigurer.accept(jweHeader);
		}
		
		this.checkHeader(jweHeader);
		
		return jweHeader;
	}
	
	/**
	 * <p>
	 * Builds the JWE payload.
	 * </p>
	 *
	 * @param overridingPayloadEncoder an overriding payload encoder
	 * @param overridingContentType    an overriding payload content type
	 * @param jweHeader                the JWE header
	 *
	 * @return a single JWE payload publisher
	 *
	 * @throws JWEBuildException        if there was an error building the JWE payload
	 * @throws JOSEObjectBuildException if there was an error building the JWE payload
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	protected Mono<GenericJWEPayload<A>> buildJWEPayload(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, GenericJWEHeader jweHeader) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		this.checkPayload();
		
		return this.getPayloadEncoder(overridingPayloadEncoder, overridingContentType, jweHeader).apply(this.payload)
			.onErrorMap(e -> new JWEBuildException("Failed to encode JWE payload", e))
			.map(rawPayload -> {
				GenericJWEPayload<A> jwePayload = new GenericJWEPayload<>(this.payload);
				jwePayload.setRaw(rawPayload);

				return jwePayload;
			});
	}
	
	/**
	 * <p>
	 * Returns the JWE compression algorithm.
	 * </p>
	 * 
	 * @param jweHeader the JWE header
	 * 
	 * @return a JWE compression algorithm or null if the payload must not be compressed
	 * 
	 * @throws JWEBuildException if the payload must be compressed and no corresponding JWE compression algorithm could be found
	 */
	protected JWEZip getPayloadZip(GenericJWEHeader jweHeader) throws JWEBuildException {
		if(StringUtils.isBlank(jweHeader.getCompressionAlgorithm())) {
			return null;
		}
		return this.zips.stream()
			.filter(z -> z.supports(jweHeader.getCompressionAlgorithm()))
			.findFirst()
			.orElseThrow(() -> new JWEBuildException("No JWE zip found supporting compression algorithm: " + jweHeader.getCompressionAlgorithm()));
	}
	
	/**
	 * <p>
	 * Generates Content Encryption Keys used to encrypt the payload.
	 * </p>
	 * 
	 * <p>
	 * This can result in multiple keys depending on the target encryption algorithm and the JWK service setup, the first succeeding key will be retained and the others dropped before they are
	 * generated.
	 * </p>
	 * 
	 * @param jweHeader the JWE header
	 * 
	 * @return a JWK publisher
	 * 
	 * @throws JWKGenerateException if no CEK corresponding to the encryption algorithm could be generated
	 */
	protected Flux<? extends JWK> generateCEK(GenericJWEHeader jweHeader) throws JWKGenerateException {
		return Flux.from(this.jwkService.generate(jweHeader.getEncryptionAlgorithm(), Map.of("secureRandom", this.secureRandom)));
	}
	
	/**
	 * <p>
	 * Amends the JWE header with custom parameters output byt key management and encryption algorithms.
	 * </p>
	 * 
	 * @param header the JWE header
	 * @param moreHeaderParameters custom parameters 
	 * 
	 * @throws JWEBuildException if there was an error amending the JWE header
	 * @throws JOSEObjectBuildException if there was an error amending the JWE header
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected void amendJWEHeader(GenericJWEHeader header, Map<String, Object> moreHeaderParameters) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		try {
			if(moreHeaderParameters != null) {
				moreHeaderParameters.forEach((k, v) -> header.addCustomParameter(k, v));
			}
			header.setRaw(this.mapper.writeValueAsBytes(header));
		}
		catch(JsonProcessingException e) {
			throw new JWEBuildException("Failed to encode JWE header", e);
		}
	}
	
	/**
	 * <p>
	 * Returns additional authentication data.
	 * </p>
	 * 
	 * <p>
	 * For a compact JWE it should be {@code ASCII(Encoded Protected Header)}. For a JSON JWE it should be {@code ASCII(Encoded Protected Header || '.' || BASE64URL(JWE AAD))}.
	 * </p>
	 * 
	 * @param jweHeader the JWE header
	 * 
	 * @return the additional authentication data
	 */
	protected byte[] getAdditionalAuthenticationData(GenericJWEHeader jweHeader) {
		return jweHeader.getEncoded().getBytes(StandardCharsets.US_ASCII);
	}
	
	/**
	 * <p>
	 * Compresses and encrypt the JWE payload.
	 * </p>
	 *
	 * @param jweHeader  the JWE header
	 * @param jwePayload the JWE payload
	 * @param payloadZip the JWE payload compression algorithm
	 * @param cek        the Content Encryption Key
	 *
	 * @return encrypted data
	 *
	 * @throws JWKProcessingException if there was an error obtaining the cipher instance
	 * @throws JWEZipException        if there was an error compressing the payload
	 * @throws JWACipherException     if there was an error encrypting the payload
	 */
	protected JWACipher.EncryptedData zipAndEncryptPayload(GenericJWEHeader jweHeader, GenericJWEPayload<A> jwePayload, JWEZip payloadZip, JWK cek) throws JWKProcessingException, JWEZipException, JWACipherException {
		byte[] aad = this.getAdditionalAuthenticationData(jweHeader);
		JWACipher cipher = cek.cipher(jweHeader.getEncryptionAlgorithm());

		JWACipher.EncryptedData encryptedData = cipher.encrypt(payloadZip != null ? payloadZip.compress(jwePayload.getRaw().getBytes()) : jwePayload.getRaw().getBytes(), aad, this.secureRandom);
		jwePayload.setEncoded(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(encryptedData.getCipherText()));
		
		return encryptedData;
	}
}

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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJOSEObjectReader;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwa.EncryptingJWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.NoAlgorithm;
import io.inverno.mod.security.jose.jwa.WrappingJWAKeyManager;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwe.JWEReadException;
import io.inverno.mod.security.jose.jwe.JWEReader;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON Web Encryption compact reader implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the expected payload type
 */
public class GenericJWEReader<A> extends AbstractJOSEObjectReader<A, JWEHeader, JWE<A>, GenericJWEReader<A>> implements JWEReader<A, GenericJWEReader<A>> {

	private static final Logger LOGGER = LogManager.getLogger(GenericJWEReader.class);
	
	private final List<JWEZip> zips;

	/**
	 * <p>
	 * Creates a generic JWE reader
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the expected payload type
	 * @param keys                  the keys to consider to decrypt the CEK
	 * @param zips                  a list of supported JWE compression algorithms
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public GenericJWEReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, List<JWEZip> zips) {
		super(mapper, dataConversionService, jwkService, type, keys);
		this.zips = zips;
	}
	
	@Override
	public Mono<JWE<A>> read(String compact, String contentType) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(compact, null, contentType);
	}

	@Override
	public Mono<JWE<A>> read(String compact, Function<String, Mono<A>> payloadDecoder) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(compact, payloadDecoder, null);
	}

	/**
	 * <p>
	 * Reads a compact JWE representation.
	 * </p>
	 *
	 * @param compact                  a JWE compact representation
	 * @param overridingPayloadDecoder an overriding payload decoder
	 * @param overridingContentType    an overriding payload content type
	 *
	 * @return a single JWE publisher
	 *
	 * @throws JWEReadException         if there was an error reading the JWE
	 * @throws JWEBuildException        if there was an error building the JWE
	 * @throws JOSEObjectReadException  if there was an error reading the JWE
	 * @throws JOSEObjectBuildException if there was an error building the JWE
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWE<A>> read(String compact, Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		String[] jweParts = compact.split("\\.", -1);
		if(jweParts.length != 5) {
			throw new JWEReadException("Invalid compact JWE");
		}
		GenericJWEHeader jweHeader = this.readJWEHeader(jweParts[0]);
		return this.read(
			jweHeader,
			jweParts[1],
			jweParts[2],
			jweParts[3],
			jweParts[4],
			this.getKeys(jweHeader),
			this.getPayloadDecoder(overridingPayloadDecoder, overridingContentType, jweHeader)
		);
	}
	
	/**
	 * <p>
	 * Reads a compact JWE representation.
	 * </p>
	 *
	 * @param jweHeader                   the JWE header
	 * @param encodedEncryptedKey         the Base64URL encoded JWE encrypted key
	 * @param encodedInitializationVector the Base64URL encoded initialization vector
	 * @param encodedCipherText           the Base64URL encoded cipher text
	 * @param encodedAuthenticationTag    the Base64URL encoded authentication tag
	 * @param keys                        the resolved keys to consider to decrypt the CEK
	 * @param payloadDecoder              the resolved payload decoder
	 *
	 * @return a single JWE publisher
	 *
	 * @throws JWEReadException         if there was an error reading the JWE
	 * @throws JWEBuildException        if there was an error building the JWE
	 * @throws JOSEObjectReadException  if there was an error reading the JWE
	 * @throws JOSEObjectBuildException if there was an error building the JWE
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	private Mono<JWE<A>> read(GenericJWEHeader jweHeader, String encodedEncryptedKey, String encodedInitializationVector, String encodedCipherText, String encodedAuthenticationTag, Flux<? extends JWK> keys, Function<String, Mono<A>> payloadDecoder) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		if(StringUtils.isBlank(encodedCipherText)) {
			throw new JWEReadException("Payload is blank");
		}
		
		final JWEZip payloadZip = this.getPayloadZip(jweHeader.getCompressionAlgorithm());
		
		byte[] iv = Base64.getUrlDecoder().decode(encodedInitializationVector);
		byte[] cipherText = Base64.getUrlDecoder().decode(encodedCipherText);
		byte[] tag = Base64.getUrlDecoder().decode(encodedAuthenticationTag);
		byte[] aad = this.getAdditionalAuthenticationData(jweHeader);
		
		if(jweHeader.getAlgorithm().equals(NoAlgorithm.DIR.getAlgorithm())) {
			if(StringUtils.isNotBlank(encodedEncryptedKey)) {
				throw new JWEReadException("Direct encryption JWE has a non-blank encrypted key");
			}
			
			return Mono.defer(() -> {
				JWEReadException error = new JWEReadException("Failed to read JWE");
				return keys
					.onErrorStop()
					.flatMap(key -> {
						// 1. Get cipher
						JWACipher cipher = key.cipher(jweHeader.getEncryptionAlgorithm());

						// 2. Check critical parameters
						this.checkCriticalParameters(jweHeader.getCritical(), cipher);

						// 3. Decrypt payload
						// directly decrypt payload with the key
						byte[] decryptedPayload = cipher.decrypt(cipherText, aad, iv, tag);

						// 4. Unzip payload
						final String payloadRaw;
						if(payloadZip != null) {
							payloadRaw = new String(payloadZip.decompress(decryptedPayload));
						}
						else {
							payloadRaw = new String(decryptedPayload);
						}

						// 5. Decode payload
						return payloadDecoder.apply(payloadRaw)
							.map(payload -> {
								GenericJWEPayload<A> jwePayload = new GenericJWEPayload<>(payload);
								jwePayload.setRaw(payloadRaw);
								jwePayload.setEncoded(encodedCipherText);

								// 4. Assemble JWE
								return (JWE<A>)new GenericJWE<>(
									jweHeader, 
									jwePayload, 
									encodedInitializationVector, 
									encodedAuthenticationTag
								);
							});
					})
					.onErrorContinue((e, key) -> {
						error.addSuppressed(e);
						LOGGER.debug(() -> "Failed to read JWE with key: " + key, e);
					})
					.next()
					.switchIfEmpty(Mono.error(error));
			});
		}
		else {
			
			return Mono.defer(() -> {
				JWEReadException error = new JWEReadException("Failed to read JWE");
				return keys
					.onErrorStop()
					.flatMap(key -> {
						// 1. Determine CEK
						final JWK cek;
						JWAKeyManager keyManager = key.keyManager(jweHeader.getAlgorithm());
						if(keyManager instanceof DirectJWAKeyManager) {
							if(StringUtils.isNotBlank(encodedEncryptedKey)) {
								throw new JWEReadException("Direct encryption JWE has a non-blank encrypted key");
							}
							DirectJWAKeyManager.DirectCEK directCEK = ((DirectJWAKeyManager)keyManager).deriveCEK(jweHeader.getEncryptionAlgorithm(), jweHeader.getCustomParameters());
							// This is done to replace EPK as Map<String, Object> to the corresponding JWK type in order to get equals() to work
							Map<String, Object> moreHeaderParameters = directCEK.getMoreHeaderParameters();
							if(moreHeaderParameters != null) {
								jweHeader.getCustomParameters().putAll(moreHeaderParameters);
							}
							cek = directCEK.getEncryptionKey();
						}
						else {
							byte[] decodedEncryptedKey = Base64.getUrlDecoder().decode(encodedEncryptedKey);
							if(keyManager instanceof EncryptingJWAKeyManager) {
								if(decodedEncryptedKey.length == 0) {
									throw new JWEReadException("Failed to decrypt JWE because encrypted key is blank and an encrypting key manager was returned by the key");
								}
								cek = ((EncryptingJWAKeyManager)keyManager).decryptCEK(decodedEncryptedKey, jweHeader.getEncryptionAlgorithm(), jweHeader.getCustomParameters());
							}
							else if(keyManager instanceof WrappingJWAKeyManager) {
								if(decodedEncryptedKey.length == 0) {
									throw new JWEReadException("Failed to decrypt JWE because encrypted key is blank and a wrapping key manager was returned by the key");
								}
								cek = ((WrappingJWAKeyManager)keyManager).unwrapCEK(decodedEncryptedKey, jweHeader.getEncryptionAlgorithm(), jweHeader.getCustomParameters());
							}
							else {
								throw new JWAProcessingException("Key manager must implement " + DirectJWAKeyManager.class + ", " + EncryptingJWAKeyManager.class + " or " + WrappingJWAKeyManager.class);
							}
						}

						// 2. Get cipher
						final JWACipher cipher = cek.cipher(jweHeader.getEncryptionAlgorithm());

						// 3. Check critical parameters
						this.checkCriticalParameters(jweHeader.getCritical(), keyManager, cipher);

						// 4. Decrypt payload
						final byte[] decryptedPayload = cipher.decrypt(cipherText, aad, iv, tag);

						// 5. Unzip payload
						final String payloadRaw;
						if(payloadZip != null) {
							payloadRaw = new String(payloadZip.decompress(decryptedPayload));
						}
						else {
							payloadRaw = new String(decryptedPayload);
						}

						// 6. Decode payload
						return payloadDecoder.apply(payloadRaw)
							.map(payload -> {
								GenericJWEPayload<A> jwePayload = new GenericJWEPayload<>(payload);
								jwePayload.setRaw(payloadRaw);
								jwePayload.setEncoded(encodedCipherText);

								// 7. Assemble JWE
								return (JWE<A>)new GenericJWE<>(
									jweHeader, 
									jwePayload, 
									encodedInitializationVector, 
									encodedAuthenticationTag,
									encodedEncryptedKey.isBlank() ? null : encodedEncryptedKey, 
									cek
								);
							});
					})
					.onErrorContinue((e, key) -> {
						error.addSuppressed(e);
						LOGGER.debug(() -> "Failed to read JWE with key: " + key, e);
					})
					.next()
					.switchIfEmpty(Mono.error(error));
			});
		}
	}
	
	@Override
	protected void checkHeader(JWEHeader header) throws JWEReadException, JOSEObjectReadException {
		super.checkHeader(header);
		if(StringUtils.isBlank(header.getEncryptionAlgorithm())) {
			throw new JWEReadException("Encryption algorithm is blank");
		}
	}
	
	/**
	 * <p>
	 * Reads the JWE header.
	 * </p>
	 *
	 * @param encodedHeader the Base64URL encoded JWE header
	 *
	 * @return the JWE header
	 *
	 * @throws JWEReadException        if there was an error reading the JWE header
	 * @throws JOSEObjectReadException if there was an error reading the JWE header
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected GenericJWEHeader readJWEHeader(String encodedHeader) throws JWEReadException, JOSEObjectReadException, JOSEProcessingException {
		try {
			GenericJWEHeader jweHeader = this.mapper.readValue(Base64.getUrlDecoder().decode(encodedHeader), GenericJWEHeader.class);
			this.checkHeader(jweHeader);
			jweHeader.setEncoded(encodedHeader);
			
			return jweHeader;
		} 
		catch(IOException e) {
			throw new JWEReadException("Failed to read JWE header", e);
		}
	}
	
	/**
	 * <p>
	 * Returns expected additional authentication data.
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
	 * Returns the JWE compression algorithm.
	 * </p>
	 * 
	 * @param zip the JWE compression algorithm name
	 * 
	 * @return a JWE compression algorithm or null if the compression algorithm name is blank
	 * 
	 * @throws JWEReadException if the compression algorithm name is not blank and no corresponding JWE compression algorithm could be found
	 */
	protected JWEZip getPayloadZip(String zip) throws JWEReadException {
		if(StringUtils.isBlank(zip)) {
			return null;
		}
		return this.zips.stream()
			.filter(z -> z.supports(zip))
			.findFirst()
			.orElseThrow(() -> new JWEReadException("No JWE zip found supporting compression algorithm: " + zip));
	}
}

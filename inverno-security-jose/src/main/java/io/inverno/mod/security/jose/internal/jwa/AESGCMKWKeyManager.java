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
package io.inverno.mod.security.jose.internal.jwa;

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWK;
import io.inverno.mod.security.jose.jwa.JWACipherException;
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * AES GCM KW encrypting key manager implementation.
 * </p>
 * 
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>A128GCMKW</li>
 * <li>A192GCMKW</li>
 * <li>A256GCMKW</li>
 * </ul>
 * 
 * <p>
 * It processes the following parameters:
 * </p>
 * 
 * <ul>
 * <li>{@code iv}: initialization vector</li>
 * <li>{@code tag}: authentication tag</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class AESGCMKWKeyManager extends AbstractEncryptingJWAKeyManager<OCTJWK, OCTAlgorithm> {

	/**
	 * The set of parameters processed by the key manager.
	 */
	public static final Set<String> PROCESSED_PARAMETERS = Set.of("iv", "tag");
	
	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<OCTAlgorithm> SUPPORTED_ALGORITHMS = Set.of(OCTAlgorithm.A128GCMKW, OCTAlgorithm.A192GCMKW, OCTAlgorithm.A256GCMKW);
	
	private SecretKey secretKey;
	
	/**
	 * <p>
	 * Creates an AES GCM KW key manager.
	 * </p>
	 *
	 * @param jwk       an octet JWK
	 * @param algorithm an octet JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AESGCMKWKeyManager(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an AES GCM KW key manager.
	 * </p>
	 * 
	 * @param jwk an octet JWK
	 */
	protected AESGCMKWKeyManager(OCTJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
		this.secretKey = this.jwk.toSecretKey().orElseThrow(() -> new JWAProcessingException("JWK is missing secret key"));
		if(this.secretKey.getEncoded().length != this.algorithm.getEncryptionKeyLength()) {
			throw new JWAProcessingException("Key length " + this.secretKey.getEncoded().length + "does not match algorithm " + this.algorithm.getAlgorithm());
		}
	}

	@Override
	public Set<String> getProcessedParameters() {
		return PROCESSED_PARAMETERS;
	}
	
	@Override
	protected EncryptedCEK doEncryptCEK(OCTJWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException {
		try {
			byte[] iv =  JOSEUtils.generateInitializationVector(secureRandom, this.algorithm.getInitializationVectorLength());
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, new GCMParameterSpec(this.algorithm.getAuthenticationTagLength() * 8, iv), secureRandom);
			cipher.updateAAD(new byte[0]);
			byte[] encryptedData = cipher.doFinal(Base64.getUrlDecoder().decode(cek.getKeyValue()));
			
			byte[] cipherText = new byte[encryptedData.length - this.algorithm.getAuthenticationTagLength()];
			byte[] authenticationTag = new byte[this.algorithm.getAuthenticationTagLength()];
			System.arraycopy(encryptedData, 0, cipherText, 0, cipherText.length);
			System.arraycopy(encryptedData, cipherText.length, authenticationTag, 0, authenticationTag.length);
			
			return new GenericEncryptedCEK(
				cipherText, 
				Map.of(
					"iv", JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(iv),
					"tag", JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(authenticationTag)
				)
			);
		} 
		catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new JWACipherException(e);
		}
	}

	@Override
	protected OCTJWK doDecryptCEK(byte[] encrypted_key, OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException {
		byte[] iv = getInitializationVector(parameters);
		byte[] tag = getAuthenticationTag(parameters);
		
		if(iv.length != this.algorithm.getInitializationVectorLength()) {
			throw new JWACipherException("Initialization vector length " + iv.length + "does not match algorithm " + this.algorithm.getAlgorithm());
		}
		try {
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, this.secretKey, new GCMParameterSpec(this.algorithm.getAuthenticationTagLength() * 8, iv));
			cipher.updateAAD(new byte[0]);
			
			byte[] encryptedData = new byte[encrypted_key.length + tag.length];
			System.arraycopy(encrypted_key, 0, encryptedData, 0, encrypted_key.length);
			System.arraycopy(tag, 0, encryptedData, encrypted_key.length, tag.length);
			
			GenericOCTJWK cek = new GenericOCTJWK(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(cipher.doFinal(encryptedData)));
			cek.setAlgorithm(octEnc);
			
			return cek;
		} 
		catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new JWACipherException(e);
		}
	}
	
	/**
	 * <p>
	 * Extracts and verifies the initialization vectors from the specified parameters map.
	 * </p>
	 * 
	 * <p>
	 * Parameters are typically custom parameters in a JOSE header. The initialization vector parameter value should be encoded as Base64URL.
	 * </p>
	 * 
	 * @param parameters the parameters map
	 * 
	 * @return the initialization vector
	 * 
	 * @throws JWAKeyManagerException if the initialization vector parameter is missing
	 */
	private static byte[] getInitializationVector(Map<String, Object> parameters) throws JWAKeyManagerException {
		String iv = (String)parameters.get("iv");
		if(StringUtils.isBlank(iv)) {
			throw new JWAKeyManagerException("Missing initialization vector");
		}
		return Base64.getUrlDecoder().decode(iv);
	}
	
	/**
	 * <p>
	 * Extracts and verifies the authentication tag from the specified parameters map.
	 * </p>
	 * 
	 * <p>
	 * Parameters are typically custom parameters in a JOSE header. The authentication tag parameter value should be encoded as Base64URL.
	 * </p>
	 * 
	 * @param parameters the parameters map
	 * 
	 * @return the authentication tag
	 * 
	 * @throws JWAKeyManagerException if the authentication tag parameter is missing
	 */
	private static byte[] getAuthenticationTag(Map<String, Object> parameters) throws JWAKeyManagerException {
		String tag = (String)parameters.get("tag");
		if(StringUtils.isBlank(tag)) {
			throw new JWAKeyManagerException("Missing authentication tag");
		}
		return Base64.getUrlDecoder().decode(tag);
	}
}

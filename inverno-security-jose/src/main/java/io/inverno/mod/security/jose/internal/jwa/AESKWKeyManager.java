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
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * <p>
 * AES KW wrapping key manager implementation.
 * </p>
 * 
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>A128KW</li>
 * <li>A192KW</li>
 * <li>A256KW</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class AESKWKeyManager extends AbstractWrappingJWAKeyManager<OCTJWK, OCTAlgorithm> {

	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<OCTAlgorithm> SUPPORTED_ALGORITHMS = Set.of(OCTAlgorithm.A128KW, OCTAlgorithm.A192KW, OCTAlgorithm.A256KW);
	
	private SecretKey secretKey;
	
	/**
	 * <p>
	 * Creates an AES KW key manager.
	 * </p>
	 *
	 * @param jwk       an octet JWK
	 * @param algorithm an octet JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AESKWKeyManager(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an AES KW key manager.
	 * </p>
	 * 
	 * @param jwk an octet JWK
	 */
	protected AESKWKeyManager(OCTJWK jwk) {
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
	protected WrappedCEK doWrapCEK(OCTJWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException {
		return cek.toSecretKey().map(cekSecretKey -> {
			try {
				Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
				cipher.init(Cipher.WRAP_MODE, this.secretKey, secureRandom);
				byte[] encryptedKey = cipher.wrap(cekSecretKey);

				return new GenericWrappedCEK(encryptedKey);
			} 
			catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
				throw new JWAKeyManagerException(e);
			}
		})
		.orElseThrow(() -> new JWAKeyManagerException("CEK secret key is missing"));
	}

	@Override
	protected OCTJWK doUnwrapCEK(byte[] encrypted_key, OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException {
		try {
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			cipher.init(Cipher.UNWRAP_MODE, this.secretKey);
			
			SecretKey decryptedKey = (SecretKey)cipher.unwrap(encrypted_key, "AES", Cipher.SECRET_KEY);
			GenericOCTJWK cek = new GenericOCTJWK(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(decryptedKey.getEncoded()), decryptedKey, false);
			cek.setAlgorithm(octEnc);
			
			return cek;
		} 
		catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			throw new JWAKeyManagerException(e);
		}
	}
}

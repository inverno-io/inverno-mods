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
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * <p>
 * RSA key manager implementation.
 * </p>
 *
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>RSA1_5</li>
 * <li>RSA-OAEP</li>
 * <li>RSA-OAEP-256</li> 
 * <li>RSA-OAEP-384</li> 
 * <li>RSA-OAEP-512</li> 
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class RSAKeyManager extends AbstractEncryptingJWAKeyManager<RSAJWK, RSAAlgorithm> {
	
	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<RSAAlgorithm> SUPPORTED_ALGORITHMS = Set.of(RSAAlgorithm.RSA1_5, RSAAlgorithm.RSA_OAEP, RSAAlgorithm.RSA_OAEP_256, RSAAlgorithm.RSA_OAEP_384, RSAAlgorithm.RSA_OAEP_512);
	
	/**
	 * <p>
	 * Creates a RSA key manager.
	 * </p>
	 *
	 * @param jwk       a RSA JWK
	 * @param algorithm a RSA JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public RSAKeyManager(RSAJWK jwk, RSAAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates a RSA key manager.
	 * </p>
	 * 
	 * @param jwk a RSA JWK
	 */
	protected RSAKeyManager(RSAJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
	}

	@Override
	protected EncryptedCEK doEncryptCEK(OCTJWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException {
		try {
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			if(this.algorithm.getSignatureParameter() != null) {
				AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("OAEP");
				algorithmParameters.init(this.algorithm.getSignatureParameter());
				cipher.init(Cipher.ENCRYPT_MODE, this.jwk.toPublicKey(), algorithmParameters, secureRandom);
			}
			else {
				cipher.init(Cipher.ENCRYPT_MODE, this.jwk.toPublicKey(), secureRandom);
			}
			
			byte[] encryptedKey = cipher.doFinal(Base64.getUrlDecoder().decode(cek.getKeyValue()));
			
			return new GenericEncryptedCEK(encryptedKey);
		} 
		catch(InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidParameterSpecException | InvalidAlgorithmParameterException e) {
			throw new JWAKeyManagerException(e);
		}
	}

	@Override
	protected OCTJWK doDecryptCEK(byte[] encrypted_key, OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException {
		return this.jwk.toPrivateKey().map(privateKey -> {
			try {
				Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
				if(this.algorithm.getSignatureParameter() != null) {
					AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("OAEP");
					algorithmParameters.init(this.algorithm.getSignatureParameter());
					cipher.init(Cipher.DECRYPT_MODE, privateKey, algorithmParameters);
				}
				else {
					cipher.init(Cipher.DECRYPT_MODE, privateKey);
				}
				
				GenericOCTJWK cek = new GenericOCTJWK(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(cipher.doFinal(encrypted_key)));
				cek.setAlgorithm(octEnc);
				
				return cek;
			} 
			catch(InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidParameterSpecException | InvalidAlgorithmParameterException e) {
				throw new JWAKeyManagerException(e);
			}
		})
		.orElseThrow(() -> new JWAKeyManagerException("JWK is missing RSA private exponent"));
	}
}

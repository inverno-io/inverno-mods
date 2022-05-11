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
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWACipherException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * <p>
 * AES GCM cipher implementation.
 * </p>
 *
 * <p>
 * It supports the following cipher algorithms:
 * </p>
 * 
 * <ul>
 * <li>A128GCM</li>
 * <li>A192GCM</li>
 * <li>A256GCM</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class AESGCMCipher extends AbstractJWACipher {

	/**
	 * The set of algorithms supported by the cipher.
	 */
	public static final Set<OCTAlgorithm> SUPPORTED_ALGORITHMS = Set.of(OCTAlgorithm.A128GCM, OCTAlgorithm.A192GCM, OCTAlgorithm.A256GCM);
	
	private SecretKey secretKey;
	
	/**
	 * <p>
	 * Creates an AES GCM cipher.
	 * </p>
	 *
	 * @param jwk       an octet JWK
	 * @param algorithm an octet JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AESGCMCipher(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an AES GCM cipher.
	 * </p>
	 * 
	 * @param jwk an octet JWK
	 */
	protected AESGCMCipher(OCTJWK jwk) {
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
	protected JWACipher.EncryptedData doEncrypt(byte[] data, byte[] aad, SecureRandom secureRandom) throws JWACipherException {
		try {
			byte[] iv = JOSEUtils.generateInitializationVector(secureRandom, this.algorithm.getInitializationVectorLength());
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, new GCMParameterSpec(this.algorithm.getAuthenticationTagLength() * 8, iv), secureRandom);
			cipher.updateAAD(aad);
			byte[] encryptedData = cipher.doFinal(data);
			
			byte[] cipherText = new byte[encryptedData.length - this.algorithm.getAuthenticationTagLength()];
			byte[] authenticationTag = new byte[this.algorithm.getAuthenticationTagLength()];
			System.arraycopy(encryptedData, 0, cipherText, 0, cipherText.length);
			System.arraycopy(encryptedData, cipherText.length, authenticationTag, 0, authenticationTag.length);
			
			return new GenericEncryptedData(iv, cipherText, authenticationTag);
		} 
		catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new JWACipherException(e);
		}
	}

	@Override
	protected byte[] doDecrypt(byte[] cipherText, byte[] aad, byte[] iv, byte[] tag) throws JWACipherException {
		if(iv.length != this.algorithm.getInitializationVectorLength()) {
			throw new JWACipherException("Initialization vector length " + iv.length + "does not match algorithm " + this.algorithm.getAlgorithm());
		}
		try {
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, this.secretKey, new GCMParameterSpec(this.algorithm.getAuthenticationTagLength() * 8, iv));
			cipher.updateAAD(aad);
			
			byte[] encryptedData = new byte[cipherText.length + tag.length];
			System.arraycopy(cipherText, 0, encryptedData, 0, cipherText.length);
			System.arraycopy(tag, 0, encryptedData, cipherText.length, tag.length);
			
			return cipher.doFinal(encryptedData);
		} 
		catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new JWACipherException(e);
		}
	}
}

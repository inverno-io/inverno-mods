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
import io.inverno.mod.security.jose.jwa.JWACipherException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * <p>
 * AES CBC cipher implementation.
 * </p>
 * 
 * <p>
 * It supports the following cipher algorithms:
 * </p>
 * 
 * <ul>
 * <li>A128CBC-HS256</li>
 * <li>A192CBC-HS384</li>
 * <li>A256CBC-HS512</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class AESCBCCipher extends AbstractJWACipher {
	
	/**
	 * The set of algorithms supported by the cipher.
	 */
	public static final Set<OCTAlgorithm> SUPPORTED_ALGORITHMS = Set.of(OCTAlgorithm.A128CBC_HS256, OCTAlgorithm.A192CBC_HS384, OCTAlgorithm.A256CBC_HS512);

	private SecretKey digestSecretKey;	
	private SecretKey secretKey;

	/**
	 * <p>
	 * Creates an AES CBC cipher.
	 * </p>
	 *
	 * @param jwk       an octet JWK
	 * @param algorithm an octet JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AESCBCCipher(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an AES CBC cipher.
	 * </p>
	 * 
	 * @param jwk an octet JWK
	 */
	protected AESCBCCipher(OCTJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
		byte[] key = Base64.getUrlDecoder().decode(jwk.getKeyValue());
		if(key.length != this.algorithm.getEncryptionKeyLength() + this.algorithm.getMacKeyLength()) {
			throw new JWAProcessingException("Key length " + key.length + "does not match algorithm " + this.algorithm.getAlgorithm());
		}
		this.digestSecretKey = new SecretKeySpec(key, 0, this.algorithm.getMacKeyLength(), this.algorithm.getMacAlgorithm());
		this.secretKey = new SecretKeySpec(key, this.algorithm.getMacKeyLength(), this.algorithm.getEncryptionKeyLength(), "AES");
	}

	@Override
	protected EncryptedData doEncrypt(byte[] data, byte[] aad, SecureRandom secureRandom) throws JWACipherException {
		byte[] iv = JOSEUtils.generateInitializationVector(secureRandom, this.algorithm.getInitializationVectorLength());
		byte[] cipherText = this.cipherText(data, iv, secureRandom);
		byte[] authenticationTag = Arrays.copyOf(this.computeMac(aad, iv, cipherText), this.algorithm.getAuthenticationTagLength());
		
		return new GenericEncryptedData(iv, cipherText, authenticationTag);
	}
	
	@Override
	protected byte[] doDecrypt(byte[] cipherText, byte[] aad, byte[] iv, byte[] tag) throws JWACipherException {
		if(iv.length != this.algorithm.getInitializationVectorLength()) {
			throw new JWACipherException("Initialization vector length " + iv.length + " does not match algorithm " + this.algorithm.getAlgorithm());
		}
		
		if(!Arrays.equals(tag, Arrays.copyOf(this.computeMac(aad, iv, cipherText), this.algorithm.getAuthenticationTagLength()))) {
			throw new JWACipherException("Invalid authentication tag");
		}
		return this.decrypt(cipherText, iv);
	}
	
	/**
	 * <p>
	 * Encrypt the specified data using the specified initialization vector and secure random
	 * </p>
	 *
	 * @param data         the data to encrypt
	 * @param iv           the initialization vector
	 * @param secureRandom a secure random
	 *
	 * @return the encrypted text
	 *
	 * @throws JWACipherException if there was an error encrypting the data
	 */
	private byte[] cipherText(byte[] data, byte[] iv, SecureRandom secureRandom) throws JWACipherException {
		try {
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, new IvParameterSpec(iv), secureRandom);
			return cipher.doFinal(data);
		} 
		catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new JWACipherException(e);
		}
	}
	
	/**
	 * <p>
	 * Computes the MAC from the additional authentication data, the initialization vector and the cipher text as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-5.2">RFC7518 Section 5.2</a>.
	 * </p>
	 * 
	 * @param aad the additional authentication data
	 * @param iv the initialization vector
	 * @param cipherText the cipher text
	 * 
	 * @return a MAC result
	 * 
	 * @throws JWACipherException if there was an error computing the MAC
	 */
	private byte[] computeMac(byte[] aad, byte[] iv, byte[] cipherText) throws JWACipherException {
		byte[] al = ByteBuffer.allocate(8).putLong(Integer.toUnsignedLong(aad.length * 8)).array();
		
		byte[] input = new byte[aad.length + iv.length + cipherText.length + al.length];
		System.arraycopy(aad, 0, input, 0, aad.length);
		System.arraycopy(iv, 0, input, aad.length, iv.length);
		System.arraycopy(cipherText, 0, input, aad.length + iv.length, cipherText.length);
		System.arraycopy(al, 0, input, aad.length + iv.length + cipherText.length, al.length);
		
		try {
			Mac sig = Mac.getInstance(this.algorithm.getMacAlgorithm());
			sig.init(this.digestSecretKey);
			return sig.doFinal(input);
		} 
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new JWACipherException(e);
		}
	}
	
	/**
	 * <p>
	 * Decrypts the specified cipher text using the specified initialization vector.
	 * </p>
	 * 
	 * @param cipherText the encrypted text
	 * @param iv the initialization vector
	 * 
	 * @return decrypted data
	 * 
	 * @throws JWACipherException if there was an error decrypting the data
	 */
	private byte[] decrypt(byte[] cipherText, byte[] iv) throws JWACipherException {
		try {
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, this.secretKey, new IvParameterSpec(iv));
			return cipher.doFinal(cipherText);
		} 
		catch(NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			throw new JWACipherException(e);
		}
	}
}

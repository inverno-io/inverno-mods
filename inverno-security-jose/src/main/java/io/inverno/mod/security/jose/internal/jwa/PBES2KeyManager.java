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
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWK;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Password-based (PBES2) key manager implementation.
 * </p>
 * 
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>PBES2-HS256+A128KW</li>
 * <li>PBES2-HS384+A192KW</li>
 * <li>PBES2-HS512+A256KW</li>
 * </ul>
 * 
 * <p>
 * It processes the following parameters:
 * </p>
 * 
 * <ul>
 * <li>{@code p2s}: PBES2 Salt Input</li>
 * <li>{@code p2c}: PBES2 Count</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class PBES2KeyManager extends AbstractEncryptingJWAKeyManager<PBES2JWK, PBES2Algorithm> {
	
	/**
	 * The set of parameters processed by the key manager.
	 */
	public static final Set<String> PROCESSED_PARAMETERS = Set.of("p2s", "p2c");
	
	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<PBES2Algorithm> SUPPORTED_ALGORITHMS = Set.of(PBES2Algorithm.PBES2_HS256_A128KW, PBES2Algorithm.PBES2_HS384_A192KW, PBES2Algorithm.PBES2_HS512_A256KW);
	
	/**
	 * <p>
	 * Creates a PBES2 key manager.
	 * </p>
	 *
	 * @param jwk       a password-based JWK
	 * @param algorithm a password-based JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public PBES2KeyManager(PBES2JWK jwk, PBES2Algorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates a PBES2 key manager.
	 * </p>
	 *
	 * @param jwk a password-based JWK
	 */
	protected PBES2KeyManager(PBES2JWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
	}

	@Override
	public Set<String> getProcessedParameters() {
		return PROCESSED_PARAMETERS;
	}
	
	@Override
	protected EncryptedCEK doEncryptCEK(OCTJWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException {
		return cek.toSecretKey().map(cekSecretKey -> {
			try {
				// Derive key
				SecretKeyFactory skf = SecretKeyFactory.getInstance(this.algorithm.getJcaAlgorithm());
				byte[] p2s = getP2s(parameters, false, secureRandom);
				int p2c = getP2c(parameters, false);
				PBEKeySpec derivedKeySpec = new PBEKeySpec(new String(Base64.getUrlDecoder().decode(this.jwk.getPassword())).toCharArray(), computeSaltValue(this.algorithm, p2s), p2c, this.algorithm.getEncryptionKeyLength() * 8);
				SecretKey derivedKey = new SecretKeySpec(skf.generateSecret(derivedKeySpec).getEncoded(), "AES") ;

				// Encrypt cek
				Cipher cipher = Cipher.getInstance(this.algorithm.getJcaEncryptionAlgorithm());
				cipher.init(Cipher.WRAP_MODE, derivedKey);

				return new GenericEncryptedCEK(
					cipher.wrap(cekSecretKey), 
					Map.of(
						"p2s", JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(p2s),
						"p2c", p2c
					)
				);
			} 
			catch(NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
				throw new JWAKeyManagerException(e);
			}
		})
		.orElseThrow(() -> new JWAKeyManagerException("CEK secret key is missing"));
	}

	@Override
	protected OCTJWK doDecryptCEK(byte[] encrypted_key, OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException {
		try {
			// Derive key
			SecretKeyFactory skf = SecretKeyFactory.getInstance(this.algorithm.getJcaAlgorithm());
			byte[] p2s = getP2s(parameters, true, null);
			int p2c = getP2c(parameters, true);
			PBEKeySpec derivedKeySpec = new PBEKeySpec(new String(Base64.getUrlDecoder().decode(this.jwk.getPassword())).toCharArray(), computeSaltValue(this.algorithm, p2s), p2c, this.algorithm.getEncryptionKeyLength() * 8);
			SecretKey derivedKey = new SecretKeySpec(skf.generateSecret(derivedKeySpec).getEncoded(), "AES");
			
			// Decrypt cek
			Cipher cipher = Cipher.getInstance(this.algorithm.getJcaEncryptionAlgorithm());
			cipher.init(Cipher.UNWRAP_MODE, derivedKey);
			
			SecretKey decryptedKey = (SecretKey) cipher.unwrap(encrypted_key, "AES", Cipher.SECRET_KEY);
			GenericOCTJWK cek = new GenericOCTJWK(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(decryptedKey.getEncoded()), decryptedKey, true);
			cek.setAlgorithm(octEnc);
			
			return cek;
		} 
		catch(NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException e) {
			throw new JWAKeyManagerException(e);
		}
	}
	
	/**
	 * <p>
	 * Computes the salt value.
	 * <p>
	 *
	 * @param algorithm the algorithm
	 * @param salt      the extracted or generated PBES2 salt input
	 *
	 * @return the computed salt value
	 */
	private static byte[] computeSaltValue(PBES2Algorithm algorithm, byte[] salt) {
		byte[] utf8_alg = algorithm.getAlgorithm().getBytes(StandardCharsets.UTF_8);
		
		byte[] saltValue = new byte[utf8_alg.length + 1 + salt.length];
		
		System.arraycopy(utf8_alg, 0, saltValue, 0, utf8_alg.length);
		System.arraycopy(salt, 0, saltValue, utf8_alg.length + 1, salt.length);
		
		return saltValue;
	}
	
	/**
	 * <p>
	 * Extracts the PBES2 salt input from the specified parameters map or generates a new one.
	 * </p>
	 * 
	 * <p>
	 * Parameters are typically custom parameters in a JOSE header. When {@code p2s} is missing, it is assumed that we are a producer and a new salt input must then be generated.
	 * </p>
	 * 
	 * @param parameters the parameters map
	 * @param failOnMissing indicates whether the method should fail if the parameters map does not contain the {@code p2s} parameter
	 * @param secureRandom a secure random
	 * 
	 * @return the extracted PBES2 salt input or a new PBES2 salt input 
	 * 
	 * @throws JWAKeyManagerException if there was an error extracting or generating the PBES2 salt input
	 */
	private static byte[] getP2s(Map<String, Object> parameters, boolean failOnMissing, SecureRandom secureRandom) throws JWAKeyManagerException {
		byte[] p2s;
		String p2ss = parameters != null ? (String)parameters.get("p2s") : null;
		if(StringUtils.isNotBlank(p2ss)) {
			p2s = Base64.getUrlDecoder().decode(p2ss);
		}
		else if(failOnMissing) {
			throw new JWAKeyManagerException("Missing PBES2 salt input");
		}
		else {
			return JOSEUtils.generateSalt(secureRandom, PBES2Algorithm.DEFAULT_SALT_LENGTH);
		}
			
		if(p2s.length < PBES2Algorithm.MINIMUM_SALT_LENGTH) {
			throw new JWAKeyManagerException("PBES2 salt input must be at least " + PBES2Algorithm.MINIMUM_SALT_LENGTH + " bytes long");
		}
		return p2s;
	}
	
	/**
	 * <p>
	 * Extracts the PBES2 count from the specified parameters map or returns the default one (see {@link PBES2Algorithm#DEFAULT_ITERATION_COUNT}).
	 * </p>
	 * 
	 * @param parameters the parameters map
	 * @param failOnMissing indicates whether the method should fail if the parameters map does not contain the {@code p2c} parameter
	 * 
	 * @return the extracted PBES2 count or the default PBES2 count
	 * 
	 * @throws JWAKeyManagerException if there was an error extracting the PBES2 count
	 */
	private static int getP2c(Map<String, Object> parameters, boolean failOnMissing) {
		Integer p2c = parameters != null ? (Integer)parameters.get("p2c") : null;
		if(p2c == null) {
			if(failOnMissing) {
				throw new JWAKeyManagerException("Missing PBES2 iteration count");
			}
			return PBES2Algorithm.DEFAULT_ITERATION_COUNT;
		}
		
		if(p2c < PBES2Algorithm.MINIMUM_ITERATION_COUNT) {
			throw new JWAKeyManagerException("PBES2 iteration count must be at least " + PBES2Algorithm.MINIMUM_ITERATION_COUNT);
		}
		return p2c;
	}
}

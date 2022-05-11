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
package io.inverno.mod.security.jose.internal.jwk.oct;

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWK;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.JWASignatureException;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwa.NoAlgorithm;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Generic Octet JSON Web Key implementation. 
 * </p>
 * 
 * <p>
 * It supports the following algorithms
 * </p>
 * 
 * <ul>
 * <li>HS256</li>
 * <li>HS384</li>
 * <li>HS512</li>
 * <li>A128KW</li>
 * <li>A256KW</li>
 * <li>A512KW</li>
 * <li>A128GCMKW</li>
 * <li>A192GCMKW</li>
 * <li>A256GCMKW</li>
 * <li>A128GCM</li>
 * <li>A192GCM</li>
 * <li>A256GCM</li>
 * <li>A128CBC-HS256</li>
 * <li>A192CBC-HS384</li>
 * <li>A256CBC-HS512</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericOCTJWK extends AbstractJWK implements OCTJWK {

	private final String k;
	
	private OCTAlgorithm octAlg;
	
	private Optional<SecretKey> secretKey;
	
	private Map<OCTAlgorithm, JWASigner> signers;
	private Map<OCTAlgorithm, JWACipher> ciphers;
	private Map<OCTAlgorithm, JWAKeyManager> keyManagers;
	
	/**
	 * <p>
	 * Creates an untrusted OCT JWK.
	 * </p>
	 */
	public GenericOCTJWK() {
		this(null, null, false);
	}
	
	/**
	 * <p>
	 * Creates an untrusted generic OCT JWK with the specified key value.
	 * </p>
	 * 
	 * @param k the key value encoded as Base64URL without padding
	 */
	public GenericOCTJWK(String k) {
		this(k, null, false);
	}
	
	/**
	 * <p>
	 * Creates a generic OCT JWK with the specified key value and secret key.
	 * </p>
	 * 
	 * @param k       the key value encoded as Base64URL without padding
	 * @param key     a secret key
	 * @param trusted true to create a trusted JWK, false otherwise
	 */
	public GenericOCTJWK(String k, SecretKey key, boolean trusted) {
		super(KEY_TYPE, key, trusted);
		this.k = k;
		this.secretKey = key != null ? Optional.of(key) : null;
	}
	
	/**
	 * <p>
	 * Sets the Octet JWA algorithm.
	 * </p>
	 * 
	 * @param octAlg an OCT algorithm
	 */
	public void setAlgorithm(OCTAlgorithm octAlg) {
		this.octAlg = octAlg;
		super.setAlgorithm(octAlg != null ? octAlg.getAlgorithm() : null);
	}
	
	@Override
	public void setAlgorithm(String alg) {
		this.octAlg = alg != null ? OCTAlgorithm.fromAlgorithm(alg) : null;
		super.setAlgorithm(alg);
	}

	@Override
	public String getKeyValue() {
		return this.k;
	}
	
	@Override
	public OCTJWK trust() {
		this.trusted = true;
		return this;
	}

	@Override
	public Optional<SecretKey> toSecretKey() throws JWKProcessingException {
		if(this.secretKey == null) {
			this.secretKey = Optional.ofNullable(this.k)
				.map(sk -> new SecretKeySpec(Base64.getUrlDecoder().decode(sk), "AES"));
		}
		return this.secretKey;
	}
	
	@Override
	public OCTJWK toPublicJWK() {
		GenericOCTJWK jwk = new GenericOCTJWK();
		jwk.setAlgorithm(this.octAlg);
		jwk.setKeyId(this.kid);
		jwk.setKeyOperations(this.key_ops);
		jwk.setPublicKeyUse(this.use);
		
		return jwk;
	}
	
	@Override
	public OCTJWK minify() {
		GenericOCTJWK jwk = new GenericOCTJWK(this.k, (SecretKey)this.key, this.trusted);
		
		return jwk;
	}

	@Override
	public String toJWKThumbprint(MessageDigest digest) {
		return toJWKThumbprint(digest, this.k, this.kty);
	}
	
	/**
	 * <p>
	 * Generates and returns an OCT JWK thumbprint using the specified digest.
	 * </p>
	 * 
	 * @param digest the message digest to use
	 * @param k      a key value encoded as Base64URL without padding
	 * @param kty    the key type ({@code OCT}}
	 * 
	 * @return an OCT JWK thumbprint or null if some input data are null
	 */
	static String toJWKThumbprint(MessageDigest digest, String k, String kty) {
		if(k == null || kty == null) {
			return null;
		}
		StringBuilder input = new StringBuilder();
		input.append('{');
		input.append("\"k\":\"").append(k).append("\",");
		input.append("\"kty\":\"").append(kty).append("\"");
		input.append('}');
		
		return JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(digest.digest(input.toString().getBytes()));
	}
	
	@Override
	public boolean supportsAlgorithm(String alg) {
		if(NoAlgorithm.DIR.getAlgorithm().equals(alg)) {
			return true;
		}
		try {
			OCTAlgorithm.fromAlgorithm(alg);
			return true;
		}
		catch(IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public JWASigner signer() throws JWKProcessingException {
		this.checkSignature(this.octAlg);
		return this.getSigner(this.octAlg);
	}
	
	@Override
	public JWASigner signer(String alg) {
		if(StringUtils.isBlank(alg)) {
			return this.signer();
		}
		OCTAlgorithm algorithm = OCTAlgorithm.fromAlgorithm(alg);
		this.checkSignature(algorithm);
		return this.getSigner(algorithm);
	}
	
	@Override
	protected void checkSignature(JWAAlgorithm<?> algorithm) throws JWKProcessingException {
		super.checkSignature(algorithm);
		if(this.k == null) {
			throw new JWASignatureException("JWK secret key is missing");
		}
	}

	private JWASigner getSigner(OCTAlgorithm algorithm) {
		if(this.signers == null) {
			this.signers = new HashMap<>();
		}
		return this.signers.computeIfAbsent(algorithm, ign -> algorithm.createSigner(this));
	}

	@Override
	public JWACipher cipher() throws JWKProcessingException {
		this.checkEncryption(this.octAlg);
		return this.getCipher(octAlg);
	}

	@Override
	public JWACipher cipher(String alg) throws JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			return this.cipher();
		}
		OCTAlgorithm algorithm = OCTAlgorithm.fromAlgorithm(alg);
		this.checkEncryption(algorithm);
		return this.getCipher(algorithm);
	}
	
	@Override
	protected void checkEncryption(JWAAlgorithm<?> algorithm) throws JWKProcessingException {
		super.checkEncryption(algorithm);
		if(this.k == null) {
			throw new JWASignatureException("JWK secret key is missing");
		}
	}
	
	private	JWACipher getCipher(OCTAlgorithm algorithm) {
		if(this.ciphers == null) {
			this.ciphers = new HashMap<>();
		}
		return this.ciphers.computeIfAbsent(algorithm, ign -> {
			return algorithm.createCipher(this);
		});
	}

	@Override
	public JWAKeyManager keyManager() throws JWKProcessingException {
		this.checkKeyManagement(this.octAlg);
		return this.getKeyManager(this.octAlg);
	}

	@Override
	public JWAKeyManager keyManager(String alg) throws JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			return this.keyManager();
		}
		OCTAlgorithm algorithm = OCTAlgorithm.fromAlgorithm(alg);
		this.checkKeyManagement(algorithm);
		return this.getKeyManager(algorithm);
	}
	
	@Override
	protected void checkKeyManagement(JWAAlgorithm<?> algorithm) throws JWAKeyManagerException {
		super.checkKeyManagement(algorithm);
		if(this.k == null) {
			throw new JWAKeyManagerException("JWK secret key is missing");
		}
	}
	
	private	JWAKeyManager getKeyManager(OCTAlgorithm algorithm) {
		if(this.keyManagers == null) {
			this.keyManagers = new HashMap<>();
		}
		return this.keyManagers.computeIfAbsent(algorithm, ign -> {
			return algorithm.createKeyManager(this);
		});
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(k);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericOCTJWK other = (GenericOCTJWK) obj;
		return Objects.equals(k, other.k);
	}
}

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
package io.inverno.mod.security.jose.internal.jwk.pbes2;

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWK;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWK;
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
 * Generic Password-based JSON Web Key implementation.
 * </p>
 * 
 * <p>
 * It supports the following algorithms
 * </p>
 * 
 * <ul>
 * <li>PBES2-HS256+A128KW</li>
 * <li>PBES2-HS384+A192KW</li>
 * <li>PBES2-HS512+A256KW</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericPBES2JWK extends AbstractJWK implements PBES2JWK {

	private final String p;
	
	private PBES2Algorithm pbes2Alg;
	
	private Optional<SecretKey> secretKey;
	
	private Map<PBES2Algorithm, JWAKeyManager> keyManagers;
	
	/**
	 * <p>
	 * Creates an untrusted PBES2 JWK.
	 * </p>
	 */
	public GenericPBES2JWK() {
		this(null, null, false);
	}
	
	/**
	 * <p>
	 * Creates an untrusted generic PBES2 JWK with the specified password value.
	 * </p>
	 * 
	 * @param p the password value encoded as Base64URL without padding
	 */
	public GenericPBES2JWK(String p) {
		this(p, null, false);
	}
	
	/**
	 * <p>
	 * Creates a generic PBES2 JWK with the specified key value and secret key.
	 * </p>
	 *
	 * @param p       the password value encoded as Base64URL without padding
	 * @param key     a secret key
	 * @param trusted true to create a trusted JWK, false otherwise
	 */
	public GenericPBES2JWK(String p, SecretKey key, boolean trusted) {
		super(PBES2JWK.KEY_TYPE, key, trusted);
		
		this.p = p;
		this.secretKey = key != null ? Optional.of(key) : null;
	}
	
	/**
	 * <p>
	 * Sets the PBES2 JWA algorithm.
	 * </p>
	 * 
	 * @param pbes2Alg a PBES2 algorithm
	 */
	public void setAlgorithm(PBES2Algorithm pbes2Alg) {
		this.pbes2Alg = pbes2Alg;
		super.setAlgorithm(pbes2Alg != null ? pbes2Alg.getAlgorithm() : null);
	}
	
	@Override
	public void setAlgorithm(String alg) {
		this.pbes2Alg = alg != null ? PBES2Algorithm.fromAlgorithm(alg) : null;
		super.setAlgorithm(alg);
	}
	
	@Override
	public String getPassword() {
		return this.p;
	}
	
	@Override
	public PBES2JWK trust() {
		this.trusted = true;
		return this;
	}

	@Override
	public Optional<SecretKey> toSecretKey() throws JWKProcessingException {
		if(this.secretKey == null) {
			this.secretKey = Optional.ofNullable(this.p)
				.map(p -> {
					if(this.pbes2Alg != null) {
						return new SecretKeySpec(Base64.getUrlDecoder().decode(this.p), this.pbes2Alg.getJcaAlgorithm());
					}
					return new SecretKeySpec(Base64.getUrlDecoder().decode(this.p), null);
				});
		}
		return this.secretKey;
	}

	@Override
	public PBES2JWK toPublicJWK() {
		GenericPBES2JWK jwk = new GenericPBES2JWK();
		jwk.setAlgorithm(this.pbes2Alg);
		jwk.setKeyId(this.kid);
		jwk.setKeyOperations(this.key_ops);
		jwk.setPublicKeyUse(this.getPublicKeyUse());
		
		return jwk;
	}

	@Override
	public PBES2JWK minify() {
		GenericPBES2JWK jwk = new GenericPBES2JWK(this.p, (SecretKey)this.key, this.trusted);
		
		return jwk;
	}
	
	@Override
	public String toJWKThumbprint(MessageDigest digest) {
		return toJWKThumbprint(digest, this.p, this.kty);
	}
	
	/**
	 * <p>
	 * Generates and returns a PBES2 JWK thumbprint using the specified digest.
	 * </p>
	 * 
	 * @param digest the message digest to use
	 * @param p      a password value encoded as Base64URL without padding
	 * @param kty    the key type ({@code OCT}}
	 * 
	 * @return a PBES2 JWK thumbprint or null if some input data are null
	 */
	static String toJWKThumbprint(MessageDigest digest, String p, String kty) {
		if(p == null || kty == null) {
			return null;
		}
		StringBuilder input = new StringBuilder();
		input.append('{');
		input.append("\"k\":\"").append(p).append("\",");
		input.append("\"kty\":\"").append(kty).append("\"");
		input.append('}');
		
		return JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(digest.digest(input.toString().getBytes()));
	}

	@Override
	public boolean supportsAlgorithm(String alg) {
		try {
			PBES2Algorithm.fromAlgorithm(alg);
			return true;
		}
		catch(IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public JWAKeyManager keyManager() throws JWKProcessingException {
		this.checkKeyManagement(this.pbes2Alg);
		return this.getKeyManager(this.pbes2Alg);
	}

	@Override
	public JWAKeyManager keyManager(String alg) throws JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			return this.keyManager();
		}
		PBES2Algorithm algorithm = PBES2Algorithm.fromAlgorithm(alg);
		this.checkKeyManagement(algorithm);
		return this.getKeyManager(algorithm);
	}
	
	@Override
	protected void checkKeyManagement(JWAAlgorithm<?> algorithm) throws JWAKeyManagerException {
		super.checkKeyManagement(algorithm);
		if(this.p == null) {
			throw new JWAKeyManagerException("JWK secret key is missing");
		}
	}
	
	/**
	 * <p>
	 * Returns the key manager corresponding to the specified algorithm.
	 * </p>
	 * 
	 * <p>
	 * This method creates the key manager if it wasn't already created.
	 * </p>
	 * 
	 * @param algorithm a PBES2 algorithm
	 * 
	 * @return a key manager
	 */
	private	JWAKeyManager getKeyManager(PBES2Algorithm algorithm) {
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
		result = prime * result + Objects.hash(p);
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
		GenericPBES2JWK other = (GenericPBES2JWK) obj;
		return Objects.equals(p, other.p);
	}
}

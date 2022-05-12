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
package io.inverno.mod.security.jose.internal.jwk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Base JSON Web Key implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public abstract class AbstractJWK implements JWK {
	
	private static MessageDigest DEFAULT_THUMBPRINT_DIGEST;
	
	/**
	 * The underlying key.
	 */
	protected final Key key;
	
	/**
	 * The key type parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.1">RFC7517 Section 4.1</a>.
	 */
	protected final String kty;
	
	/**
	 * The Public Key Use parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.2">RFC7517 Section 4.2</a>.
	 */
	protected String use;
	
	/**
	 * The Key Operations parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	protected Set<String> key_ops;
	
	/**
	 * The Algorithm parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.4">RFC7517 Section 4.4</a>.
	 */
	protected String alg;
	
	/**
	 * The Key id parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.5">RFC7517 Section 4.5</a>.
	 */
	protected String kid;
	
	/**
	 * Indicates whether the JWK can be trusted.
	 */
	protected boolean trusted;
	
	/**
	 * <p>
	 * Creates an untrusted JWK.
	 * </p>
	 * 
	 * @param kty the key type
	 * 
	 * @throws JWKProcessingException if the key type is blank
	 */
	protected AbstractJWK(String kty) throws JWKProcessingException {
		this(kty, null, false);
	}
	
	/**
	 * <p>
	 * Creates a JWK with the specified key.
	 * </p>
	 * 
	 * @param kty     the key type
	 * @param key     a key
	 * @param trusted true to create a trusted JWK, false otherwise
	 * 
	 * @throws JWKProcessingException if the key type is blank
	 */
	protected AbstractJWK(String kty, Key key, boolean trusted) throws JWKProcessingException {
		if(StringUtils.isBlank(kty)) {
			throw new JWKProcessingException("Key type is blank");
		}
		this.kty = kty;
		this.key = key;
		this.trusted = trusted;
	}
	
	/**
	 * <p>
	 * Returns the default message digest to generate JWK thumbprint.
	 * </p>
	 * 
	 * @return a message digest
	 */
	public static MessageDigest getDefaultThumbprintDigest() {
		if(DEFAULT_THUMBPRINT_DIGEST == null) {
			try {
				DEFAULT_THUMBPRINT_DIGEST = MessageDigest.getInstance("SHA-256");
			}
			catch(NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
		}
		return DEFAULT_THUMBPRINT_DIGEST;
	}
	
	@Override
	public String getKeyType() {
		return kty;
	}

	@Override
	public String getPublicKeyUse() {
		return use;
	}

	/**
	 * <p>
	 * Sets the intended use of the public key: signature ({@code sig}) or encryption ({@code enc}).
	 * </p>
	 * 
	 * @param use the public key use
	 */
	public void setPublicKeyUse(String use) {
		this.use = use;
	}
	
	@Override
	public Set<String> getKeyOperations() {
		return this.key_ops;
	}

	/**
	 * <p>
	 * Sets the key operations for which the key is intended to be used.
	 * </p>
	 * 
	 * @param key_ops a set of key operations
	 */
	public void setKeyOperations(Set<String> key_ops) {
		this.key_ops = key_ops != null && !key_ops.isEmpty() ? Collections.unmodifiableSet(key_ops) : null;
	}

	@Override
	public String getAlgorithm() {
		return alg;
	}

	/**
	 * <p>
	 * Sets the algorithm intended for use with the key.
	 * </p>
	 * 
	 * @param alg the JWA algorithm
	 */
	public void setAlgorithm(String alg) {
		this.alg = alg;
	}

	@Override
	public String getKeyId() {
		return kid;
	}

	/**
	 * <p>
	 * Sets the id of the key.
	 * </p>
	 * 
	 * @param kid the key id
	 */
	public void setKeyId(String kid) {
		this.kid = kid;
	}

	/**
	 * <p>
	 * Returns the key.
	 * </p>
	 * 
	 * @return a key
	 */
	@JsonIgnore
	public Key getKey() {
		return key;
	}

	@Override
	@JsonIgnore
	public boolean isTrusted() {
		return this.trusted;
	}
	
	@Override
	public JWASigner signer() throws JWKProcessingException {
		throw new JWKProcessingException("JWK does not support signing operations");
	}

	@Override
	public JWASigner signer(String alg) throws JWKProcessingException {
		throw new JWKProcessingException("JWK does not support signing operations");
	}
	
	/**
	 * <p>
	 * Checks that the key and the specified algorithm supports signature operations.
	 * </p>
	 * 
	 * @param algorithm a JWA algorithm
	 * 
	 * @throws JWKProcessingException if the key and/or the specified algorithm do not support signature operations
	 */
	protected void checkSignature(JWAAlgorithm<?> algorithm) throws JWKProcessingException {
		if(algorithm == null) {
			throw new JWKProcessingException("Missing algorithm");
		}
		if(this.use != null && !this.use.equals(JWK.USE_SIG)) {
			throw new JWKProcessingException("JWK is not to be used for signature");
		}
		if(this.key_ops != null && !(this.key_ops.contains(JWK.KEY_OP_SIGN) || this.key_ops.contains(JWK.KEY_OP_VERIFY))) {
			throw new JWKProcessingException("JWK does not support signing operations");
		}
		if(this.alg != null && !this.alg.equals(algorithm.getAlgorithm())) {
			throw new JWKProcessingException("JWK algorithm " + this.alg + " does not match " + algorithm);
		}
	}

	@Override
	public JWACipher cipher() throws JWKProcessingException {
		throw new JWKProcessingException("JWK does not support encryption operations");
	}

	@Override
	public JWACipher cipher(String alg) throws JWKProcessingException {
		throw new JWKProcessingException("JWK does not support encryption operations");
	}
	
	/**
	 * <p>
	 * Checks that the key and the specified algorithm supports encryption operations.
	 * </p>
	 * 
	 * @param algorithm a JWA algorithm
	 * 
	 * @throws JWKProcessingException if the key and/or the specified algorithm do not support encryption operations
	 */
	protected void checkEncryption(JWAAlgorithm<?> algorithm) throws JWKProcessingException {
		if(algorithm == null) {
			throw new JWKProcessingException("Missing algorithm");
		}
		if(this.use != null && !this.use.equals(JWK.USE_ENC)) {
			throw new JWKProcessingException("JWK is not to be used for encryption");
		}
		if(this.key_ops != null && !(this.key_ops.contains(JWK.KEY_OP_ENCRYPT) || this.key_ops.contains(JWK.KEY_OP_DECRYPT))) {
			throw new JWKProcessingException("JWK does not support encryption operations");
		}
		if(this.alg != null && !this.alg.equals(algorithm.getAlgorithm())) {
			throw new JWKProcessingException("JWK algorithm " + this.alg + " does not match " + algorithm);
		}
	}

	@Override
	public JWAKeyManager keyManager() throws JWKProcessingException {
		throw new JWKProcessingException("JWK does not support key management operations");
	}

	@Override
	public JWAKeyManager keyManager(String alg) throws JWKProcessingException {
		throw new JWKProcessingException("JWK does not support key management operations");
	}
	
	/**
	 * <p>
	 * Checks that the key and the specified algorithm supports key management operations.
	 * </p>
	 * 
	 * @param algorithm a JWA algorithm
	 * 
	 * @throws JWKProcessingException if the key and/or the specified algorithm do not support key management operations
	 */
	protected void checkKeyManagement(JWAAlgorithm<?> algorithm) throws JWKProcessingException {
		if(algorithm == null) {
			throw new JWKProcessingException("Missing algorithm");
		}
		if(this.use != null && !this.use.equals(JWK.USE_ENC)) {
			throw new JWKProcessingException("JWK is not to be used for encryption");
		}
		if(this.key_ops != null && !(this.key_ops.contains(JWK.KEY_OP_ENCRYPT) || this.key_ops.contains(JWK.KEY_OP_DECRYPT) || this.key_ops.contains(JWK.KEY_OP_WRAP_KEY) || this.key_ops.contains(JWK.KEY_OP_UNWRAP_KEY) || this.key_ops.contains(JWK.KEY_OP_DERIVE_KEY))) {
			throw new JWKProcessingException("JWK does not support key management operations");
		}
		if(this.alg != null && !this.alg.equals(algorithm.getAlgorithm())) {
			throw new JWKProcessingException("JWK algorithm " + this.alg + " does not match " + algorithm);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(alg, key_ops, kid, kty, use);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractJWK other = (AbstractJWK) obj;
		return Objects.equals(alg, other.alg) && Objects.equals(key_ops, other.key_ops)
				&& Objects.equals(kid, other.kid) && Objects.equals(kty, other.kty) && Objects.equals(use, other.use);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("{");
		str.append("\"kty\":\"").append(this.kty).append("\"");
		if(StringUtils.isNotBlank(this.kid)) {
			str.append(",\"kid\":\"").append(this.kid).append("\"");
		}
		if(StringUtils.isNotBlank(this.alg)) {
			str.append(",\"alg\":\"").append(this.alg).append("\"");
		}
		if(StringUtils.isNotBlank(this.use)) {
			str.append(",\"use\":\"").append(this.use).append("\"");
		}
		if(this.key_ops != null && !this.key_ops.isEmpty()) {
			str.append(",\"key_ops\":[").append(this.key_ops.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", "))).append("]");
		}
		str.append("}");
		
		return str.toString();
	}
}

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
package io.inverno.mod.security.jose.jwk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWK;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWASigner;
import java.security.MessageDigest;
import java.util.Set;

/**
 * <p>
 * A JSON Web Key as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517">RFC7517</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface JWK {
	
	/**
	 * Default JWK thumbprint digest.
	 */
	MessageDigest DEFAULT_THUMBPRINT_DIGEST = AbstractJWK.getDefaultThumbprintDigest();
	
	/**
	 * Signature public key use as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.1">RFC7517 Section 4.2</a>.
	 */
	static final String USE_SIG = "sig";
	/**
	 * Encryption public key use as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.1">RFC7517 Section 4.2</a>.
	 */
	static final String USE_ENC = "enc";
	
	/**
	 * Compute digital signature or MAC operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_SIGN = "sign";
	/**
	 * Verify digital signature or MAC operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_VERIFY = "verify";
	/**
	 * Encrypt content operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_ENCRYPT = "encrypt";
	/**
	 * Decrypt content and validate decryption operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_DECRYPT = "decrypt";
	/**
	 * Encrypt key operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_WRAP_KEY = "wrapKey";
	/**
	 * Decrypt key and validate decryption operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_UNWRAP_KEY = "unwrapKey";
	/**
	 * Derive key operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_DERIVE_KEY = "deriveKey";
	/**
	 * Derive bits not to be used as a key operation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	static final String KEY_OP_DERIVE_BITS = "deriveBits";

	/**
	 * <p>
	 * Returns the key type.
	 * </p>
	 * 
	 * @return the key type
	 */
	@JsonProperty("kty")
	String getKeyType();

	/**
	 * <p>
	 * Returns the public key use.
	 * </p>
	 * 
	 * @return the public key use
	 */
	@JsonProperty("use")
	String getPublicKeyUse();
	
	/**
	 * <p>
	 * Returns the set of key operations.
	 * </p>
	 * 
	 * @return the set of key operations
	 */
	@JsonProperty("key_ops")
	Set<String> getKeyOperations();

	/**
	 * <p>
	 * Returns the algorithm intended for use with the key.
	 * </p>
	 * 
	 * @return the key algorithm
	 */
	@JsonProperty("alg")
	String getAlgorithm();

	/**
	 * <p>
	 * Returns the key id.
	 * </p>
	 * 
	 * @return the key id
	 */
	@JsonProperty("kid")
	String getKeyId();
	
	/**
	 * <p>
	 * Determines whether this key is trusted.
	 * </p>
	 * 
	 * <p>
	 * Untrusted keys are typically resolved from unsecured JOSE headers (e.g. no x5c, no x5u...), the are excluded when reading a JOSE object for obvious security reasons.
	 * </p>
	 * 
	 * <p>
	 * Implementations can rely on trust stores or certificate paths validation to determine whether a key is trusted. It is also possible to explicitly trust a key by invoking the {@link #trust()}.
	 * method. 
	 * </p>
	 * 
	 * @return true if the key is trusted, false otherwise.
	 */
	@JsonIgnore
	boolean isTrusted();
	
	/**
	 * <p>
	 * Trusts the key explicitly.
	 * </p>
	 * 
	 * <p>
	 * This should be used with care when the authenticity of an untrusted key has been established through external means.
	 * </p>
	 * 
	 * @return this JWK
	 */
	JWK trust();

	/**
	 * <p>
	 * Returns a public and safe to share representation of the key.
	 * </p>
	 * 
	 * @return a public representation of this JWK
	 */
	JWK toPublicJWK();
	
	/**
	 * <p>
	 * Returns a minified representation of the key only containing required data.
	 * </p>
	 * 
	 * <p>
	 * Note that the returned JWK may contain private data.
	 * </p>
	 * 
	 * @return a minified representation of this JWK
	 */
	JWK minify();
	
	/**
	 * <p>
	 * Generates and returns the JWK thumbprint using the defaul digest.
	 * </p>
	 * 
	 * @return the JWK thumbprint
	 */
	default String toJWKThumbprint() {
		return this.toJWKThumbprint(DEFAULT_THUMBPRINT_DIGEST);
	}
	
	/**
	 * <p>
	 * Generates and returns the JWK thumbprint using the specified digest.
	 * </p>
	 * 
	 * @param digest the message digest to use
	 * 
	 * @return the JWK thumbprint
	 */
	String toJWKThumbprint(MessageDigest digest);
	
	/**
	 * <p>
	 * Determines whether the JWK supports the specified JWA algorithm.
	 * </p>
	 * 
	 * @param alg a JWA algorithm
	 * 
	 * @return true if the algorithm is supported, false otherwise
	 */
	boolean supportsAlgorithm(String alg);
	
	/**
	 * <p>
	 * Returns a signer using this JWK.
	 * </p>
	 * 
	 * @return a signer
	 * 
	 * @throws JWKProcessingException if the JWK does not support signature operations (i.e. missing algorithm, algorithm is not a signature algorithm...)
	 */
	JWASigner signer() throws JWKProcessingException;
	
	/**
	 * <p>
	 * Returns a signer using this JWK and the specified algorithm.
	 * </p>
	 * 
	 * @param alg a JWA signature algorithm
	 *
	 * @return a signer
	 *
	 * @throws JWKProcessingException if the JWK does not support signature operations or if the specified algorithm is not a supported signature algorithm
	 */
	JWASigner signer(String alg) throws JWKProcessingException;
	
	/**
	 * <p>
	 * Returns a cipher using this JWK.
	 * </p>
	 * 
	 * @return a cipher
	 * 
	 * @throws JWKProcessingException if the JWK does not support encryption operations (i.e. missing algorithm, algorithm is not an encryption algorithm...)
	 */
	JWACipher cipher() throws JWKProcessingException;
	
	/**
	 * <p>
	 * Returns a cipher using this JWK and the specified algorithm.
	 * </p>
	 * 
	 * @param alg a JWA encryption algorithm
	 *
	 * @return a cipher
	 *
	 * @throws JWKProcessingException if the JWK does not support encryption operations or if the specified algorithm is not a supported encryption algorithm
	 */
	JWACipher cipher(String alg) throws JWKProcessingException;
	
	/**
	 * <p>
	 * Returns a key manager using this JWK.
	 * </p>
	 * 
	 * @return a key manager
	 * 
	 * @throws JWKProcessingException if the JWK does not support key management operations (i.e. missing algorithm, algorithm is not a key management algorithm...)
	 */
	JWAKeyManager keyManager() throws JWKProcessingException;
	
	/**
	 * <p>
	 * Returns a key manager using this JWK and the specified algorithm.
	 * </p>
	 * 
	 * @param alg a JWA key management algorithm
	 *
	 * @return a key manager
	 *
	 * @throws JWKProcessingException if the JWK does not support key management operations or if the specified algorithm is not a supported key management algorithm
	 */
	JWAKeyManager keyManager(String alg) throws JWKProcessingException;
	
	@Override
	int hashCode();
	
	@Override
	boolean equals(Object obj);
}

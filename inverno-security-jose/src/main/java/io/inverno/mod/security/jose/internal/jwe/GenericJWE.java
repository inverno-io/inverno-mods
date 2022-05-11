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
package io.inverno.mod.security.jose.internal.jwe;

import io.inverno.mod.security.jose.internal.AbstractJOSEObject;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwk.JWK;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Generic JSON Web Encryption implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class GenericJWE<A> extends AbstractJOSEObject<A, JWEHeader, GenericJWEPayload<A>> implements JWE<A> {

	private final String initializationVector;
	private final String authenticationTag;
	private final String encryptedKey;
	private final JWK cek;
	private final Set<String> processedParameters;
	
	private final String compact;
	
	/**
	 * <p>
	 * Creates a generic JWE.
	 * </p>
	 *
	 * @param header               the JWE header
	 * @param payload              the JWE payload
	 * @param initializationVector the Base64URL encoded initialization vector without padding
	 * @param authenticationTag    the Base64URL encoded authentication tag without padding
	 */
	public GenericJWE(GenericJWEHeader header, GenericJWEPayload<A> payload, String initializationVector, String authenticationTag) {
		this(header, payload, initializationVector, authenticationTag, null, null, null);
	}
	
	/**
	 * <p>
	 * Creates a generic JWE with custom processed parameters.
	 * </p>
	 * 
	 * @param header               the JWE header
	 * @param payload              the JWE payload
	 * @param initializationVector the Base64URL encoded initialization vector without padding
	 * @param authenticationTag    the Base64URL encoded authentication tag without padding
	 * @param processedParameters  custom processed parameters
	 */
	public GenericJWE(GenericJWEHeader header, GenericJWEPayload<A> payload, String initializationVector, String authenticationTag, Set<String> processedParameters) {
		this(header, payload, initializationVector, authenticationTag, processedParameters, null, null);
	}
	
	/**
	 * <p>
	 * Creates a generic JWE with encrypted key and corresponding content encryption key.
	 * </p>
	 * 
	 * @param header               the JWE header
	 * @param payload              the JWE payload
	 * @param initializationVector the Base64URL encoded initialization vector without padding
	 * @param authenticationTag    the Base64URL encoded authentication tag without padding
	 * @param encryptedKey         the encrypted key
	 * @param cek                  the corresponding CEK
	 */
	public GenericJWE(GenericJWEHeader header, GenericJWEPayload<A> payload, String initializationVector, String authenticationTag, String encryptedKey, JWK cek) {
		this(header, payload, initializationVector, authenticationTag, null, encryptedKey, cek);
	}
	
	/**
	 * <p>
	 * Creates a generic JWE with custom processed parameters, encrypted key and corresponding content encryption key.
	 * </p>
	 * 
	 * @param header               the JWE header
	 * @param payload              the JWE payload
	 * @param initializationVector the Base64URL encoded initialization vector without padding
	 * @param authenticationTag    the Base64URL encoded authentication tag without padding
	 * @param processedParameters  custom processed parameters
	 * @param encryptedKey         the encrypted key
	 * @param cek                  the corresponding CEK
	 */
	private GenericJWE(GenericJWEHeader header, GenericJWEPayload<A> payload, String initializationVector, String authenticationTag, Set<String> processedParameters, String encryptedKey, JWK cek) {
		super(header, payload);
		this.initializationVector = initializationVector;
		this.authenticationTag = authenticationTag;
		this.processedParameters = processedParameters;
		this.encryptedKey = encryptedKey;
		this.cek = cek;
		
		// if alg=dir, the encrypted key is null
		this.compact = header.getEncoded() + "." + (this.encryptedKey != null ? this.encryptedKey : "") + "." + this.initializationVector + "." + this.payload.getEncoded() + "." + this.authenticationTag;
	}

	/**
	 * <p>
	 * Returns the attached CEK when available.
	 * </p>
	 * 
	 * @return the attached CEK or null
	 */
	JWK getCEK() {
		return this.cek;
	}

	/**
	 * <p>
	 * Returns the custom processed parameters.
	 * </p>
	 * 
	 * @return a set of processed parameters or null
	 */
	public Set<String> getProcessedParameters() {
		return processedParameters;
	}
	
	@Override
	public String getEncryptedKey() {
		return this.encryptedKey;
	}

	@Override
	public String getInitializationVector() {
		return this.initializationVector;
	}
	
	@Override
	public String getCipherText() {
		return this.payload.getEncoded();
	}

	@Override
	public String getAuthenticationTag() {
		return this.authenticationTag;
	}

	@Override
	public String toCompact() {
		return this.compact;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(authenticationTag, encryptedKey, initializationVector);
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
		GenericJWE<?> other = (GenericJWE<?>) obj;
		return Objects.equals(authenticationTag, other.authenticationTag)
				&& Objects.equals(encryptedKey, other.encryptedKey)
				&& Objects.equals(initializationVector, other.initializationVector);
	}
}

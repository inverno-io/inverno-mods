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
package io.inverno.mod.security.jose.jwe;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.SecurityException;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.TokenAuthentication;
import io.inverno.mod.security.jose.JOSEProcessingException;
import java.util.Optional;

/**
 * <p>
 * A token authentication that wraps the original authentication in a JWE and uses its compact representation as token value.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the original authentication type
 */
public class JWEAuthentication<A extends Authentication> implements TokenAuthentication {

	/**
	 * The JWE.
	 */
	private final JWE<A> jwe;
	
	/**
	 * The security error resulting from a JOSE processing error.
	 */
	private final SecurityException cause;

	/**
	 * <p>
	 * Creates a JWE authentication with the specified JWE.
	 * </p>
	 * 
	 * @param jwe a JWE wrapping the original authentication
	 */
	public JWEAuthentication(JWE<A> jwe) {
		this.jwe = jwe;
		this.cause = null;
	}
	
	/**
	 * <p>
	 * Creates a denied JWE authentication with the specified security error.
	 * </p>
	 * 
	 * @param cause a security error or null
	 */
	public JWEAuthentication(SecurityException cause) {
		this.jwe = null;
		this.cause = cause;
	}
	
	/**
	 * <p>
	 * Creates a denied JWE authentication after a JOSE processing error.
	 * </p>
	 * 
	 * @param cause a JOSE processing error or null
	 */
	JWEAuthentication(JOSEProcessingException cause) {
		this.jwe = null;
		this.cause = cause != null ? new InvalidCredentialsException("Invalid token", cause) : null;
	}

	/**
	 * <p>
	 * Returns the JWE.
	 * </p>
	 * 
	 * @return the JWE or null if unauthenticated
	 */
	public JWE<A> getJwe() {
		return this.jwe;
	}

	/**
	 * <p>
	 * Returns the JWE compact representation.
	 * </p>
	 * 
	 * @return the JWE compact representation or null if unauthenticated
	 */
	@JsonProperty( value = "token", access = JsonProperty.Access.READ_ONLY )
	@Override
	public String getToken() {
		return this.jwe != null ? this.jwe.toCompact() : null;
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.jwe != null && this.jwe.getPayload().isAuthenticated();
	}

	@Override
	public Optional<SecurityException> getCause() {
		if(this.jwe != null) {
			return this.jwe.getPayload().getCause();
		}
		return Optional.ofNullable(this.cause);
	}
}

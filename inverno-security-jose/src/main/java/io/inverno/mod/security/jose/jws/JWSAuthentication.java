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
package io.inverno.mod.security.jose.jws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.SecurityException;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.TokenAuthentication;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.jwa.NoAlgorithm;
import java.util.Optional;

/**
 * <p>
 * A token authentication that wraps the original authentication in a JWS and uses its compact representation as token value.
 * </p>
 * 
 * <p>
 * The authentication is considered authenticated when the underlying JWS is valid and is not using the {@link NoAlgorithm#NONE} algorithm.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the original authentication type
 */
public class JWSAuthentication<A extends Authentication> implements TokenAuthentication {

	/**
	 * The JWS.
	 */
	private final JWS<A> jws;
	
	/**
	 * The security error resulting from a JOSE processing error.
	 */
	private final Optional<SecurityException> cause;

	/**
	 * <p>
	 * Creates a JWS authentication with the specified JWS.
	 * </p>
	 * 
	 * @param jws a JWS wrapping the original authentication
	 */
	public JWSAuthentication(JWS<A> jws) {
		this.jws = jws;
		this.cause = Optional.empty();
	}
	
	/**
	 * <p>
	 * Creates a denied JWS authentication with the specified security error.
	 * </p>
	 * 
	 * @param cause a security error or null
	 */
	public JWSAuthentication(SecurityException cause) {
		this.jws = null;
		this.cause = Optional.ofNullable(cause);
	}

	/**
	 * <p>
	 * Creates a denied JWS authentication after a JOSE processing error.
	 * </p>
	 * 
	 * @param cause a JOSE processing error or null
	 */
	JWSAuthentication(JOSEProcessingException cause) {
		this.jws = null;
		this.cause = Optional.ofNullable(cause).map(e -> new InvalidCredentialsException("Invalid token", e));
	}
	
	/**
	 * <p>
	 * Returns the JWS.
	 * </p>
	 * 
	 * @return the JWS or null if unauthenticated
	 */
	public JWS<A> getJws() {
		return this.jws;
	}

	/**
	 * <p>
	 * Returns the JWS compact representation.
	 * </p>
	 * 
	 * @return the JWS compact representation or null if unauthenticated
	 */
	@JsonProperty( value = "token", access = JsonProperty.Access.READ_ONLY )
	@Override
	public String getToken() {
		return this.jws != null ? this.jws.toCompact() : null;
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.jws != null && !this.jws.getHeader().getAlgorithm().equals(NoAlgorithm.NONE.getAlgorithm()) && this.jws.getPayload().isAuthenticated();
	}

	@Override
	public Optional<SecurityException> getCause() {
		if(this.jws != null) {
			return this.jws.getPayload().getCause();
		}
		return this.cause;
	}
}

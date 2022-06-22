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
import io.inverno.mod.security.authentication.TokenAuthentication;
import java.util.Optional;

/**
 * <p>
 * A token authentication that wraps the original authentication in a JWS and uses its compact representation as token value.
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
	 * <p>
	 * Creates a JWE authentication with the specified JWS.
	 * </p>
	 * 
	 * @param jws a JWS wrapping the original authentication
	 */
	public JWSAuthentication(JWS<A> jws) {
		this.jws = jws;
	}

	/**
	 * <p>
	 * Returns the JWS.
	 * </p>
	 * 
	 * @return the JWS
	 */
	public JWS<A> getJws() {
		return this.jws;
	}

	/**
	 * <p>
	 * Returns the JWS compact representation.
	 * </p>
	 * 
	 * @return the JWS compact representation
	 */
	@JsonProperty( value = "token", access = JsonProperty.Access.READ_ONLY )
	@Override
	public String getToken() {
		return this.jws.toCompact();
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.jws.getPayload().isAuthenticated();
	}

	@Override
	public Optional<SecurityException> getCause() {
		return this.jws.getPayload().getCause();
	}
}

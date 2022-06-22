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
package io.inverno.mod.security.jose.jwt;

import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.TokenAuthentication;
import io.inverno.mod.security.jose.jwe.JWE;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A token authentication that uses the compact representation of a JWTE as token value.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWT claims set type
 */
public class JWTEAuthentication <A extends JWTClaimsSet> implements TokenAuthentication {

	/**
	 * The JWTE.
	 */
	private final JWE<A> jwt;
	
	/**
	 * <p>
	 * Creates a JWTE authentication with the specified JWT.
	 * </p>
	 * 
	 * @param jwt a JWTE
	 */
	public JWTEAuthentication(JWE<A> jwt) {
		this.jwt = Objects.requireNonNull(jwt);
	}

	/**
	 * <p>
	 * Returns the JWTE.
	 * </p>
	 * 
	 * @return a JWTE
	 */
	public JWE<A> getJwt() {
		return this.jwt;
	}
	
	/**
	 * <p>
	 * Returns the JWT claims set.
	 * </p>
	 * 
	 * @return the JWT claims set
	 */
	public A getJWTClaimsSet() {
		return this.jwt.getPayload();
	}
	
	/**
	 * <p>
	 * Returns the JWTE compact representation.
	 * </p>
	 * 
	 * @return the JWTE compact representation
	 */
	@Override
	public String getToken() {
		return this.jwt.toCompact();
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.jwt.getPayload().isValid();
	}

	@Override
	public Optional<io.inverno.mod.security.SecurityException> getCause() {
		try {
			this.jwt.getPayload().ifInvalidThrow();
			return Optional.empty();
		}
		catch(InvalidJWTException e) {
			return Optional.of(new InvalidCredentialsException(e));
		}
	}
}
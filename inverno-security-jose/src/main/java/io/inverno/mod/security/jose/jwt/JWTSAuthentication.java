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
import io.inverno.mod.security.jose.jws.JWS;
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class JWTSAuthentication <A extends JWTClaimsSet> implements TokenAuthentication {

	private final JWS<A> jwt;
	
	public JWTSAuthentication(JWS<A> jwt) {
		this.jwt = Objects.requireNonNull(jwt);
	}

	public JWS<A> getJwt() {
		return this.jwt;
	}
	
	public A getJWTClaimsSet() {
		return this.jwt.getPayload();
	}
	
	@Override
	public String getToken() {
		return this.jwt.toCompact();
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.jwt.getPayload().isValid();
	}

	@Override
	@SuppressWarnings("exports")
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
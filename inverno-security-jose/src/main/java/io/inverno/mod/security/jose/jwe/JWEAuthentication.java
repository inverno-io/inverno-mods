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
import io.inverno.mod.security.authentication.TokenAuthentication;
import java.util.Optional;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class JWEAuthentication<A extends Authentication> implements TokenAuthentication {

	private final JWE<A> jwe;

	public JWEAuthentication(JWE<A> jwe) {
		this.jwe = jwe;
	}

	public JWE<A> getJws() {
		return this.jwe;
	}

	@JsonProperty( value = "token", access = JsonProperty.Access.READ_ONLY )
	@Override
	public String getToken() {
		return this.jwe.toCompact();
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.jwe.getPayload().isAuthenticated();
	}

	@Override
	public Optional<SecurityException> getCause() {
		return this.jwe.getPayload().getCause();
	}
}

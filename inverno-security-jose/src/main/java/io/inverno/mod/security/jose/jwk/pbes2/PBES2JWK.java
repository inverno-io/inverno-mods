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
package io.inverno.mod.security.jose.jwk.pbes2;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.jwk.SymmetricJWK;

/**
 * <p>
 * Password-based JSON Web Key.
 * </p>
 * 
 * <p>
 * A Password-based JWK is symmetric, the secret key id defined by the password value.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface PBES2JWK extends SymmetricJWK {

	/**
	 * Password-based key type is the same as Octet sequence key type as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.1">RFC7518 Section 6.1</a>.
	 */
	static final String KEY_TYPE = "oct";
	
	/**
	 * <p>
	 * Returns the password encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded password with no padding.
	 */
	@JsonProperty("p")
	String getPassword();
	
	@Override
	PBES2JWK toPublicJWK();

	@Override
	PBES2JWK minify();
	
	@Override
	PBES2JWK trust();
}

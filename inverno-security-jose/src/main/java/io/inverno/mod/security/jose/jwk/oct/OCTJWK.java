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
package io.inverno.mod.security.jose.jwk.oct;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.jwk.SymmetricJWK;

/**
 * <p>
 * Octet JSON Web Key.
 * </p>
 * 
 * <p>
 * An octet JWK is symmetric, the secret key id defined by the key value.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface OCTJWK extends SymmetricJWK {
	
	/**
	 * Octet sequence key type (used to represent symmetric keys) as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.1">RFC7518 Section 6.1</a>.
	 */
	String KEY_TYPE = "oct";
	
	/**
	 * <p>
	 * Returns the key value encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded key value with no padding.
	 */
	@JsonProperty("k")
	String getKeyValue();

	@Override
	OCTJWK toPublicJWK();

	@Override
	OCTJWK minify();
	
	@Override
	OCTJWK trust();
}

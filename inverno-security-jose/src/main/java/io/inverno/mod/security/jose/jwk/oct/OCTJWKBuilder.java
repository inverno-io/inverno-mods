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

import io.inverno.mod.security.jose.jwk.JWKBuilder;

/**
 * <p>
 * An Octet JSON Web Key builder.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the Octet JWK type
 * @param <B> the Octet JWK builder type
 */
public interface OCTJWKBuilder<A extends OCTJWK, B extends OCTJWKBuilder<A, B>> extends JWKBuilder<A, B> {

	/**
	 * <p>
	 * Specifies the key value encoded as Base64URL.
	 * </p>
	 * 
	 * @param k the Base64URL encoded key value with no padding
	 * 
	 * @return this builder
	 */
	B keyValue(String k);
}

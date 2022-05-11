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

import io.inverno.mod.security.jose.jwk.JWKGenerator;
import java.security.SecureRandom;

/**
 * <p>
 * An Octet JSON Web Key generator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the Octet JWK type
 * @param <B> the Octet JWK generator type
 */
public interface OCTJWKGenerator<A extends OCTJWK, B extends OCTJWKGenerator<A, B>> extends JWKGenerator<A, B> {

	/**
	 * <p>
	 * Specifies the size of the key to generate in bytes.
	 * </p>
	 * 
	 * @param keySize the size of the key in bytes
	 * 
	 * @return this generator
	 */
	B keySize(int keySize);

	/**
	 * <p>
	 * Specifies the secure random to use to generate the key.
	 * </p>
	 * 
	 * <p>
	 * If not specified a default secure random will be used.
	 * </p>
	 * 
	 * @param secureRandom a secure random
	 * 
	 * @return this builder
	 */
	B secureRandom(SecureRandom secureRandom);
}

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
 */package io.inverno.mod.security.jose.jwk.pbes2;
import io.inverno.mod.security.jose.jwk.JWKGenerator;
import org.apache.commons.text.RandomStringGenerator;

/**
 * <p>
 * Password-based JSON Web Key generator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the Password-based JWK type
 * @param <B> the Password-based JWK generator type
 */
public interface PBES2JWKGenerator<A extends PBES2JWK, B extends PBES2JWKGenerator<A, B>> extends JWKGenerator<A, B> {

	/**
	 * <p>
	 * Specifies the length of the password to generate in bytes.
	 * </p>
	 * 
	 * @param length the length of the password in bytes
	 * 
	 * @return this builder
	 */
	B length(int length);
	
	/**
	 * <p>
	 * Specifies the random string generator to use to generate the password.
	 * </p>
	 * 
	 * <p>
	 * If not specified a default random string generator will be used.
	 * </p>
	 * 
	 * @param randomStringGenerator a random string generator
	 * 
	 * @return this builder
	 */
	B randomStringGenerator(RandomStringGenerator randomStringGenerator);
}

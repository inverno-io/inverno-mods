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
package io.inverno.mod.security.jose.jwk.rsa;

import io.inverno.mod.security.jose.jwk.X509JWKGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * <p>
 * RSA JSON Web Key generator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the RSA JWK type
 * @param <B> the RSA JWK generator type
 */
public interface RSAJWKGenerator<A extends RSAJWK, B extends RSAJWKGenerator<A,B>> extends X509JWKGenerator<RSAPublicKey, RSAPrivateKey, A, B> {

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
}

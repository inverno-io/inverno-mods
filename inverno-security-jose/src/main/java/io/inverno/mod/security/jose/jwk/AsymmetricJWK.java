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
package io.inverno.mod.security.jose.jwk;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

/**
 * <p>
 * An asymmetric JSON Web Key based on asymmetric public and private keys.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 */
public interface AsymmetricJWK<A extends PublicKey, B extends PrivateKey> extends JWK {
	
	/**
	 * <p>
	 * Converts the JWK to its corresponding public key.
	 * </p>
	 * 
	 * @return a public key
	 * 
	 * @throws JWKProcessingException if there was an error converting the JWK to a public key
	 */
	A toPublicKey() throws JWKProcessingException;
	
	/**
	 * <p>
	 * Converts the JWK to its corresponding private key.
	 * </p>
	 * 
	 * @return an optional containing the private key or an empty optional if the key does not contain private information
	 * 
	 * @throws JWKProcessingException if there was an error converting the JWK to a private key
	 */
	Optional<B> toPrivateKey() throws JWKProcessingException;
	
	@Override
	AsymmetricJWK<A, B> toPublicJWK();
	
	@Override
	AsymmetricJWK<A, B> trust();
}

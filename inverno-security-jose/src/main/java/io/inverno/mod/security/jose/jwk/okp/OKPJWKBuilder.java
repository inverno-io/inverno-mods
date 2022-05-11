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
package io.inverno.mod.security.jose.jwk.okp;

import io.inverno.mod.security.jose.jwk.X509JWKBuilder;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * <p>
 * Octet Key Pair JSON Web Key builder.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the Octet Key Pair JWK type
 * @param <D> the Octet Key Pair JWK builder type
 */
public interface OKPJWKBuilder<A extends PublicKey, B extends PrivateKey, C extends OKPJWK<A, B>, D extends OKPJWKBuilder<A, B, C, D>> extends X509JWKBuilder<A, B, C, D> {

	/**
	 * <p>
	 * Specifies the Elliptic curve JWA name.
	 * </p>
	 * 
	 * @param crv the JWA Elliptic curve name
	 * 
	 * @return this builder
	 */
	D curve(String crv);
	
	/**
	 * <p>
	 * Specifies the public key encoded as Base64URL.
	 * </p>
	 * 
	 * @param x the Base64URL encoded public key with no padding
	 * 
	 * @return this builder
	 */
	D publicKey(String x);
	
	/**
	 * <p>
	 * Specifies the private key encoded as Base64URL.
	 * </p>
	 * 
	 * @param d the Base64URL encoded private key with no padding
	 * 
	 * @return this builder
	 */
	D privateKey(String d);
}

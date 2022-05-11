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
package io.inverno.mod.security.jose.jwk.ec;

import io.inverno.mod.security.jose.jwk.X509JWKBuilder;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

/**
 * <p>
 * An Elliptic curve JSON Web Key builder.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the Elliptic curve JWK type
 * @param <B> the Elliptic curve JWK builder type
 */
public interface ECJWKBuilder<A extends ECJWK, B extends ECJWKBuilder<A, B>> extends X509JWKBuilder<ECPublicKey, ECPrivateKey, A, B> {

	/**
	 * <p>
	 * Specifies the Elliptic curve JWA name.
	 * </p>
	 * 
	 * @param crv the JWA Elliptic curve name
	 * 
	 * @return this builder
	 */
	B curve(String crv);
	
	/**
	 * <p>
	 * Specifies the X coordinate encoded as Base64URL.
	 * </p>
	 * 
	 * @param x the Base64URL encoded X coordinate with no padding
	 * 
	 * @return this builder
	 */
	B xCoordinate(String x);
	
	/**
	 * <p>
	 * Specifies the Y coordinate encoded as Base64URL.
	 * </p>
	 * 
	 * @param y the Base64URL encoded Y coordinate with no padding
	 * 
	 * @return this builder
	 */
	B yCoordinate(String y);
	
	/**
	 * <p>
	 * Specifies the ECC private key encoded as Base64URL.
	 * </p>
	 * 
	 * @param d the Base64URL encoded ECC private key with no padding
	 * 
	 * @return this builder
	 */
	B eccPrivateKey(String d);
}

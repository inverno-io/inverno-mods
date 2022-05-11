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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.jwk.X509JWK;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

/**
 * <p>
 * Elliptic curve JSON Web key.
 * </p>
 * 
 * <p>
 * An Elliptic curve JWK is asymetric. The public key is composed of the elliptic curve and the X and Y coordinates. The private key is defined by the ECC private key.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface ECJWK extends X509JWK<ECPublicKey, ECPrivateKey> {

	/**
	 * Elliptic Curve key type as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.1">RFC7518 Section 6.1</a>
	 */
	static final String KEY_TYPE = "EC";
	
	/**
	 * <p>
	 * Returns the JWA Elliptic curve name.
	 * </p>
	 * 
	 * @return the Elliptic curve name
	 */
	@JsonProperty("crv")
	String getCurve();
	
	/**
	 * <p>
	 * Returns the X coordinate encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded X coordinate with no padding.
	 */
	@JsonProperty("x")
	String getXCoordinate();
	
	/**
	 * <p>
	 * Returns the Y coordinate encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded Y coordinate with no padding.
	 */
	@JsonProperty("y")
	String getYCoordinate();
	
	/**
	 * <p>
	 * Returns the ECC private key encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded ECC private key with no padding.
	 */
	@JsonProperty("d")
	String getEccPrivateKey();

	@Override
	ECJWK toPublicJWK();

	@Override
	ECJWK minify();
	
	@Override
	ECJWK trust();
}

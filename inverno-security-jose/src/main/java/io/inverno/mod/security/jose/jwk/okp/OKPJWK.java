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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.jwk.X509JWK;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * <p>
 * Octet Key Pair JSON Web Key.
 * </p>
 *
 * <p>
 * An Octet Key Pair JWK is asymmetric.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 */
public interface OKPJWK<A extends PublicKey, B extends PrivateKey> extends X509JWK<A, B> {
	
	/**
	 * Octet Key Pair key type as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-2">RFC8037 Section 2</a>.
	 */
	static final String KEY_TYPE = "OKP";
	
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
	 * Returns the public key encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded public key with no padding.
	 */
	@JsonProperty("x")
	String getPublicKey();
	
	/**
	 * <p>
	 * Returns the private key encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded private key with no padding.
	 */
	@JsonProperty("d")
	String getPrivateKey();
	
	@Override
	OKPJWK<A, B> toPublicJWK();

	@Override
	OKPJWK<A, B> minify();
	
	@Override
	OKPJWK<A, B> trust();
}

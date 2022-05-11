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

import io.inverno.mod.security.jose.jwk.X509JWKBuilder;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * <p>
 * RSA JSON Web Key builder.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the RSA JWK type
 * @param <B> the RSA JWK builder type
 */
public interface RSAJWKBuilder<A extends RSAJWK, B extends RSAJWKBuilder<A, B>> extends X509JWKBuilder<RSAPublicKey, RSAPrivateKey, A, B> {

	/**
	 * <p>
	 * Specifies the modulus encoded as Base64URL.
	 * </p>
	 * 
	 * @param n the Base64URL encoded modulus with no padding
	 * 
	 * @return this builder
	 */
	B modulus(String n);
	
	/**
	 * <p>
	 * Specifies the public exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @param e the Base64URL encoded public exponent with no padding
	 * 
	 * @return this builder
	 */
	B publicExponent(String e);
	
	/**
	 * <p>
	 * Specifies the private exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @param d the Base64URL encoded private exponent with no padding
	 * 
	 * @return this builder
	 */
	B privateExponent(String d);
	
	/**
	 * <p>
	 * Specifies the first prime factor encoded as Base64URL.
	 * </p>
	 * 
	 * @param p the Base64URL encoded first prime factor with no padding
	 * 
	 * @return this builder
	 */
	B firstPrimeFactor(String p);
	
	/**
	 * <p>
	 * Specifies the second prime factor encoded as Base64URL.
	 * </p>
	 * 
	 * @param q the Base64URL encoded second prime factor with no padding
	 * 
	 * @return this builder
	 */
	B secondPrimeFactor(String q);
	
	/**
	 * <p>
	 * Specifies the first factor exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @param dp the Base64URL encoded first factor exponent with no padding
	 * 
	 * @return this builder
	 */
	B firstFactorExponent(String dp);
	
	/**
	 * <p>
	 * Specifies the second factor exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @param dq the Base64URL encoded second factor exponent with no padding
	 * 
	 * @return this builder
	 */
	B secondFactorExponent(String dq);
	
	/**
	 * <p>
	 * Specifies the first coefficient encoded as Base64URL.
	 * </p>
	 * 
	 * @param qi the Base64URL encoded first coefficient with no padding
	 * 
	 * @return this builder
	 */
	B firstCoefficient(String qi);
	
	/**
	 * <p>
	 * Specifies a prime info to add to the definition of the key.
	 * </p>
	 * 
	 * @param primeFactor the Base64URL encoded other prime info prime factor with no padding
	 * @param exponent the Base64URL encoded other prime info exponent with no padding
	 * @param coefficient the Base64URL encoded other prime info coefficient with no padding
	 * 
	 * @return this builder
	 */
	B otherPrimeInfo(String primeFactor, String exponent, String coefficient);
}

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.jwk.X509JWK;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * <p>
 * RSA JSON Web Key.
 * </p>
 * 
 * <p>
 * An RSA JWK is asymmetric. The public key is composed of the modulus and the public exponent. The private key is defined by the private exponent. Other parameters can be present when the key is a
 * multi prime key.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface RSAJWK extends X509JWK<RSAPublicKey, RSAPrivateKey> {

	/**
	 * RSA key type as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.1">RFC7518 Section 6.1</a>.
	 */
	String KEY_TYPE = "RSA";
	
	/**
	 * <p>
	 * Returns the modulus encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded modulus with no padding.
	 */
	@JsonProperty("n")
	String getModulus();
	
	/**
	 * <p>
	 * Returns the public exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded public exponent with no padding.
	 */
	@JsonProperty("e")
	String getPublicExponent();
	
	/**
	 * <p>
	 * Returns the private exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded private exponent with no padding.
	 */
	@JsonProperty("d")
	String getPrivateExponent();
	
	/**
	 * <p>
	 * Returns the first prime factor encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded first prime factor with no padding.
	 */
	@JsonProperty("p")
	String getFirstPrimeFactor();
	
	/**
	 * <p>
	 * Returns the second prime factor encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded second prime factor with no padding.
	 */
	@JsonProperty("q")
	String getSecondPrimeFactor();
	
	/**
	 * <p>
	 * Returns the first factor exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded first factor exponent with no padding.
	 */
	@JsonProperty("dp")
	String getFirstFactorExponent();
	
	/**
	 * <p>
	 * Returns the second factor exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded second factor exponent with no padding.
	 */
	@JsonProperty("dq")
	String getSecondFactorExponent();
	
	/**
	 * <p>
	 * Returns the first coefficient encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded first coefficient with no padding.
	 */
	@JsonProperty("qi")
	String getFirstCoefficient();
	
	/**
	 * <p>
	 * Returns the list of other primes info.
	 * </p>
	 * 
	 * @return the list of other primes info or null
	 */
	@JsonProperty("oth")
	List<OtherPrimeInfo> getOtherPrimesInfo();
	
	@Override
	RSAJWK toPublicJWK();

	@Override
	RSAJWK minify();
	
	@Override
	RSAJWK trust();
	
	/**
	 * <p>
	 * RSA JSON Web Key other prime info.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	interface OtherPrimeInfo {
		
		/**
		 * <p>
		 * Returns the other prime info prime factor encoded as Base64URL.
		 * </p>
		 * 
		 * @return the Base64URL encoded other prime info prime factor with no padding.
		 */
		@JsonProperty("r")
		String getPrimeFactor();
		
		/**
		 * <p>
		 * Returns the other prime info exponent encoded as Base64URL.
		 * </p>
		 * 
		 * @return the Base64URL encoded other prime info exponent with no padding.
		 */
		@JsonProperty("d")
		String getExponent();
		
		/**
		 * <p>
		 * Returns the other prime info coefficient encoded as Base64URL.
		 * </p>
		 * 
		 * @return the Base64URL encoded other prime info coefficient with no padding.
		 */
		@JsonProperty("t")
		String getCoefficient();
	}
}

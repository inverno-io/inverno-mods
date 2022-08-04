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
package io.inverno.mod.security.jose.jwa;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidParameterSpecException;

/**
 * <p>
 * Elliptic Curves as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a>.
 * </p>
 *
 * <p>Supported curves:</p>
 * 
 * <ul>
 * <li>P-256</li>
 * <li>P-384</li>
 * <li>P-521</li>
 * <li>secp256k1 (deprecated)</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum ECCurve {
	
	/**
	 * P-256 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.2.1.1">RFC7518 Section 6.2.1.1</a>
	 */
	P_256("P-256", "secp256r1", 256),
	/**
	 * P-384 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.2.1.1">RFC7518 Section 6.2.1.1</a>
	 */
	P_384("P-384", "secp384r1", 384),
	/**
	 * P-521 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.2.1.1">RFC7518 Section 6.2.1.1</a>
	 */
	P_521("P-521", "secp521r1", 521),
	/**
	 * secp256k1 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8812#section-4.4">RFC8812 Section 4.4</a>
	 * 
	 * @deprecated secp256k1 has been disabled in the JDK (>=15), it can be activated by setting jdk.sunec.disableNative property to false ({@code -Djdk.sunec.disableNative=false})
	 */
	@Deprecated
	SECP256K1("secp256k1", "secp256k1", 256);
	
	/**
	 * P-256 Elliptic curve parameters.
	 */
	private static ECParameterSpec P_256_EC_PARAMETER_SPEC;
	/**
	 * P-384 Elliptic curve parameters.
	 */
	private static ECParameterSpec P_384_EC_PARAMETER_SPEC;
	/**
	 * P-521 Elliptic curve parameters.
	 */
	private static ECParameterSpec P_521_EC_PARAMETER_SPEC;
	/**
	 * secp256k1 Elliptic curve parameters.
	 */
	private static ECParameterSpec SECP256K1_EC_PARAMETER_SPEC;

	/**
	 * The JWA registered curve name.
	 */
	private final String crv;
	/**
	 * The JCA curve name.
	 */
	private final String jcaName;
	/**
	 * The field size of the curve in bits.
	 */
	private final int fieldSize;
	/**
	 * The key length in bytes.
	 */
	private final int keyLength;
	/**
	 * The signature length in bytes.
	 */
	private final int signatureLength;
	
	/**
	 * <p>
	 * Creates an Elliptic Curve.
	 * </p>
	 * 
	 * @param crv the JWA registered name
	 * @param jcaName the JCA name
	 * @param fieldSize the field size in bits
	 */
	private ECCurve(String crv, String jcaName, int fieldSize) {
		this.crv = crv;
		this.jcaName = jcaName;
		this.fieldSize = fieldSize;
		this.keyLength = (this.fieldSize + 7) / 8;
		this.signatureLength = 2 * this.keyLength;
	}
	
	/**
	 * <p>
	 * Returns the JWA registered curve name.
	 * </p>
	 * 
	 * @return the registered name of the curve
	 */
	public String getCurve() {
		return crv;
	}

	/**
	 * <p>
	 * Returns the JCA curve name.
	 * </p>
	 * 
	 * @return the JCA curve name
	 */
	public String getJCAName() {
		return jcaName;
	}

	/**
	 * <p>
	 * Returns the field size in bits.
	 * </p>
	 * 
	 * @return the field size in bits
	 */
	public int getFieldSize() {
		return fieldSize;
	}
	
	/**
	 * <p>
	 * Returns the key length in bytes.
	 * </p>
	 * 
	 * @return the key length in bytes
	 */
	public int getKeyLength() {
		return this.keyLength;
	}
	
	/**
	 * <p>
	 * Returns the signature length in bytes.
	 * </p>
	 * 
	 * @return the signature length in bytes
	 */
	public int getSignatureLength() {
		return this.signatureLength;
	}
	
	/**
	 * <p>
	 * Returns the Elliptic curve parameters.
	 * </p>
	 *
	 * @return the Elliptic curve parameters
	 *
	 * @throws JWAProcessingException if the curve is not fully supported
	 * @throws IllegalStateException  if there was an error creating the parameters
	 */
	public ECParameterSpec getParameterSpec() throws JWAProcessingException, IllegalStateException {
		try {
			switch(this.crv) {
				case "P-256": {
					if(P_256_EC_PARAMETER_SPEC == null) {
						AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
						parameters.init(new ECGenParameterSpec("secp256r1"));
						P_256_EC_PARAMETER_SPEC = parameters.getParameterSpec(ECParameterSpec.class);
					}
					return P_256_EC_PARAMETER_SPEC;
				}
				case "P-384": {
					if(P_384_EC_PARAMETER_SPEC == null) {
						AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
						parameters.init(new ECGenParameterSpec("secp384r1"));
						P_384_EC_PARAMETER_SPEC = parameters.getParameterSpec(ECParameterSpec.class);
					}
					return P_384_EC_PARAMETER_SPEC;
				}
				case "P-521": {
					if(P_521_EC_PARAMETER_SPEC == null) {
						AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
						parameters.init(new ECGenParameterSpec("secp521r1"));
						P_521_EC_PARAMETER_SPEC = parameters.getParameterSpec(ECParameterSpec.class);
					}
					return P_521_EC_PARAMETER_SPEC;
				}
				case "secp256k1": {
					if(SECP256K1_EC_PARAMETER_SPEC == null) {
						AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
						parameters.init(new ECGenParameterSpec("secp256k1"));
						P_521_EC_PARAMETER_SPEC = parameters.getParameterSpec(ECParameterSpec.class);
					}
					return SECP256K1_EC_PARAMETER_SPEC;
				}
				default: 
					throw new JWAProcessingException("Unsupported curve: " + crv);
			}
		}
		catch (NoSuchAlgorithmException | InvalidParameterSpecException e) {
			throw new IllegalStateException("Error while creating EC key", e);
		}
	}
	
	/**
	 * <p>
	 * Determines whether the specified point is on the curve.
	 * </p>
	 * 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * 
	 * @return true if the point is on the curve, false otherwise
	 */
	public boolean isOnCurve(BigInteger x, BigInteger y) {
		EllipticCurve curve = this.getParameterSpec().getCurve();
		
		BigInteger a = curve.getA();
		BigInteger b = curve.getB();
		BigInteger p = ((ECFieldFp) curve.getField()).getP();
		BigInteger leftSide = (y.pow(2)).mod(p);
		BigInteger rightSide = (x.pow(3).add(a.multiply(x)).add(b)).mod(p);
		
		return leftSide.equals(rightSide);
	}
	
	/**
	 * <p>
	 * Returns the curve corresponding to the specified JWA registered curve name.
	 * </p>
	 * 
	 * @param crv a JWA registered curve name
	 * 
	 * @return an Elliptic curve
	 * 
	 * @throws IllegalArgumentException if the specified curve is not supported
	 */
	public static ECCurve fromCurve(String crv) throws IllegalArgumentException {
		switch(crv) {
			case "P-256":
				return ECCurve.P_256;
			case "P-384":
				return ECCurve.P_384;
			case "P-521":
				return ECCurve.P_521;
			case "secp256k1":
				return ECCurve.SECP256K1;
			default:
				throw new IllegalArgumentException("Unknown curve " + crv);
		}
	}
}

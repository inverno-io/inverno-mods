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

/**
 * <p>
 * Octet Key Pair Elliptic curves as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037">RFC8037</a>
 * </p>
 * 
 * <p>Supported curves:</p>
 * 
 * <ul>
 * <li>Ed25519</li>
 * <li>Ed448</li>
 * <li>X25519</li>
 * <li>X448</li>
 * </ul>
 * 
 * <p>
 * These curves are used in conjuntion with algorithms defined by {@link EdECAlgorithm} and {@link XECAlgorithm}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum OKPCurve {
	
	/**
	 * Ed25519 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.1">RFC8037 Section 3.1</a>
	 */
	ED25519("Ed25519", "Ed25519", 32),
	/**
	 * Ed448 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.1">RFC8037 Section 3.1</a>
	 */
	ED448("Ed448", "Ed448", 57),
	/**
	 * X25519 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.2">RFC8037 Section 3.2</a>
	 */
	X25519("X25519", "X25519", 32),
	/**
	 * X448 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.2">RFC8037 Section 3.2</a>
	 */
	X448("X448", "X448", 56);
	
	/**
	 * The JWA registered curve name.
	 */
	private final String crv;
	/**
	 * The JCA curve name.
	 */
	private final String jcaName;
	/**
	 * The key length in bytes.
	 */
	private final int keyLength;
	
	/**
	 * <p>
	 * Creates an Octet Key Pair curve.
	 * </p>
	 * 
	 * @param crv the JWA registered name
	 * @param jcaName the JCA name
	 * @param keyLength the key length in bytes
	 */
	private OKPCurve(String crv, String jcaName, int keyLength) {
		this.crv = crv;
		this.jcaName = jcaName;
		this.keyLength = keyLength;
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
	 * Returns the key length in bytes.
	 * </p>
	 * 
	 * @return the key length in bytes
	 */
	public int getKeyLength() {
		return keyLength;
	}
	
	/**
	 * <p>
	 * Returns the curve corresponding to the specified JWA registered curve name.
	 * </p>
	 * 
	 * @param crv a JWA registered curve name
	 * 
	 * @return an OKP curve
	 * 
	 * @throws IllegalArgumentException if the specified curve is not supported
	 */
	public static OKPCurve fromCurve(String crv) {
		switch(crv) {
			case "Ed25519":
				return OKPCurve.ED25519;
			case "Ed448":
				return OKPCurve.ED448;
			case "X25519":
				return OKPCurve.X25519;
			case "X448":
				return OKPCurve.X448;
			default:
				throw new IllegalArgumentException("Unknown curve " + crv);
		}
	}
}

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
package io.inverno.mod.security.jose.internal.jwa;

import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Octet Key Pair Elliptic Curve Diffie-Hellman Ephemeral Static with AES key wrap key manager implementation.
 * </p>
 * 
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH-ES+A128KW with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A192KW with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A256KW with extended elliptic curve X25519 or X448</li>
 * </ul>
 * 
 * <p>
 * It processes the following parameters:
 * </p>
 * 
 * <ul>
 * <li>{@code epk}: ephemeral public key</li>
 * <li>{@code apu}: Agreement PartyUInfo</li>
 * <li>{@code apv}: Agreement PartyVInfo</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class OKP_ECDH_ES_AESKWKeyManager extends AbstractECDH_ES_AESKWKeyManager<XECPublicKey, XECPrivateKey, XECJWK, XECAlgorithm> {
	
	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<XECAlgorithm> SUPPORTED_ALGORITHMS = Set.of(XECAlgorithm.ECDH_ES_A128KW, XECAlgorithm.ECDH_ES_A192KW, XECAlgorithm.ECDH_ES_A256KW);
	
	/**
	 * The set of elliptic curves supported by the key manager.
	 */
	public static final Set<String> SUPPORTED_CURVES = Set.of(OKPCurve.X25519.getCurve(), OKPCurve.X448.getCurve());
	
	private OKP_ECDH_ESKeyManager ecdh_es_keyManager;
	
	private OCTAlgorithm keyWrappingAlgorithm;
	
	/**
	 * <p>
	 * Creates an OKP ECDH-ES AES KW key manager.
	 * </p>
	 *
	 * @param jwk       an extended elliptic curve JWK
	 * @param algorithm an extended elliptic curve JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public OKP_ECDH_ES_AESKWKeyManager(XECJWK jwk, XECAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		if(!SUPPORTED_CURVES.contains(jwk.getCurve())) {
			throw new JWAProcessingException("Unsupported curve: " + jwk.getCurve());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an OKP ECDH-ES AES KW key manager.
	 * </p>
	 *
	 * @param jwk an extended elliptic curve JWK
	 */
	protected OKP_ECDH_ES_AESKWKeyManager(XECJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
		this.ecdh_es_keyManager = new OKP_ECDH_ESKeyManager(this.jwk, XECAlgorithm.ECDH_ES);
		this.keyWrappingAlgorithm = OCTAlgorithm.fromAlgorithm(this.algorithm.getKeyWrappingAlgorithm());
	}
	
	@Override
	protected OCTAlgorithm getCEKWrappingAlgorithm() {
		return this.keyWrappingAlgorithm;
	}

	@Override
	protected DirectJWAKeyManager.DirectCEK deriveCEKWrappingKey(XECAlgorithm algorithm, Map<String, Object> parameters) {
		return this.ecdh_es_keyManager.deriveCEK(this.algorithm.getKeyWrappingAlgorithm(), parameters);
	}
}

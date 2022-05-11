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
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Elliptic Curve Diffie-Hellman Ephemeral Static with AES key wrap key manager implementation.
 * </p>
 * 
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH-ES+A128KW with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A192KW with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A256KW with elliptic curve P-256, P-384 or P-521</li>
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
public class ECDH_ES_AESKWKeyManager extends AbstractECDH_ES_AESKWKeyManager<ECPublicKey, ECPrivateKey, ECJWK, ECAlgorithm> {
	
	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<ECAlgorithm> SUPPORTED_ALGORITHMS = Set.of(ECAlgorithm.ECDH_ES_A128KW, ECAlgorithm.ECDH_ES_A192KW, ECAlgorithm.ECDH_ES_A256KW);
	
	private ECDH_ESKeyManager ecdh_es_keyManager;
	
	private OCTAlgorithm keyWrappingAlgorithm;
	
	/**
	 * <p>
	 * Creates an ECDH-ES AES KW key manager.
	 * </p>
	 *
	 * @param jwk       an elliptic curve JWK
	 * @param algorithm an elliptic curve JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public ECDH_ES_AESKWKeyManager(ECJWK jwk, ECAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an ECDH-ES AES KW key manager.
	 * </p>
	 *
	 * @param jwk an elliptic curve JWK
	 */
	protected ECDH_ES_AESKWKeyManager(ECJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
		this.ecdh_es_keyManager = new ECDH_ESKeyManager(this.jwk, ECAlgorithm.ECDH_ES);
		this.keyWrappingAlgorithm = OCTAlgorithm.fromAlgorithm(this.algorithm.getKeyWrappingAlgorithm());
	}
	
	@Override
	protected OCTAlgorithm getCEKWrappingAlgorithm() {
		return this.keyWrappingAlgorithm;
	}

	@Override
	protected DirectJWAKeyManager.DirectCEK deriveCEKWrappingKey(ECAlgorithm algorithm, Map<String, Object> parameters) {
		return this.ecdh_es_keyManager.deriveCEK(this.algorithm.getKeyWrappingAlgorithm(), parameters);
	}
}

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
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwa.WrappingJWAKeyManager;
import io.inverno.mod.security.jose.jwk.AsymmetricJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Base Elliptic Curve Diffie-Hellman Ephemeral Static with AES key wrap key manager implementation.
 * </p>
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
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the asymmetric JWK type
 * @param <D> the JWA algorithm type
 */
public abstract class AbstractECDH_ES_AESKWKeyManager<A extends PublicKey, B extends PrivateKey, C extends AsymmetricJWK<A, B>, D extends JWAAlgorithm<C>> extends AbstractWrappingJWAKeyManager<C, D> {

	/**
	 * The set of parameters processed by the key manager.
	 */
	public static final Set<String> PROCESSED_PARAMETERS = Set.of("epk", "apu", "apv");
	
	/**
	 * <p>
	 * Creates an ECDH-ES AES wrapping key manager.
	 * </p>
	 * 
	 * @param jwk       a JWK
	 * @param algorithm a JWA algorithm
	 * 
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AbstractECDH_ES_AESKWKeyManager(C jwk, D algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
	}

	/**
	 * <p>
	 * Creates an ECDH-ES AES wrapping key manager.
	 * </p>
	 * 
	 * @param jwk a JWK
	 */
	public AbstractECDH_ES_AESKWKeyManager(C jwk) {
		super(jwk);
	}
	
	/**
	 * <p>
	 * Returns the key wrapping algorithm to use to wrap the CEK.
	 * </p>
	 * 
	 * @return a key wrapping JWA algorithm
	 */
	protected abstract OCTAlgorithm getCEKWrappingAlgorithm();
	
	/**
	 * <p>
	 * Derives the key to use to wrap the CEK.
	 * </p>
	 * 
	 * @param algorithm  the direct key manager algorithm
	 * @param parameters the parameters map
	 * 
	 * @return the derived key to use to wrap the CEK
	 */
	protected abstract DirectJWAKeyManager.DirectCEK deriveCEKWrappingKey(D algorithm, Map<String, Object> parameters);
	
	@Override
	public Set<String> getProcessedParameters() {
		return PROCESSED_PARAMETERS;
	}
	
	@Override
	protected WrappedCEK doWrapCEK(OCTJWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException {
		// 1. Derive shared key
		DirectJWAKeyManager.DirectCEK derivedCEKWrappingKey = this.deriveCEKWrappingKey(this.algorithm, parameters);
		
		// 2. Wrap key
		AESKWKeyManager aeskm = new AESKWKeyManager(derivedCEKWrappingKey.getEncryptionKey(), this.getCEKWrappingAlgorithm());
		WrappingJWAKeyManager.WrappedCEK wrappedCEK = aeskm.doWrapCEK(cek, parameters, secureRandom);
		
		Map<String, Object> mergedCustomParameters = new HashMap<>();
		if(derivedCEKWrappingKey.getMoreHeaderParameters() != null) {
			mergedCustomParameters.putAll(derivedCEKWrappingKey.getMoreHeaderParameters());
		}
		if(wrappedCEK.getMoreHeaderParameters() != null) {
			mergedCustomParameters.putAll(wrappedCEK.getMoreHeaderParameters());
		}
		
		return new GenericWrappedCEK(wrappedCEK.getWrappedKey(), mergedCustomParameters);
	}

	@Override
	protected OCTJWK doUnwrapCEK(byte[] encrypted_key, OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException {
		// 1. Derive shared key
		DirectJWAKeyManager.DirectCEK derivedCEKWrappingKey = this.deriveCEKWrappingKey(this.algorithm, parameters);
		
		// This is done to replace EPK as Map<String, Object> to the corresponding JWK type in order to get equals() to work
		if(parameters != null && derivedCEKWrappingKey.getMoreHeaderParameters() != null) {
			parameters.putAll(derivedCEKWrappingKey.getMoreHeaderParameters());
		}
		
		// 2. Unwrap key
		AESKWKeyManager aeskm = new AESKWKeyManager(derivedCEKWrappingKey.getEncryptionKey(), this.getCEKWrappingAlgorithm());
		return aeskm.doUnwrapCEK(encrypted_key, octEnc, parameters);
	}
}

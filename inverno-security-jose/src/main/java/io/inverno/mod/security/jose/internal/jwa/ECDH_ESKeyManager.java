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

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWK;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Elliptic Curve Diffie-Hellman Ephemeral Static key manager implementation.
 * </p>
 * 
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH-ES with elliptic curve P-256, P-384 or P-521</li>
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
public class ECDH_ESKeyManager extends AbstractECDH_ESKeyManager<ECPublicKey, ECPrivateKey, ECJWK, ECAlgorithm> {

	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<ECAlgorithm> SUPPORTED_ALGORITHMS = Set.of(ECAlgorithm.ECDH_ES);
	
	private ECCurve curve;
	
	/**
	 * <p>
	 * Creates an ECDH-ES key manager.
	 * </p>
	 *
	 * @param jwk       an elliptic curve JWK
	 * @param algorithm an elliptic curve JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public ECDH_ESKeyManager(ECJWK jwk, ECAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}

	/**
	 * <p>
	 * Creates an ECDH-ES key manager.
	 * </p>
	 *
	 * @param jwk an elliptic curve JWK
	 */
	public ECDH_ESKeyManager(ECJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
		this.curve = ECCurve.fromCurve(this.jwk.getCurve());
	}

	@Override
	protected String getKeyAgreementAlgorithm() {
		return "ECDH";
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected ECJWK getEPK(Map<String, Object> parameters) throws JWAProcessingException {
		Object epk_obj = parameters != null ? parameters.get("epk") : null;
		if(epk_obj != null) {
			if(epk_obj instanceof ECJWK) {
				// a private epk can be returned here, this is useful for tests
				// TODO determine whether this is a security threat
				return (ECJWK)epk_obj;
			}
			else if(epk_obj instanceof Map) {
				Map<String, Object> epk = (Map<String, Object>)epk_obj;
				String kty = (String)epk.get("kty");
				String crv = (String)epk.get("crv");
				String x = (String)epk.get("x");
				String y = (String)epk.get("y");

				if(!ECJWK.KEY_TYPE.equals(kty)) {
					throw new JWAProcessingException("Invalid ephemeral key type: " + kty);
				}

				if(crv == null) {
					throw new JWAProcessingException("Missing curve in ephemeral key");
				}
				if(!curve.getCurve().equals(crv)) {
					throw new JWAProcessingException("Curve " + crv + " in ephemeral key is not consistent with expected curve " + this.curve.getCurve());
				}

				if(StringUtils.isBlank(x)) {
					throw new JWAProcessingException("X is blank in ephemeral key");
				}
				if(StringUtils.isBlank(y)) {
					throw new JWAProcessingException("Y is blank in ephemeral key");
				}
				return new GenericECJWK(this.curve, x, y);
			}
			else {
				throw new JWAProcessingException("Invalid EPK type: " + epk_obj.getClass());
			}
		}
		else {
			// Generate an ephemeral EC key
			try {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
				keyPairGenerator.initialize(new ECGenParameterSpec(this.curve.getJCAName()));
				KeyPair keyPair = keyPairGenerator.generateKeyPair();

				ECPublicKey publicKey = (ECPublicKey)keyPair.getPublic();
				ECPrivateKey privateKey = (ECPrivateKey)keyPair.getPrivate();

				return new GenericECJWK(
					this.curve,
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(publicKey.getW().getAffineX(), this.curve.getKeyLength())), 
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(publicKey.getW().getAffineY(), this.curve.getKeyLength())), 
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(privateKey.getS(), this.curve.getKeyLength()))
				);
			} 
			catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
				throw new JWAProcessingException("Error generating ephemeral key", e);
			}
		}
	}
}

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
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWK;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwk.okp.OKPJWK;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Octet Key Pair Elliptic Curve Diffie-Hellman Ephemeral Static key manager implementation.
 * </p>
 * 
 * <p>
 * It supports the following key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH-ES with extended elliptic curve X25519 or X448</li>
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
public class OKP_ECDH_ESKeyManager extends AbstractECDH_ESKeyManager<XECPublicKey, XECPrivateKey, XECJWK, XECAlgorithm> {

	/**
	 * The set of algorithms supported by the key manager.
	 */
	public static final Set<XECAlgorithm> SUPPORTED_ALGORITHMS = Set.of(XECAlgorithm.ECDH_ES);
	
	/**
	 * The set of curves supported by the key manager.
	 */
	public static final Set<String> SUPPORTED_CURVES = Set.of(OKPCurve.X25519.getCurve(), OKPCurve.X448.getCurve());
	
	private OKPCurve curve;
	
	/**
	 * <p>
	 * Creates an OKP ECDH-ES key manager.
	 * </p>
	 *
	 * @param jwk       an extended elliptic curve JWK
	 * @param algorithm an extended elliptic curve JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public OKP_ECDH_ESKeyManager(XECJWK jwk, XECAlgorithm algorithm) throws JWAProcessingException {
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
	 * Creates an OKP ECDH-ES key manager.
	 * </p>
	 *
	 * @param jwk an extended elliptic curve JWK
	 */
	public OKP_ECDH_ESKeyManager(XECJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
		this.curve = OKPCurve.fromCurve(this.jwk.getCurve());
	}

	@Override
	protected String getKeyAgreementAlgorithm() {
		return this.curve.getJCAName();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected XECJWK getEPK(Map<String, Object> parameters) throws JWAProcessingException {
		Object epk_obj = parameters != null ? parameters.get("epk") : null;
		if(epk_obj != null) {
			if(epk_obj instanceof XECJWK) {
				// a private epk can be returned here, this is useful for tests
				// TODO determine whether this is a security threat
				return (XECJWK)epk_obj;
			}
			else if(epk_obj instanceof Map) {
				Map<String, Object> epk = (Map<String, Object>)epk_obj;
				String kty = (String)epk.get("kty");
				String crv = (String)epk.get("crv");
				String x = (String)epk.get("x");

				if(!OKPJWK.KEY_TYPE.equals(kty)) {
					throw new JWAProcessingException("Invalid ephemeral key type: " + kty);
				}

				if(crv == null) {
					throw new JWAProcessingException("Missing curve in ephemeral key");
				}
				if(!this.curve.getCurve().equals(crv)) {
					throw new JWAProcessingException("Curve " + crv + " in ephemeral key is not consistent with expected curve " + this.curve.getCurve());
				}

				if(StringUtils.isBlank(x)) {
					throw new JWAProcessingException("X is blank in ephemeral key");
				}

				return new GenericXECJWK(this.curve, x);
			}
			else {
				throw new JWAProcessingException("Invalid EPK type: " + epk_obj.getClass());
			}
		}
		else {
			// Generate an ephemeral EC key
			try {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(this.curve.getJCAName());
				KeyPair keyPair = keyPairGenerator.generateKeyPair();
				
				XECPublicKey publicKey = (XECPublicKey)keyPair.getPublic();
				XECPrivateKey privateKey = (XECPrivateKey)keyPair.getPrivate();
				
				byte[] xBytes = new byte[this.curve.getKeyLength()];
				byte[] encodedKeyBytes = publicKey.getEncoded();
				System.arraycopy(encodedKeyBytes, encodedKeyBytes.length - xBytes.length, xBytes, 0, xBytes.length);
				
				return new GenericXECJWK(
					this.curve, 
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(xBytes),
					privateKey.getScalar().map(JOSEUtils.BASE64_NOPAD_URL_ENCODER::encodeToString).orElse(null)
				);
			} 
			catch (NoSuchAlgorithmException e) {
				throw new JWAProcessingException("Error generating ephemeral key", e);
			}
		}
	}
}

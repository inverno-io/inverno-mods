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
package io.inverno.mod.security.jose.internal.jwk.okp;

import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPrivateKeySpec;
import java.security.spec.XECPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Generic Extended Elliptic Curve JSON Web Key implementation.
 * </p>
 *
 * <p>
 * It supports the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH-ES with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A128KW with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A192KW with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A256KW with extended elliptic curve X25519 or X448</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericXECJWK extends AbstractOKPJWK<XECPublicKey, XECPrivateKey> implements XECJWK {

	/**
	 * The set of curves supported by the JWK.
	 */
	public static final Set<OKPCurve> SUPPORTED_CURVES = Set.of(OKPCurve.X25519, OKPCurve.X448);
	
	private XECAlgorithm xecAlg;
	
	private Map<XECAlgorithm, JWAKeyManager> keyManagers;

	/**
	 * <p>
	 * Creates an untrusted public generic XEC JWK with the specified curve and public key value.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the public key value encoded as Base64URL without padding
	 */
	public GenericXECJWK(OKPCurve curve, String x) {
		super(curve, x);
	}

	/**
	 * <p>
	 * Creates a public generic XEC JWK with the specified curve, public key value and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param curve       an elliptic curve
	 * @param x           the public key encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public GenericXECJWK(OKPCurve curve, String x, X509Certificate certificate) {
		super(curve, x, certificate);
	}

	/**
	 * <p>
	 * Creates an untrusted private generic XEC JWK with the specified curve, public key value and private key value.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the public key value encoded as Base64URL without padding
	 * @param d     the private key value encoded as Base64URL without padding
	 */
	public GenericXECJWK(OKPCurve curve, String x, String d) {
		super(curve, x, d);
	}

	/**
	 * <p>
	 * Creates a private generic XEC JWK with the specified curve, public key value, private key value and private key.
	 * </p>
	 * 
	 * @param curve   an elliptic curve
	 * @param x       the public key value encoded as Base64URL without padding
	 * @param d       the private key value encoded as Base64URL without padding
	 * @param key     a private key
	 * @param trusted true to create a trusted JWK, false otherwise
	 */
	public GenericXECJWK(OKPCurve curve, String x, String d, XECPrivateKey key, boolean trusted) {
		super(curve, x, d, key, trusted);
	}

	/**
	 * <p>
	 * Creates a public generic XEC JWK with the specified curve, public key value, private key value and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param curve       an elliptic curve
	 * @param x           the public key value encoded as Base64URL without padding
	 * @param d           the private key value encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public GenericXECJWK(OKPCurve curve, String x, String d, X509Certificate certificate) {
		super(curve, x, d, certificate);
	}

	/**
	 * <p>
	 * Creates a private generic XEC JWK with the specified curve, public coordinates, private key value, OKP private key and certificate.
	 * </p>
	 *
	 * @param curve       an elliptic curve
	 * @param x           the public key value encoded as Base64URL without padding
	 * @param d           the private key value encoded as Base64URL without padding
	 * @param key         a private key
	 * @param certificate an X.509 certificate
	 * @param trusted     true to create a trusted JWK, false otherwise
	 */
	public GenericXECJWK(OKPCurve curve, String x, String d, XECPrivateKey key, X509Certificate certificate, boolean trusted) {
		super(curve, x, d, key, certificate, trusted);
	}
	
	/**
	 * <p>
	 * Sets the extended Elliptic Curve JWA algorithm.
	 * </p>
	 * 
	 * @param xecAlg an XEC algorithm
	 */
	public void setAlgorithm(XECAlgorithm xecAlg) {
		super.setAlgorithm(xecAlg != null ? xecAlg.getAlgorithm() : null);
		this.xecAlg = xecAlg;
	}

	@Override
	public void setAlgorithm(String alg) {
		this.xecAlg = alg != null ? XECAlgorithm.fromAlgorithm(alg) : null;
		super.setAlgorithm(alg);
	}
	
	@Override
	public XECJWK trust() {
		this.trusted = true;
		return this;
	}
	
	@Override
	public XECPublicKey toPublicKey() throws JWKProcessingException {
		if(this.publicKey == null) {
			if(this.certificate == null) {
				try {
					byte[] encodedPoint = Base64.getUrlDecoder().decode(this.x);
					if(this.curve.equals(OKPCurve.X25519)) {
						encodedPoint[encodedPoint.length - 1] &= (byte) 0x7F;
					}
					reverse(encodedPoint);
					BigInteger u = new BigInteger(1, encodedPoint);

					XECPublicKeySpec xecPublicKeySpec = new XECPublicKeySpec(new NamedParameterSpec(this.curve.getJCAName()), u);
					this.publicKey = (XECPublicKey) KeyFactory.getInstance(this.curve.getJCAName()).generatePublic(xecPublicKeySpec);
				}
				catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
					throw new JWKProcessingException("Error converting JWK to public key", e);
				}
			}
			else {
				this.publicKey = (XECPublicKey)this.certificate.getPublicKey();
			}
		}
		return this.publicKey;
	}

	@Override
	public Optional<XECPrivateKey> toPrivateKey() throws JWKProcessingException {
		if(this.privateKey == null && this.d != null) {
			try {
				XECPrivateKeySpec xecPrivateKeySpec = new XECPrivateKeySpec(new NamedParameterSpec(this.curve.getJCAName()), Base64.getUrlDecoder().decode(this.d));
				this.privateKey = (XECPrivateKey) KeyFactory.getInstance(this.curve.getJCAName()).generatePrivate(xecPrivateKeySpec);
			}
			catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
				throw new JWKProcessingException("Error converting JWK to private key", e);
			}
		}
		return Optional.ofNullable(this.privateKey);
	}

	@Override
	public XECJWK toPublicJWK() {
		GenericXECJWK jwk = new GenericXECJWK(this.curve, this.x, this.certificate);
		jwk.publicKey = this.publicKey;
		jwk.setPublicKeyUse(this.use);
		jwk.setKeyOperations(this.key_ops);
		jwk.setAlgorithm(this.xecAlg);
		jwk.setKeyId(this.kid);
		jwk.setX509CertificateURL(this.x5u);
		jwk.setX509CertificateChain(this.x5c);
		jwk.setX509CertificateSHA1Thumbprint(this.x5t);
		jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);
		
		return jwk;
	}

	@Override
	public XECJWK minify() {
		GenericXECJWK jwk = new GenericXECJWK(this.curve, this.x, this.d, (XECPrivateKey)this.key, this.certificate, this.trusted);
		jwk.publicKey = this.publicKey;
		
		return jwk;
	}
	
	@Override
	public boolean supportsAlgorithm(String alg) {
		try {
			XECAlgorithm.fromAlgorithm(alg);
			return true;
		} 
		catch(IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public JWAKeyManager keyManager() throws JWKProcessingException {
		this.checkKeyManagement(this.xecAlg);
		return this.getKeyManager(this.xecAlg);
	}

	@Override
	public JWAKeyManager keyManager(String alg) throws JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			return this.keyManager();
		}
		XECAlgorithm algorithm = XECAlgorithm.fromAlgorithm(alg);
		this.checkKeyManagement(algorithm);
		return this.getKeyManager(algorithm);
	}
	
	/**
	 * <p>
	 * Returns the key manager corresponding to the specified algorithm.
	 * </p>
	 * 
	 * <p>
	 * This method creates the key manager if it wasn't already created.
	 * </p>
	 * 
	 * @param algorithm an XEC algorithm
	 * 
	 * @return a key manager
	 */
	private	JWAKeyManager getKeyManager(XECAlgorithm algorithm) {
		if(this.keyManagers == null) {
			this.keyManagers = new HashMap<>();
		}
		return this.keyManagers.computeIfAbsent(algorithm, ign -> algorithm.createKeyManager(this));
	}
}

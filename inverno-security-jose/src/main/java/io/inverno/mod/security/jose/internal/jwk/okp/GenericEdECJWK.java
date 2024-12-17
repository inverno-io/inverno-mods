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

import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Generic Edward-Curve JSON Web Key implementation.
 * </p>
 * 
 * <p>
 * It supports the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>EdDSA with elliptic curve Ed25519 and Ed448.</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericEdECJWK extends AbstractOKPJWK<EdECPublicKey, EdECPrivateKey> implements EdECJWK {

	/**
	 * The set of curves supported by the JWK.
	 */
	public static final Set<OKPCurve> SUPPORTED_CURVES = Set.of(OKPCurve.ED25519, OKPCurve.ED448);
	
	private EdECAlgorithm edecAlg;
	
	private JWASigner signer;

	/**
	 * <p>
	 * Creates an untrusted public generic EdEC JWK with the specified curve and public key value.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the public key value encoded as Base64URL without padding
	 */
	public GenericEdECJWK(OKPCurve curve, String x) {
		super(curve, x);
	}

	/**
	 * <p>
	 * Creates a public generic EdEC JWK with the specified curve, public key value and certificate.
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
	public GenericEdECJWK(OKPCurve curve, String x, X509Certificate certificate) {
		super(curve, x, certificate);
	}

	/**
	 * <p>
	 * Creates an untrusted private generic EdEC JWK with the specified curve, public key value and private key value.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the public key value encoded as Base64URL without padding
	 * @param d     the private key value encoded as Base64URL without padding
	 */
	public GenericEdECJWK(OKPCurve curve, String x, String d) {
		super(curve, x, d);
	}

	/**
	 * <p>
	 * Creates a private generic EdEC JWK with the specified curve, public key value, private key value and private key.
	 * </p>
	 * 
	 * @param curve   an elliptic curve
	 * @param x       the public key value encoded as Base64URL without padding
	 * @param d       the private key value encoded as Base64URL without padding
	 * @param key     a private key
	 * @param trusted true to create a trusted JWK, false otherwise
	 */
	public GenericEdECJWK(OKPCurve curve, String x, String d, EdECPrivateKey key, boolean trusted) {
		super(curve, x, d, key, trusted);
	}

	/**
	 * <p>
	 * Creates a public generic EdEC JWK with the specified curve, public key value, private key value and certificate.
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
	public GenericEdECJWK(OKPCurve curve, String x, String d, X509Certificate certificate) {
		super(curve, x, d, certificate);
	}

	/**
	 * <p>
	 * Creates a private generic EdEC JWK with the specified curve, public coordinates, private key value, OKP private key and certificate.
	 * </p>
	 *
	 * @param curve       an elliptic curve
	 * @param x           the public key value encoded as Base64URL without padding
	 * @param d           the private key value encoded as Base64URL without padding
	 * @param key         a private key
	 * @param certificate an X.509 certificate
	 * @param trusted     true to create a trusted JWK, false otherwise
	 */
	public GenericEdECJWK(OKPCurve curve, String x, String d, EdECPrivateKey key, X509Certificate certificate, boolean trusted) {
		super(curve, x, d, key, certificate, trusted);
	}
	
	/**
	 * <p>
	 * Sets the Edward-Curve JWA algorithm.
	 * </p>
	 * 
	 * @param edecAlg an EdEC algorithm
	 */
	public void setAlgorithm(EdECAlgorithm edecAlg) {
		super.setAlgorithm(edecAlg != null ? edecAlg.getAlgorithm() : null);
		this.edecAlg = edecAlg;
	}

	@Override
	public void setAlgorithm(String alg) {
		this.edecAlg = alg != null ? EdECAlgorithm.fromAlgorithm(alg, this.curve) : null;
		super.setAlgorithm(alg);
	}
	
	@Override
	public EdECJWK trust() {
		this.trusted = true;
		return this;
	}
	
	@Override
	public EdECPublicKey toPublicKey() throws JWKProcessingException {
		if(this.publicKey == null) {
			if(this.certificate == null) {
				try {
					byte[] encodedPoint = Base64.getUrlDecoder().decode(this.x);
					byte msb = encodedPoint[encodedPoint.length - 1];
					encodedPoint[encodedPoint.length - 1] &= (byte) 0x7F;
					boolean xOdd = (msb & 0x80) != 0;
					reverse(encodedPoint);
					BigInteger y = new BigInteger(1, encodedPoint);

					EdECPublicKeySpec edEcPublicKeySpec = new EdECPublicKeySpec(new NamedParameterSpec(this.curve.getJCAName()), new EdECPoint(xOdd, y));
					this.publicKey = (EdECPublicKey) KeyFactory.getInstance(this.curve.getJCAName()).generatePublic(edEcPublicKeySpec);
				}
				catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
					throw new JWKProcessingException("Error converting JWK to public key", e);
				}
			}
			else {
				this.publicKey = (EdECPublicKey)this.certificate.getPublicKey();
			}
		}
		return this.publicKey;
	}
	
	@Override
	public Optional<EdECPrivateKey> toPrivateKey() throws JWKProcessingException {
		if(this.privateKey == null && this.d != null) {
			try {
				EdECPrivateKeySpec edEcPrivateKeySpec = new EdECPrivateKeySpec(new NamedParameterSpec(this.curve.getJCAName()), Base64.getUrlDecoder().decode(this.d));
				this.privateKey = (EdECPrivateKey) KeyFactory.getInstance(this.curve.getJCAName()).generatePrivate(edEcPrivateKeySpec);
			}
			catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
				throw new JWKProcessingException("Error converting JWK to private key", e);
			}
		}
		return Optional.ofNullable(this.privateKey);
	}

	@Override
	public EdECJWK toPublicJWK() {
		GenericEdECJWK jwk = new GenericEdECJWK(this.curve, this.x, this.certificate);
		jwk.publicKey = this.publicKey;
		jwk.setPublicKeyUse(this.use);
		jwk.setKeyOperations(this.key_ops);
		jwk.setAlgorithm(this.edecAlg);
		jwk.setKeyId(this.kid);
		jwk.setX509CertificateURL(this.x5u);
		jwk.setX509CertificateChain(this.x5c);
		jwk.setX509CertificateSHA1Thumbprint(this.x5t);
		jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);
		
		return jwk;
	}

	@Override
	public EdECJWK minify() {
		GenericEdECJWK jwk = new GenericEdECJWK(this.curve, this.x, this.d, (EdECPrivateKey)this.key, this.certificate, this.trusted);
		jwk.publicKey = this.publicKey;
		
		return jwk;
	}

	@Override
	public boolean supportsAlgorithm(String alg) {
		try {
			EdECAlgorithm.fromAlgorithm(alg, this.curve);
			return true;
		} 
		catch(IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public JWASigner signer() throws JWKProcessingException {
		this.checkSignature(this.edecAlg);
		
		// We know we can only have one signer since the JWK is defined for a single curve
		if(this.signer == null) {
			this.signer = this.edecAlg.createSigner(this);
		}
		return this.signer;
	}

	@Override
	public JWASigner signer(String alg) throws JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			return this.signer();
		}
		EdECAlgorithm algorithm = EdECAlgorithm.fromAlgorithm(alg, this.curve);
		this.checkSignature(algorithm);
		
		// We know we can only have one signer since the JWK is defined for a single curve
		if(this.signer == null) {
			this.signer = algorithm.createSigner(this);
		}
		return this.signer;
	}
	
	@Override
	protected void checkSignature(JWAAlgorithm<?> algorithm) throws JWKProcessingException {
		super.checkSignature(algorithm);
		if(!this.curve.equals(((EdECAlgorithm)algorithm).getCurve())) {
			throw new JWKProcessingException("JWK with curve " + this.curve + " doesn't support algorithm " + algorithm);
		}
	}
}

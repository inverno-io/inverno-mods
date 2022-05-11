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
package io.inverno.mod.security.jose.internal.jwk.ec;

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWK;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Generic Elliptic Curve JSON Web Key implementation.
 * </p>
 *
 * <p>
 * It supports the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>ES256</li>
 * <li>ES384</li>
 * <li>ES512</li>
 * <li>ES256K (deprecated)</li>
 * <li>ECDH-ES with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A128KW with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A192KW with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A256KW with elliptic curve P-256, P-384 or P-521</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericECJWK extends AbstractX509JWK<ECPublicKey, ECPrivateKey> implements ECJWK {
	
	private final ECCurve curve;
	private final String x;
	private final String y;
	private final String d;
	
	private ECAlgorithm ecAlg;

	private ECPublicKey publicKey;
	private Optional<ECPrivateKey> privateKey;
	
	private JWASigner signer;
	private Map<ECAlgorithm, JWAKeyManager> keyManagers;
	
	/**
	 * <p>
	 * Creates an untrusted public generic EC JWK with the specified curve and public coordinates.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the x coordinate encoded as Base64URL without padding
	 * @param y     the y coordinate encoded as Base64URL without padding
	 */
	public GenericECJWK(ECCurve curve, String x, String y) {
		this(curve, x, y, null, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a public generic EC JWK with the specified curve, public coordinates and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param curve       an elliptic curve
	 * @param x           the x coordinate encoded as Base64URL without padding
	 * @param y           the y coordinate encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public GenericECJWK(ECCurve curve, String x, String y, X509Certificate certificate) {
		this(curve, x, y, null, null, certificate, certificate != null);
	}
	
	/**
	 * <p>
	 * Creates an untrusted private generic EC JWK with the specified curve, public coordinates and private key value.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the x coordinate encoded as Base64URL without padding
	 * @param y     the y coordinate encoded as Base64URL without padding
	 * @param d     the private key value encoded as Base64URL without padding
	 */
	public GenericECJWK(ECCurve curve, String x, String y, String d) {
		this(curve, x, y, d, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a private generic EC JWK with the specified curve, public coordinates, private key value and EC private key.
	 * </p>
	 * 
	 * @param curve   an elliptic curve
	 * @param x       the x coordinate encoded as Base64URL without padding
	 * @param y       the y coordinate encoded as Base64URL without padding
	 * @param d       the private key value encoded as Base64URL without padding
	 * @param key     an EC private key
	 * @param trusted true to create a trusted JWK, false otherwise
	 */
	public GenericECJWK(ECCurve curve, String x, String y, String d, ECPrivateKey key, boolean trusted) {
		this(curve, x, y, d, key, null, trusted);
	}
	
	/**
	 * <p>
	 * Creates a public generic EC JWK with the specified curve, public coordinates, private key value and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param curve       an elliptic curve
	 * @param x           the x coordinate encoded as Base64URL without padding
	 * @param y           the y coordinate encoded as Base64URL without padding
	 * @param d           the private key value encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public GenericECJWK(ECCurve curve, String x, String y, String d, X509Certificate certificate) {
		this(curve, x, y, d, null, certificate, certificate != null);
	}
	
	/**
	 * <p>
	 * Creates a private generic EC JWK with the specified curve, public coordinates, private key value, EC private key and certificate.
	 * </p>
	 *
	 * @param curve       an elliptic curve
	 * @param x           the x coordinate encoded as Base64URL without padding
	 * @param y           the y coordinate encoded as Base64URL without padding
	 * @param d           the private key value encoded as Base64URL without padding
	 * @param key         an EC private key
	 * @param certificate an X.509 certificate
	 * @param trusted     true to create a trusted JWK, false otherwise
	 */
	public GenericECJWK(ECCurve curve, String x, String y, String d, ECPrivateKey key, X509Certificate certificate, boolean trusted) {
		super(KEY_TYPE, key, certificate, trusted);
		this.curve = curve;
		this.x = x;
		this.y = y;
		this.d = d;
		this.privateKey = key != null ? Optional.of(key) : null;
	}

	/**
	 * <p>
	 * Sets the Elliptic Curve JWA algorithm.
	 * </p>
	 * 
	 * @param ecAlg an EC algorithm
	 */
	public void setAlgorithm(ECAlgorithm ecAlg) {
		this.ecAlg = ecAlg;
		super.setAlgorithm(ecAlg != null ? ecAlg.getAlgorithm() : null);
	}
	
	@Override
	public void setAlgorithm(String alg) {
		this.ecAlg = alg != null ? ECAlgorithm.fromAlgorithm(alg) : null;
		super.setAlgorithm(alg);
	}

	@Override
	public String getCurve() {
		return this.curve.getCurve();
	}

	@Override
	public String getXCoordinate() {
		return this.x;
	}

	@Override
	public String getYCoordinate() {
		return this.y;
	}
	
	@Override
	public String getEccPrivateKey() {
		return this.d;
	}
	
	@Override
	public ECJWK trust() {
		this.trusted = true;
		return this;
	}

	@Override
	public ECPublicKey toPublicKey() throws JWKProcessingException {
		if(this.publicKey == null) {
			this.publicKey = this.certificate
				.map(cert -> (ECPublicKey)cert.getPublicKey())
				.orElseGet(() -> {
					try {
						ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(
							new ECPoint(
								new BigInteger(1, Base64.getUrlDecoder().decode(this.x)), 
								new BigInteger(1, Base64.getUrlDecoder().decode(this.y))
							), 
							this.curve.getParameterSpec()
						);
						return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(ecPublicKeySpec);
					}
					catch(NoSuchAlgorithmException | InvalidKeySpecException e) {
						throw new JWKProcessingException("Error creating verifier", e);
					}
				});
		}
		return this.publicKey;
	}

	@Override
	public Optional<ECPrivateKey> toPrivateKey() throws JWKProcessingException {
		if(this.privateKey == null) {
			this.privateKey = Optional.ofNullable(this.d)
				.map(pk -> {
					try {
						ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(
							new BigInteger(1, Base64.getUrlDecoder().decode(pk)), 
							this.curve.getParameterSpec()
						);
						return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(ecPrivateKeySpec);
					} 
					catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
						throw new JWKProcessingException("Error creating signer", e);
					}
				});
		}
		return this.privateKey;
	}

	@Override
	public ECJWK toPublicJWK() {
		GenericECJWK jwk = new GenericECJWK(this.curve, this.x, this.y, this.certificate.orElse(null));
		jwk.publicKey = this.publicKey;
		jwk.setPublicKeyUse(this.use);
		jwk.setKeyOperations(this.key_ops);
		jwk.setAlgorithm(this.ecAlg);
		jwk.setKeyId(this.kid);
		jwk.setX509CertificateURL(this.x5u);
		jwk.setX509CertificateChain(this.x5c);
		jwk.setX509CertificateSHA1Thumbprint(this.x5t);
		jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);
		
		return jwk;
	}
	
	@Override
	public ECJWK minify() {
		GenericECJWK jwk = new GenericECJWK(this.curve, this.x, this.y, this.d, (ECPrivateKey)this.key, this.certificate.orElse(null), this.trusted);
		jwk.publicKey = this.publicKey;
		
		return jwk;
	}
	
	@Override
	public String toJWKThumbprint(MessageDigest digest) {
		return toJWKThumbprint(digest, this.curve.getCurve(), this.kty, this.x, this.y);
	}
	
	/**
	 * <p>
	 * Generates and returns an EC JWK thumbprint using the specified digest.
	 * </p>
	 *
	 * @param digest the message digest to use
	 * @param crv    the JWA elliptic curve
	 * @param kty    the key type ({@code EC})
	 * @param x      the x coordinate encoded as Base64URL without padding
	 * @param y      the y coordinate encoded as Base64URL without padding
	 *
	 * @return an EC JWK thumbprint or null if some input data are null
	 */
	static String toJWKThumbprint(MessageDigest digest, String crv, String kty, String x, String y) {
		if(crv == null || kty == null || x == null || y == null) {
			return null;
		}
		StringBuilder input = new StringBuilder();
		input.append('{');
		input.append("\"crv\":\"").append(crv).append("\",");
		input.append("\"kty\":\"").append(kty).append("\",");
		input.append("\"x\":\"").append(x).append("\",");
		input.append("\"y\":\"").append(y).append("\"");
		input.append('}');
		
		return JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(digest.digest(input.toString().getBytes()));
	}
	
	@Override
	public boolean supportsAlgorithm(String alg) {
		// if we can return false because of the curve we should do it
		try {
			ECAlgorithm algorithm = ECAlgorithm.fromAlgorithm(alg);
			
			// Unlike signing algorithm, key management algorithms (ECDH-ES) do not use a fixed curve
			return algorithm.getCurve() == null || algorithm.getCurve().equals(this.curve);
		} 
		catch(IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public JWASigner signer() throws JWKProcessingException {
		this.checkSignature(this.ecAlg);
		// We know we can only have one signer since the JWK is defined for a single curve and we only support one signature algorithm per curve
		if(this.signer == null) {
			this.signer = this.ecAlg.createSigner(this);
		}
		return this.signer;
	}
	
	@Override
	public JWASigner signer(String alg) {
		if(StringUtils.isBlank(alg)) {
			return this.signer();
		}
		ECAlgorithm algorithm = ECAlgorithm.fromAlgorithm(alg);
		this.checkSignature(algorithm);
		
		// We know we can only have one signer since the JWK is defined for a single curve and we only support one signature algorithm per curve
		if(this.signer == null) {
			this.signer = algorithm.createSigner(this);
		}
		return this.signer;
	}
	
	@Override
	protected void checkSignature(JWAAlgorithm<?> algorithm) throws JWKProcessingException {
		super.checkSignature(algorithm);
		if(!this.curve.equals(((ECAlgorithm)algorithm).getCurve())) {
			throw new JWKProcessingException("JWK with curve " + this.curve + " doesn't support algorithm " + algorithm);
		}
	}

	@Override
	public JWAKeyManager keyManager() throws JWKProcessingException {
		this.checkKeyManagement(this.ecAlg);
		return this.getKeyManager(this.ecAlg);
	}

	@Override
	public JWAKeyManager keyManager(String alg) throws JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			return this.keyManager();
		}
		ECAlgorithm algorithm = ECAlgorithm.fromAlgorithm(alg);
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
	 * @param algorithm an EC algorithm
	 * 
	 * @return a key manager
	 */
	private	JWAKeyManager getKeyManager(ECAlgorithm algorithm) {
		if(this.keyManagers == null) {
			this.keyManagers = new HashMap<>();
		}
		return this.keyManagers.computeIfAbsent(algorithm, ign -> {
			return algorithm.createKeyManager(this);
		});
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(curve, d, x, y);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericECJWK other = (GenericECJWK) obj;
		return curve == other.curve && Objects.equals(d, other.d) && Objects.equals(x, other.x)
				&& Objects.equals(y, other.y);
	}
}

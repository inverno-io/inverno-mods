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
package io.inverno.mod.security.jose.internal.jwk.rsa;

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWK;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAMultiPrimePrivateCrtKeySpec;
import java.security.spec.RSAOtherPrimeInfo;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Generic RSA JSON Web Key implementation.
 * </p>
 * 
 * <p>
 * It supports the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>RS1</li>
 * <li>RS256</li>
 * <li>RS384</li>
 * <li>RS512</li>
 * <li>PS256</li>
 * <li>PS384</li>
 * <li>PS512</li>
 * <li>RSA1_5</li>
 * <li>RSA-OAEP</li>
 * <li>RSA-OAEP-256</li>
 * <li>RSA-OAEP-384</li>
 * <li>RSA-OAEP-512</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericRSAJWK extends AbstractX509JWK<RSAPublicKey, RSAPrivateKey> implements RSAJWK {

	private final String n;
	private final String e;
	private final String d;
	private String p;
	private String q;
	private String dp;
	private String dq;
	private String qi;
	private List<RSAJWK.OtherPrimeInfo> oth;
	
	private RSAAlgorithm rsaAlg;
	
	private RSAPublicKey publicKey;
	private Optional<RSAPrivateKey> privateKey;
	
	private Map<RSAAlgorithm, JWASigner> signers;
	private Map<RSAAlgorithm, JWAKeyManager> keyManagers;

	/**
	 * <p>
	 * Creates an untrusted public generic RSA JWK with the specified public key parameters.
	 * </p>
	 * 
	 * @param n     the modulus encoded as Base64URL without padding
	 * @param e     the public exponent encoded as Base64URL without padding
	 */
	public GenericRSAJWK(String n, String e) {
		this(n, e, null, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a public generic RSA JWK with the specified public key parameters and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param n     the modulus encoded as Base64URL without padding
	 * @param e     the public exponent encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public GenericRSAJWK(String n, String e, X509Certificate certificate) {
		this(n, e, null, null, certificate, certificate != null);
	}
	
	/**
	 * <p>
	 * Creates an untrusted private generic RSA JWK with the specified public key parameters and private exponent.
	 * </p>
	 * 
	 * @param n     the modulus encoded as Base64URL without padding
	 * @param e     the public exponent encoded as Base64URL without padding
	 * @param d     the private exponent encoded as Base64URL without padding
	 */
	public GenericRSAJWK(String n, String e, String d) {
		this(n, e, d, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a private generic RSA JWK with the specified public key parameters, private exponent and RSA private key.
	 * </p>
	 * 
	 * @param n     the modulus encoded as Base64URL without padding
	 * @param e     the public exponent encoded as Base64URL without padding
	 * @param d     the private exponent encoded as Base64URL without padding
	 * @param key     an EC private key
	 * @param trusted true to create a trusted JWK, false otherwise
	 */
	public GenericRSAJWK(String n, String e, String d, RSAPrivateKey key, boolean trusted) {
		this(n, e, d, key, null, trusted);
	}
	
	/**
	 * <p>
	 * Creates a public generic RSA JWK with the specified public key parameters, private exponent and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param n     the modulus encoded as Base64URL without padding
	 * @param e     the public exponent encoded as Base64URL without padding
	 * @param d     the private exponent encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public GenericRSAJWK(String n, String e, String d, X509Certificate certificate) {
		this(n, e, d, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a private generic RSA JWK with the specified public key parameters, private exponent, RSA private key and certificate.
	 * </p>
	 *
	 * @param n           the modulus encoded as Base64URL without padding
	 * @param e           the public exponent encoded as Base64URL without padding
	 * @param d           the private exponent encoded as Base64URL without padding
	 * @param key         an RSA private key
	 * @param certificate an X.509 certificate
	 * @param trusted     true to create a trusted JWK, false otherwise
	 */
	public GenericRSAJWK(String n, String e, String d, RSAPrivateKey key, X509Certificate certificate, boolean trusted) {
		super(KEY_TYPE, key, certificate, trusted);
		this.n = n;
		this.e = e;
		this.d = d;
		this.privateKey = key != null ? Optional.of(key) : null;
	}

	/**
	 * <p>
	 * Sets the RSA JWA algorithm.
	 * </p>
	 * 
	 * @param rsaAlg an RSA algorithm
	 */
	public void setAlgorithm(RSAAlgorithm rsaAlg) {
		this.rsaAlg = rsaAlg;
		super.setAlgorithm(rsaAlg != null ? rsaAlg.getAlgorithm() : null);
	}
	
	@Override
	public void setAlgorithm(String alg) {
		this.rsaAlg = alg != null ? RSAAlgorithm.fromAlgorithm(alg) : null;
		super.setAlgorithm(alg);
	}
	
	@Override
	public String getModulus() {
		return this.n;
	}

	@Override
	public String getPublicExponent() {
		return this.e;
	}
	
	@Override
	public String getPrivateExponent() {
		return this.d;
	}
	
	/**
	 * <p>
	 * Sets the first prime factor encoded as Base64URL.
	 * </p>
	 * 
	 * @param p a Base64URL encoded first prime factor without padding
	 */
	public void setFirstPrimeFactor(String p) {
		this.p = p;
	}

	@Override
	public String getFirstPrimeFactor() {
		return this.p;
	}

	/**
	 * <p>
	 * Sets the second prime factor encoded as Base64URL.
	 * </p>
	 * 
	 * @param q a Base64URL encoded second prime factor without padding
	 */
	public void setSecondPrimeFactor(String q) {
		this.q = q;
	}
	
	@Override
	public String getSecondPrimeFactor() {
		return this.q;
	}

	/**
	 * <p>
	 * Sets the first factor exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @param dp a Base64URL encoded first factor exponent without padding
	 */
	public void setFirstFactorExponent(String dp) {
		this.dp = dp;
	}
	
	@Override
	public String getFirstFactorExponent() {
		return this.dp;
	}

	/**
	 * <p>
	 * Sets the second factor exponent encoded as Base64URL.
	 * </p>
	 * 
	 * @param dq a Base64URL encoded second factor exponent without padding
	 */
	public void setSecondFactorExponent(String dq) {
		this.dq = dq;
	}
	
	@Override
	public String getSecondFactorExponent() {
		return this.dq;
	}

	/**
	 * <p>
	 * Sets the first coefficient encoded as Base64URL.
	 * </p>
	 * 
	 * @param qi a Base64URL encoded first coefficient without padding
	 */
	public void setFirstCoefficient(String qi) {
		this.qi = qi;
	}
	
	@Override
	public String getFirstCoefficient() {
		return this.qi;
	}

	/**
	 * <p>
	 * Sets other prime info.
	 * </p>
	 * 
	 * @param oth a list of other prime info
	 */
	public void setOtherPrimesInfo(List<RSAJWK.OtherPrimeInfo> oth) {
		this.oth = oth != null && !oth.isEmpty() ? Collections.unmodifiableList(oth) : null;
	}
	
	@Override
	public List<RSAJWK.OtherPrimeInfo> getOtherPrimesInfo() {
		return this.oth;
	}

	@Override
	public RSAJWK trust() {
		this.trusted = true;
		return this;
	}
	
	@Override
	public RSAPublicKey toPublicKey() throws JWKProcessingException {
		if(this.publicKey == null) {
			this.publicKey = this.certificate
				.map(cert -> (RSAPublicKey)cert.getPublicKey())
				.orElseGet(() -> {
					try {
						RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
							new BigInteger(1, Base64.getUrlDecoder().decode(this.n)), 
							new BigInteger(1, Base64.getUrlDecoder().decode(this.e))
						);
						return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);
					} 
					catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
						throw new JWKProcessingException("Error creating verifier", e);
					}
				});
		}
		return this.publicKey;
	}

	@Override
	public Optional<RSAPrivateKey> toPrivateKey() throws JWKProcessingException {
		if(this.privateKey == null) {
			this.privateKey = Optional.ofNullable(this.d)
				.map(pe -> {
					try {
						BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(this.n));
						BigInteger privateExponent = new BigInteger(1, Base64.getUrlDecoder().decode(pe));

						final RSAPrivateKeySpec rsaPrivateKeySpec;
						if(this.getFirstPrimeFactor() != null) {
							BigInteger publicExponent = new BigInteger(1, Base64.getUrlDecoder().decode(this.e));
							BigInteger primeP = new BigInteger(1, Base64.getUrlDecoder().decode(this.p));
							BigInteger primeQ = new BigInteger(1, Base64.getUrlDecoder().decode(this.q));
							BigInteger primeExponentP = new BigInteger(1, Base64.getUrlDecoder().decode(this.dp));
							BigInteger primeExponentQ = new BigInteger(1, Base64.getUrlDecoder().decode(this.dq));
							BigInteger crtCoefficient = new BigInteger(1, Base64.getUrlDecoder().decode(this.qi));
							RSAOtherPrimeInfo[] otherPrimeInfo = null;
							if(this.getOtherPrimesInfo() != null) {
								otherPrimeInfo = this.getOtherPrimesInfo().stream()
									.map(o -> new RSAOtherPrimeInfo(
										new BigInteger(1, Base64.getUrlDecoder().decode(o.getPrimeFactor())), 
										new BigInteger(1, Base64.getUrlDecoder().decode(o.getExponent())), 
										new BigInteger(1, Base64.getUrlDecoder().decode(o.getCoefficient())))
									)
									.toArray(RSAOtherPrimeInfo[]::new);
							}
							rsaPrivateKeySpec = new RSAMultiPrimePrivateCrtKeySpec(modulus, publicExponent, privateExponent, primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient, otherPrimeInfo);
						}
						else {
							rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, privateExponent);
						}
						return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(rsaPrivateKeySpec);
					} 
					catch(NoSuchAlgorithmException | InvalidKeySpecException e) {
						throw new JWKProcessingException("Error creating signer", e);
					}
				});
		}
		return this.privateKey;
	}
	
	@Override
	public RSAJWK toPublicJWK() {
		GenericRSAJWK jwk = new GenericRSAJWK(this.n, this.e, this.certificate.orElse(null));
		jwk.publicKey = this.publicKey;
		jwk.setPublicKeyUse(this.use);
		jwk.setKeyOperations(this.key_ops);
		jwk.setAlgorithm(this.rsaAlg);
		jwk.setKeyId(this.kid);
		jwk.setX509CertificateURL(this.x5u);
		jwk.setX509CertificateChain(this.x5c);
		jwk.setX509CertificateSHA1Thumbprint(this.x5t);
		jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);
		
		return jwk;
	}

	@Override
	public RSAJWK minify() {
		GenericRSAJWK jwk = new GenericRSAJWK(this.n, this.e, this.d, (RSAPrivateKey)this.key, this.certificate.orElse(null), this.trusted);
		jwk.publicKey = this.publicKey;
		
		return jwk;
	}
	
	@Override
	public String toJWKThumbprint(MessageDigest digest) {
		return toJWKThumbprint(digest, this.e, this.kty, this.n);
	}
	
	/**
	 * <p>
	 * Generates and returns an RSA JWK thumbprint using the specified digest.
	 * </p>
	 *
	 * @param digest the message digest to use
	 * @param e the public exponent encoded as Base64URL without padding
	 * @param kty the key type ({@code RSA})
	 * @param n the modulus encoded as Base64URL without padding
	 * 
	 * @return an RSA JWK thumbprint or null if some input data are null
	 */
	static String toJWKThumbprint(MessageDigest digest, String e, String kty, String n) {
		if(e == null || kty == null || n == null) {
			return null;
		}
		StringBuilder input = new StringBuilder();
		input.append('{');
		input.append("\"e\":\"").append(e).append("\",");
		input.append("\"kty\":\"").append(kty).append("\",");
		input.append("\"n\":\"").append(n).append("\"");
		input.append('}');
		
		return JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(digest.digest(input.toString().getBytes()));
	}
	
	@Override
	public boolean supportsAlgorithm(String alg) {
		try {
			RSAAlgorithm.fromAlgorithm(alg);
			return true;
		}
		catch(IllegalArgumentException e) {
			return false;
		}
	}
	
	@Override
	public JWASigner signer() throws JWKProcessingException {
		this.checkSignature(this.rsaAlg);
		return this.getSigner(this.rsaAlg);
	}
	
	@Override
	public JWASigner signer(String alg) {
		if(StringUtils.isBlank(alg)) {
			return this.signer();
		}
		RSAAlgorithm algorithm = RSAAlgorithm.fromAlgorithm(alg);
		this.checkSignature(algorithm);
		return this.getSigner(algorithm);
	}
	
	/**
	 * <p>
	 * Returns the signer corresponding to the specified algorithm.
	 * </p>
	 * 
	 * <p>
	 * This method creates the signer if it wasn't already created.
	 * </p>
	 * 
	 * @param algorithm an RSA algorithm
	 * 
	 * @return a signer
	 */
	private JWASigner getSigner(RSAAlgorithm algorithm) {
		if(this.signers == null) {
			this.signers = new HashMap<>();
		}
		return this.signers.computeIfAbsent(algorithm, ign -> algorithm.createSigner(this));
	}

	@Override
	public JWAKeyManager keyManager() throws JWKProcessingException {
		this.checkEncryption(this.rsaAlg);
		return this.getKeyManager(rsaAlg);
	}

	@Override
	public JWAKeyManager keyManager(String alg) throws JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			return this.keyManager();
		}
		RSAAlgorithm algorithm = RSAAlgorithm.fromAlgorithm(alg);
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
	 * @param algorithm an RSA algorithm
	 * 
	 * @return a key manager
	 */
	private	JWAKeyManager getKeyManager(RSAAlgorithm algorithm) {
		if(this.keyManagers == null) {
			this.keyManagers = new HashMap<>();
		}
		return this.keyManagers.computeIfAbsent(algorithm, ign -> algorithm.createKeyManager(this));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(d, dp, dq, e, n, oth, p, q, qi);
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
		GenericRSAJWK other = (GenericRSAJWK) obj;
		return Objects.equals(d, other.d) && Objects.equals(dp, other.dp) && Objects.equals(dq, other.dq)
				&& Objects.equals(e, other.e) && Objects.equals(n, other.n) && Objects.equals(oth, other.oth)
				&& Objects.equals(p, other.p) && Objects.equals(q, other.q) && Objects.equals(qi, other.qi);
	}



	/**
	 * <p>
	 * Generic {@code RSAJWK.OtherPrimeInfo} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class GenericOtherPrimeInfo implements RSAJWK.OtherPrimeInfo {
		
		private final String r;
		private final String d;
		private final String t;

		/**
		 * <p>
		 * Creates a generic other prime info.
		 * </p>
		 *
		 * @param r the prime factor encoded as Base64URL without padding
		 * @param d the factor CRT exponent encoded as Base64URL without padding
		 * @param t the factor CRT coefficient encoded as Base64URL without padding
		 *
		 * @throws JWKProcessingException if one of the parameter is null
		 */
		public GenericOtherPrimeInfo(String r, String d, String t) throws JWKProcessingException {
			if(StringUtils.isBlank(r)) {
				throw new JWKProcessingException("Prime factor is blank");
			}
			if(StringUtils.isBlank(d)) {
				throw new JWKProcessingException("Exponent is blank");
			}
			if(StringUtils.isBlank(t)) {
				throw new JWKProcessingException("Coefficient is blank");
			}
			
			this.r = r;
			this.d = d;
			this.t = t;
		}

		@Override
		public String getPrimeFactor() {
			return this.r;
		}

		@Override
		public String getExponent() {
			return this.d;
		}

		@Override
		public String getCoefficient() {
			return this.t;
		}

		@Override
		public int hashCode() {
			return Objects.hash(d, r, t);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GenericOtherPrimeInfo other = (GenericOtherPrimeInfo) obj;
			return Objects.equals(d, other.d) && Objects.equals(r, other.r) && Objects.equals(t, other.t);
		}
	}
}

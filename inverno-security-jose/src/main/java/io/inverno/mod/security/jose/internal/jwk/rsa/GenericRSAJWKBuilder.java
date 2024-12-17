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

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKBuilder;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWKBuilder;
import java.math.BigInteger;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic RSA JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericRSAJWKBuilder extends AbstractX509JWKBuilder<RSAPublicKey, RSAPrivateKey, GenericRSAJWK, GenericRSAJWKBuilder> implements RSAJWKBuilder<GenericRSAJWK, GenericRSAJWKBuilder> {

	private static final Set<String> SUPPORTED_SIG_JCA_ALGORITHMS = Arrays.stream(RSAAlgorithm.values())
		.filter(RSAAlgorithm::isSignature)
		.map(RSAAlgorithm::getJcaAlgorithm)
		.collect(Collectors.toSet());
	
	private String n;
	private String e;
	private String d;
	private String p;
	private String q;
	private String dp;
	private String dq;
	private String qi;
	private List<RSAJWK.OtherPrimeInfo> oth;
	
	private RSAAlgorithm rsaAlg;
	
	/**
	 * <p>
	 * Creates a generic RSA JWK builder.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericRSAJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		this(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, null);
	}
	
	/**
	 * <p>
	 * Creates a generic RSA JWK builder initialized with the specified parameters map.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 * @param parameters        a parameters map used to initialize the builder
	 * 
	 * @throws JWKReadException if there was an error reading the parameters map
	 */
	public GenericRSAJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, parameters);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void set(String field, Object value) throws JWKReadException {
		switch(field) {
			case "n": {
				this.modulus((String)value);
				break;
			}
			case "e": {
				this.publicExponent((String)value);
				break;
			}
			case "d": {
				this.privateExponent((String)value);
				break;
			}
			case "p": {
				this.firstPrimeFactor((String)value);
				break;
			}
			case "q": {
				this.secondPrimeFactor((String)value);
				break;
			}
			case "dp": {
				this.firstFactorExponent((String)value);
				break;
			}
			case "dq": {
				this.secondFactorExponent((String)value);
				break;
			}
			case "qi": {
				this.firstCoefficient((String)value);
				break;
			}
			case "oth": {
				List<Map<String, Object>> oth = (List<Map<String, Object>>)value;
				if(oth != null && !oth.isEmpty()) {
					oth.forEach(m -> this.otherPrimeInfo((String)m.get("r"), (String)m.get("d"), (String)m.get("t")));
				}
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}

	@Override
	public GenericRSAJWKBuilder algorithm(String alg) {
		this.rsaAlg = alg != null ? RSAAlgorithm.fromAlgorithm(alg) : null;
		return super.algorithm(alg);
	}
	
	@Override
	public GenericRSAJWKBuilder modulus(String n) {
		this.n = n;
		return this;
	}

	@Override
	public GenericRSAJWKBuilder publicExponent(String e) {
		this.e = e;
		return this;
	}
	
	@Override
	public GenericRSAJWKBuilder privateExponent(String d) {
		this.d = d;
		return this;
	}

	@Override
	public GenericRSAJWKBuilder firstPrimeFactor(String p) {
		this.p = p;
		return this;
	}

	@Override
	public GenericRSAJWKBuilder secondPrimeFactor(String q) {
		this.q = q;
		return this;
	}

	@Override
	public GenericRSAJWKBuilder firstFactorExponent(String dp) {
		this.dp = dp;
		return this;
	}

	@Override
	public GenericRSAJWKBuilder secondFactorExponent(String dq) {
		this.dq = dq;
		return this;
	}

	@Override
	public GenericRSAJWKBuilder firstCoefficient(String qi) {
		this.qi = qi;
		return this;
	}

	@Override
	public GenericRSAJWKBuilder otherPrimeInfo(String primeFactor, String exponent, String coefficient) {
		if(this.oth == null) {
			this.oth = new LinkedList<>();
		}
		this.oth.add(new GenericRSAJWK.GenericOtherPrimeInfo(primeFactor, exponent, coefficient));
		return this;
	}
	
	@Override
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		String thumbprint = GenericRSAJWK.toJWKThumbprint(JWK.DEFAULT_THUMBPRINT_DIGEST, this.e, RSAJWK.KEY_TYPE, this.n);
		return super.resolveFromJWKStore().switchIfEmpty(Mono.defer(() -> this.jwkStore.getByJWKThumbprint(thumbprint))).doOnNext(jwk -> {
			if(!(jwk instanceof RSAJWK)) {
				throw new JWKResolveException("Stored JWK is not of expected type: " + RSAJWK.class);
			}
			RSAJWK rsaJWK = (RSAJWK)jwk;
			if((this.e != null && !this.e.equals(rsaJWK.getPublicExponent())) || 
				(this.n != null && !this.n.equals(rsaJWK.getModulus())) || 
				(this.d != null && !this.d.equals(rsaJWK.getPrivateExponent())) ||
				(this.p != null && !this.p.equals(rsaJWK.getFirstPrimeFactor())) ||
				(this.q != null && !this.q.equals(rsaJWK.getSecondPrimeFactor())) ||
				(this.dp != null && !this.dp.equals(rsaJWK.getFirstFactorExponent())) ||
				(this.dq != null && !this.dq.equals(rsaJWK.getSecondFactorExponent())) ||
				(this.qi != null && !this.qi.equals(rsaJWK.getFirstCoefficient())) ||
				(this.oth != null && !this.oth.equals(rsaJWK.getOtherPrimesInfo()))
			) {
				throw new JWKResolveException("JWK parameters does not match stored JWK");
			}
		});
	}

	@Override
	protected Mono<Void> resolveKey(Key key) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.justOrEmpty(key)
			.flatMap(tmpKey -> {
				if(!(tmpKey instanceof RSAPrivateKey)) {
					throw new JWKBuildException("Key is not a RSA private key");
				}
				RSAPrivateKey rsaPrivateKey = (RSAPrivateKey)tmpKey;
				
				if(this.n == null) {
					this.n = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPrivateKey.getModulus()));
				}
				else if(!rsaPrivateKey.getModulus().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.n)))) {
					throw new JWKBuildException("Resolved private key does not match JWK parameters");
				}
				
				if(this.d == null) {
					this.d = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPrivateKey.getPrivateExponent()));
				}
				else if(!rsaPrivateKey.getPrivateExponent().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.d)))) {
					throw new JWKBuildException("Resolved private key does not match JWK parameters");
				}
				
				// TODO key can be RSAPrivateCrtKey if that's the case we can also include p, q, dp, dq, qi
				
				if(rsaPrivateKey instanceof RSAPrivateCrtKey) {
					RSAPrivateCrtKey rsaPrivateCrtKey = (RSAPrivateCrtKey)rsaPrivateKey;
					
					if(this.p == null) {
						this.p = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPrivateCrtKey.getPrimeP()));
					}
					else if(!rsaPrivateCrtKey.getPrimeP().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.p)))) {
						throw new JWKBuildException("Resolved private key does not match JWK parameters");
					}
					
					if(this.q == null) {
						this.q = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPrivateCrtKey.getPrimeQ()));
					}
					else if(!rsaPrivateCrtKey.getPrimeQ().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.q)))) {
						throw new JWKBuildException("Resolved private key does not match JWK parameters");
					}
					
					if(this.dp == null) {
						this.dp = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPrivateCrtKey.getPrimeExponentP()));
					}
					else if(!rsaPrivateCrtKey.getPrimeExponentP().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.dp)))) {
						throw new JWKBuildException("Resolved private key does not match JWK parameters");
					}
					
					if(this.dq == null) {
						this.dq = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPrivateCrtKey.getPrimeExponentQ()));
					}
					else if(!rsaPrivateCrtKey.getPrimeExponentQ().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.dq)))) {
						throw new JWKBuildException("Resolved private key does not match JWK parameters");
					}
					
					if(this.qi == null) {
						this.qi = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPrivateCrtKey.getCrtCoefficient()));
					}
					else if(!rsaPrivateCrtKey.getCrtCoefficient().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.qi)))) {
						throw new JWKBuildException("Resolved private key does not match JWK parameters");
					}
				}
				
				return Mono.empty();
			});
	}
	
	@Override
	protected Mono<Void> resolveCertificate(X509Certificate certificate) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolveCertificate(certificate)
			.then(Mono.justOrEmpty(certificate)
				.flatMap(tmpCert -> { 
					PublicKey publicKey = tmpCert.getPublicKey();
					if(!(publicKey instanceof RSAPublicKey)) {
						throw new JWKBuildException("Resolved X.509 certificate does not contain a RSA public key");
					}

					if(tmpCert.getSigAlgName() != null) {
						if(this.rsaAlg != null && this.rsaAlg.isSignature() && !this.rsaAlg.getJcaAlgorithm().equals(tmpCert.getSigAlgName())) {
							throw new JWKBuildException("Resolved X.509 certificate algorithm does not match JWK algorithm");
						}
						if((this.rsaAlg == null || this.rsaAlg.isSignature()) && !SUPPORTED_SIG_JCA_ALGORITHMS.contains(tmpCert.getSigAlgName())) {
							throw new JWKBuildException("Resolved X.509 certificate signature algorithm is not supported: " + tmpCert.getSigAlgName());
						}
					}

					RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
					if(this.n == null) {
						this.n = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPublicKey.getModulus()));
					}
					else if(!rsaPublicKey.getModulus().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.n)))) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					if(this.e == null) {
						this.e = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(rsaPublicKey.getPublicExponent()));
					}
					else if(!rsaPublicKey.getPublicExponent().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.e)))) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					return Mono.empty();
				})
			);
	}

	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve().then(Mono.fromRunnable(() -> {
			if(StringUtils.isBlank(this.n)) {
				throw new JWKBuildException("Modulus is blank");
			}
			if(StringUtils.isBlank(this.e)) {
				throw new JWKBuildException("Exponent is blank");
			}
			if(!StringUtils.isAllBlank(this.p, this.q, this.dp, this.dq, this.qi)) {
				if(StringUtils.isBlank(this.d)) {
					throw new JWKBuildException("Private exponent must be specified when optimizations parameters are specified");
				}
				else if(StringUtils.isAnyBlank(this.p, this.q, this.dp, this.dq, this.qi)) {
					throw new JWKBuildException("Optimizations parameters must be all present when one is specified");
				}
			}
		}));
	}

	@Override
	protected Mono<GenericRSAJWK> doBuild() throws JWKBuildException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			GenericRSAJWK jwk = new GenericRSAJWK(this.n, this.e, this.d, (RSAPrivateKey)this.key, this.certificate, (this.key != null && (this.keyTrusted || this.certificate == null)) || (this.key == null && this.certificate != null));
			jwk.setFirstPrimeFactor(this.p);
			jwk.setSecondPrimeFactor(this.q);
			jwk.setFirstFactorExponent(this.dp);
			jwk.setSecondFactorExponent(this.dq);
			jwk.setFirstCoefficient(this.qi);
			jwk.setOtherPrimesInfo(this.oth);
			jwk.setPublicKeyUse(this.use);
			jwk.setKeyOperations(this.key_ops);
			jwk.setAlgorithm(this.alg);
			jwk.setKeyId(this.kid);
			jwk.setX509CertificateURL(this.x5u);
			jwk.setX509CertificateChain(this.x5c);
			jwk.setX509CertificateSHA1Thumbprint(this.x5t);
			jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);

			return jwk;
		});
	}
}

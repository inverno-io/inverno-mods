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

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.SwitchableJWKURLResolver;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import io.inverno.mod.security.jose.jwk.ec.ECJWKBuilder;
import java.math.BigInteger;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Elliptic Curve JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericECJWKBuilder extends AbstractX509JWKBuilder<ECPublicKey, ECPrivateKey, GenericECJWK, GenericECJWKBuilder> implements ECJWKBuilder<GenericECJWK, GenericECJWKBuilder> {

	private static final Set<String> SUPPORTED_SIG_JCA_ALGORITHMS = Arrays.stream(ECAlgorithm.values())
		.filter(ECAlgorithm::isSignature)
		.map(ECAlgorithm::getJcaAlgorithm)
		.collect(Collectors.toSet());
	
	private ECCurve curve;
	private String x;
	private String y;
	private String d;
	
	private ECAlgorithm ecAlg;

	/**
	 * <p>
	 * Creates a generic EC JWK builder.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericECJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		this(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, null);
	}
	
	/**
	 * <p>
	 * Creates a generic EC JWK builder initialized with the specified parameters map.
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
	public GenericECJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, parameters);
	}
	
	@Override
	protected void set(String field, Object value) throws JWKReadException {
		switch(field) {
			case "crv": {
				this.curve((String)value);
				break;
			}
			case "x": {
				this.xCoordinate((String)value);
				break;
			}
			case "y": {
				this.yCoordinate((String)value);
				break;
			}
			case "d": {
				this.eccPrivateKey((String)value);
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}
	
	@Override
	public GenericECJWKBuilder algorithm(String alg) {
		this.ecAlg = alg != null ? ECAlgorithm.fromAlgorithm(alg) : null;
		return super.algorithm(alg);
	}
	
	@Override
	public GenericECJWKBuilder curve(String crv) {
		this.curve = ECCurve.fromCurve(crv);
		return this;
	}

	@Override
	public GenericECJWKBuilder xCoordinate(String x) {
		this.x = x;
		return this;
	}

	@Override
	public GenericECJWKBuilder yCoordinate(String y) {
		this.y = y;
		return this;
	}

	@Override
	public GenericECJWKBuilder eccPrivateKey(String d) {
		this.d = d;
		return this;
	}

	@Override
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		String thumbprint = GenericECJWK.toJWKThumbprint(JWK.DEFAULT_THUMBPRINT_DIGEST, this.curve != null ? this.curve.getCurve() : null, ECJWK.KEY_TYPE, this.x, this.y);
		return super.resolveFromJWKStore().switchIfEmpty(Mono.defer(() -> this.jwkStore.getByJWKThumbprint(thumbprint))).doOnNext(jwk -> {
			if(!(jwk instanceof ECJWK)) {
				throw new JWKResolveException("Stored JWK is not of expected type: " + ECJWK.class);
			}
			ECJWK ecJWK = (ECJWK)jwk;
			if((this.curve != null && !this.curve.getCurve().equals(ecJWK.getCurve())) || 
				(this.x != null && !this.x.equals(ecJWK.getXCoordinate())) || 
				(this.y != null && !this.y.equals(ecJWK.getYCoordinate())) 
			) {
				throw new JWKResolveException("JWK parameters does not match stored JWK");
			}
		});
	}
	
	@Override
	protected Mono<Void> resolveKey(Key key) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.justOrEmpty(key)
			.flatMap(tmpKey -> {
				if(!(tmpKey instanceof ECPrivateKey)) {
					throw new JWKBuildException("Key is not an EC private key");
				}
				ECPrivateKey ecPrivateKey = (ECPrivateKey)tmpKey;
				if(this.curve == null) {
					this.curve = ECCurve.fromCurve("P-" + ecPrivateKey.getParams().getCurve().getField().getFieldSize());
				}
				else if(!ECCurve.fromCurve("P-" + ecPrivateKey.getParams().getCurve().getField().getFieldSize()).equals(this.curve)) {
					throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
				}
				
				if(this.d == null) {
					this.d = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(ecPrivateKey.getS(), this.curve.getKeyLength()));
				}
				else if(!ecPrivateKey.getS().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.d)))) {
					throw new JWKBuildException("Resolved private key does not match JWK parameters");
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
					if(!(publicKey instanceof ECPublicKey)) {
						throw new JWKBuildException("Resolved X.509 certificate does not contain an EC public key");
					}

					if(tmpCert.getSigAlgName() != null) {
						if(this.ecAlg != null && !this.ecAlg.getJcaAlgorithm().equals(tmpCert.getSigAlgName())) {
							throw new JWKBuildException("Resolved X.509 certificate algorithm does not match JWK algoritm");
						}
						if(!SUPPORTED_SIG_JCA_ALGORITHMS.contains(tmpCert.getSigAlgName())) {
							throw new JWKBuildException("Resolved X.509 certificate signature algorithm is not supported: " + tmpCert.getSigAlgName());
						}
					}

					ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
					if(this.curve == null) {
						this.curve = ECCurve.fromCurve("P-" + ecPublicKey.getParams().getCurve().getField().getFieldSize());
					}
					else if(!ECCurve.fromCurve("P-" + ecPublicKey.getParams().getCurve().getField().getFieldSize()).equals(this.curve)) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					if(this.x == null) {
						this.x = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(ecPublicKey.getW().getAffineX(), this.curve.getKeyLength()));
					}
					else if(!ecPublicKey.getW().getAffineX().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.x)))) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					if(this.y == null) {
						this.y = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(ecPublicKey.getW().getAffineY(), this.curve.getKeyLength()));
					}
					else if(!ecPublicKey.getW().getAffineY().equals(new BigInteger(1, Base64.getUrlDecoder().decode(this.y)))) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					return Mono.empty();
				})
			);
	}
	
	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve().then(Mono.fromRunnable(() -> {
			if(this.curve == null) {
				throw new JWKBuildException("Curve is null");
			}
			
			if(this.ecAlg != null && this.ecAlg.getCurve() != null && !this.ecAlg.getCurve().equals(this.curve)) {
				throw new JWKBuildException("Algorithm does not match curve");
			}

			if(StringUtils.isBlank(this.x)) {
				throw new JWKBuildException("X coordinate is blank");
			}

			if(StringUtils.isBlank(this.y)) {
				throw new JWKBuildException("Y coordinate is blank");
			}

			if(!this.curve.isOnCurve(new BigInteger(1, Base64.getUrlDecoder().decode(this.x)), new BigInteger(1, Base64.getUrlDecoder().decode(this.y)))) {
				throw new JWKBuildException("Public x, y coordinates are not on curve " + curve);
			}
		}));
	}

	@Override
	protected Mono<GenericECJWK> doBuild() throws JWKBuildException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			GenericECJWK jwk = new GenericECJWK(this.curve, this.x, this.y, this.d, (ECPrivateKey)this.key, this.certificate, (this.key != null && (this.keyTrusted || this.certificate == null)) || (this.key == null && this.certificate != null));
			jwk.setPublicKeyUse(this.use);
			jwk.setKeyOperations(this.key_ops);
			jwk.setAlgorithm(this.ecAlg);
			jwk.setKeyId(this.kid);
			jwk.setX509CertificateURL(this.x5u);
			jwk.setX509CertificateChain(this.x5c);
			jwk.setX509CertificateSHA1Thumbprint(this.x5t);
			jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);

			return jwk;
		});
	}
}

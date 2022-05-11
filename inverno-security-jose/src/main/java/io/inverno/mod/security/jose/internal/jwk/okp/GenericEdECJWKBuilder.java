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

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import io.inverno.mod.security.jose.jwk.okp.EdECJWKBuilder;
import java.math.BigInteger;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Edward-Curve JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericEdECJWKBuilder extends AbstractOKPJWKBuilder<EdECPublicKey, EdECPrivateKey, GenericEdECJWK, GenericEdECJWKBuilder> implements EdECJWKBuilder<GenericEdECJWK, GenericEdECJWKBuilder> {

	private static final Set<String> SUPPORTED_SIG_JCA_ALGORITHMS = Arrays.stream(EdECAlgorithm.values())
		.filter(EdECAlgorithm::isSignature)
		.map(EdECAlgorithm::getJcaAlgorithm)
		.collect(Collectors.toSet());
	
	private EdECAlgorithm edecAlg;
	
	/**
	 * <p>
	 * Creates a generic EdEC JWK builder.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericEdECJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		super(configuration, jwkStore, keyResolver, urlResolver, certPathValidator);
	}

	/**
	 * <p>
	 * Creates a generic EdEC JWK builder initialized with the specified parameters map.
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
	public GenericEdECJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, parameters);
	}

	@Override
	protected Mono<Void> resolveKey(Key key) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.justOrEmpty(key)
			.flatMap(tmpKey -> {
				if(!(tmpKey instanceof EdECPrivateKey)) {
					throw new JWKBuildException("Key is not an OKP private key");
				}
				EdECPrivateKey edEcPrivateKey = (EdECPrivateKey)tmpKey;
				
				if(this.curve == null) {
					this.curve = OKPCurve.fromCurve(edEcPrivateKey.getParams().getName());
				}
				else if(!OKPCurve.fromCurve(edEcPrivateKey.getParams().getName()).equals(this.curve)) {
					throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
				}
				
				edEcPrivateKey.getBytes().ifPresent(keyBytes -> {
					if(this.d == null) {
						this.d = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(keyBytes);
					}
					else if(!new BigInteger(keyBytes).equals(new BigInteger(Base64.getUrlDecoder().decode(this.d)))) {
						throw new JWKBuildException("Resolved private key does not match JWK parameters");
					}
				});
				return Mono.empty();
			});
	}

	@Override
	protected Mono<Void> resolveCertificate(X509Certificate certificate) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolveCertificate(certificate)
			.then(Mono.justOrEmpty(certificate)
				.flatMap(tmpCert -> {
					PublicKey publicKey = tmpCert.getPublicKey();
					if(!(publicKey instanceof EdECPublicKey)) {
						throw new JWKBuildException("Resolved X.509 certificate does not contain an OKP public key");
					}

					EdECPublicKey edEcPublicKey = (EdECPublicKey)publicKey;

					if(this.curve == null) {
						this.curve = OKPCurve.fromCurve(edEcPublicKey.getParams().getName());
					}
					else if(!OKPCurve.fromCurve(edEcPublicKey.getParams().getName()).equals(this.curve)) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					if(tmpCert.getSigAlgName() != null) {
						if(this.edecAlg != null && !this.edecAlg.getJcaAlgorithm().equals(tmpCert.getSigAlgName())) {
							throw new JWKBuildException("Resolved X.509 certificate algorithm does not match JWK algoritm");
						}
						if(!SUPPORTED_SIG_JCA_ALGORITHMS.contains(tmpCert.getSigAlgName())) {
							throw new JWKBuildException("Resolved X.509 certificate signature algorithm is not supported: " + tmpCert.getSigAlgName());
						}
					}

					byte[] xBytes = new byte[this.curve.getKeyLength()];
					byte[] encodedKeyBytes = edEcPublicKey.getEncoded();
					System.arraycopy(encodedKeyBytes, encodedKeyBytes.length - xBytes.length, xBytes, 0, xBytes.length);

					if(this.x == null) {
						this.x = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(xBytes);
					}
					else if(!new BigInteger(xBytes).equals(new BigInteger(Base64.getUrlDecoder().decode(this.x)))) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					return Mono.empty();
				})
			);
	}
	
	@Override
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		return super.resolveFromJWKStore().doOnNext(jwk -> {
			if(!(jwk instanceof EdECJWK)) {
				throw new JWKReadException("Stored JWK is not of expected type: " + EdECJWK.class);
			}
		});
	}
	
	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve().then(Mono.fromRunnable(() -> {
			if(!GenericEdECJWK.SUPPORTED_CURVES.contains(this.curve)) {
				throw new JWKBuildException("Unsupported OKP curve: " + this.curve.getCurve());
			}
			if(this.alg != null && this.edecAlg == null) {
				this.edecAlg = EdECAlgorithm.fromAlgorithm(this.alg, this.curve);
			}
		}));
	}

	@Override
	protected Mono<GenericEdECJWK> doBuild() throws JWKBuildException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			GenericEdECJWK jwk = new GenericEdECJWK(this.curve, this.x, this.d, (EdECPrivateKey)this.key, this.certificate, (this.key != null && (this.keyTrusted || this.certificate == null)) || (this.key == null && this.certificate != null));
			jwk.setPublicKeyUse(this.use);
			jwk.setKeyOperations(this.key_ops);
			jwk.setAlgorithm(this.edecAlg);
			jwk.setKeyId(this.kid);
			jwk.setX509CertificateURL(this.x5u);
			jwk.setX509CertificateChain(this.x5c);
			jwk.setX509CertificateSHA1Thumbprint(this.x5t);
			jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);
			
			return jwk;
		});
	}
}

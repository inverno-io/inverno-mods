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
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import io.inverno.mod.security.jose.jwk.okp.XECJWKBuilder;
import java.math.BigInteger;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.NamedParameterSpec;
import java.util.Base64;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Extended Elliptic Curve JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericXECJWKBuilder extends AbstractOKPJWKBuilder<XECPublicKey, XECPrivateKey, GenericXECJWK, GenericXECJWKBuilder> implements XECJWKBuilder<GenericXECJWK, GenericXECJWKBuilder> {

	private XECAlgorithm xecAlg;
	
	/**
	 * <p>
	 * Creates a generic XEC JWK builder.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericXECJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		super(configuration, jwkStore, keyResolver, urlResolver, certPathValidator);
	}

	/**
	 * <p>
	 * Creates a generic XEC JWK builder initialized with the specified parameters map.
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
	public GenericXECJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, parameters);
	}

	@Override
	protected Mono<Void> resolveKey(Key key) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.justOrEmpty(key)
			.flatMap(tmpKey -> {
				if(!(tmpKey instanceof XECPrivateKey)) {
					throw new JWKBuildException("Key is not an XEC private key");
				}
				XECPrivateKey xecPrivateKey = (XECPrivateKey)tmpKey;
				
				if(this.curve == null) {
					this.curve = OKPCurve.fromCurve(((NamedParameterSpec)xecPrivateKey.getParams()).getName());
				}
				else if(!OKPCurve.fromCurve(((NamedParameterSpec)xecPrivateKey.getParams()).getName()).equals(this.curve)) {
					throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
				}
				
				xecPrivateKey.getScalar().ifPresent(keyBytes -> {
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
					if(!(publicKey instanceof XECPublicKey)) {
						throw new JWKBuildException("Resolved X.509 certificate does not contain an OKP public key");
					}

					XECPublicKey xecPublicKey = (XECPublicKey)publicKey;

					if(this.curve == null) {
						this.curve = OKPCurve.fromCurve(((NamedParameterSpec)xecPublicKey.getParams()).getName());
					}
					else if(!OKPCurve.fromCurve(((NamedParameterSpec)xecPublicKey.getParams()).getName()).equals(this.curve)) {
						throw new JWKBuildException("Resolved X.509 certificate key does not match JWK parameters");
					}

					byte[] xBytes = new byte[this.curve.getKeyLength()];
					byte[] encodedKeyBytes = xecPublicKey.getEncoded();
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
			if(!(jwk instanceof XECJWK)) {
				throw new JWKReadException("Stored JWK is not of expected type: " + XECJWK.class);
			}
		});
	}

	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve().then(Mono.fromRunnable(() -> {
			if(!GenericXECJWK.SUPPORTED_CURVES.contains(this.curve)) {
				throw new JWKBuildException("Unsupported OKP curve: " + this.curve.getCurve());
			}
			if(this.alg != null && this.xecAlg == null) {
				this.xecAlg = XECAlgorithm.fromAlgorithm(this.alg);
			}
		}));
	}
	
	@Override
	protected Mono<GenericXECJWK> doBuild() throws JWKBuildException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			GenericXECJWK jwk = new GenericXECJWK(this.curve, this.x, this.d, (XECPrivateKey)this.key, this.certificate, (this.key != null && (this.keyTrusted || this.certificate == null)) || (this.key == null && this.certificate != null));
			jwk.setPublicKeyUse(this.use);
			jwk.setKeyOperations(this.key_ops);
			jwk.setAlgorithm(this.xecAlg);
			jwk.setKeyId(this.kid);
			jwk.setX509CertificateURL(this.x5u);
			jwk.setX509CertificateChain(this.x5c);
			jwk.setX509CertificateSHA1Thumbprint(this.x5t);
			jwk.setX509CertificateSHA256Thumbprint(this.x5t_S256);
			
			return jwk;
		});
	}
}

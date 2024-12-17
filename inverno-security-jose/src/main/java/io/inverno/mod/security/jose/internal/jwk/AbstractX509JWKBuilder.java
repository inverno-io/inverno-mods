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
package io.inverno.mod.security.jose.internal.jwk;

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWK;
import io.inverno.mod.security.jose.jwk.X509JWKBuilder;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base X.509 JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the X.509 JWK type
 * @param <D> the X.509 JWK builder type
 */
public abstract class AbstractX509JWKBuilder<A extends PublicKey, B extends PrivateKey, C extends X509JWK<A, B>, D extends AbstractX509JWKBuilder<A, B, C, D>> extends AbstractJWKBuilder<C, D> implements X509JWKBuilder<A, B, C, D> {

	/**
	 * The JWK URL resolver.
	 */
	protected final JWKURLResolver urlResolver;
	
	/**
	 * The X.509 Certificate path validator
	 */
	protected final X509JWKCertPathValidator certPathValidator;
	
	/**
	 * The X.509 URL parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.6">RFC7517 Section 4.6</a>.
	 */
	protected URI x5u;
	
	/**
	 * The X.509 Certificate Chain parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.7">RFC7517 Section 4.7</a>.
	 */
	protected String[] x5c;
	
	/**
	 * The X.509 Certificate SHA-1 Thumbprint parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.8">RFC7517 Section 4.8</a>.
	 */
	protected String x5t;
	
	/**
	 * The X.509 Certificate SHA-256 Thumbprint parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.9">RFC7517 Section 4.9</a>.
	 */
	protected String x5t_S256; // x5t#S256
	
	/**
	 * Indicates whether the built JWK can be trusted.
	 */
	protected volatile boolean keyTrusted;
	
	/**
	 * The underlying certificate.
	 */
	protected volatile X509Certificate certificate;

	/**
	 * <p>
	 * Creates an X.509 JWK builder.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public AbstractX509JWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		this(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, null);
	}
	
	/**
	 * <p>
	 * Creates an X.509 JWK builder initialized with the specified parameters map.
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
	public AbstractX509JWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, parameters);
		this.certPathValidator = certPathValidator;
		this.urlResolver = urlResolver;
	}
	
	/**
	 * <p>
	 * Sets the specified parameter into the builder.
	 * </p>
	 * 
	 * <p>
	 * Unsupported parameters are ignored.
	 * </p>
	 *
	 * @param name  the parameter name
	 * @param value the parameter value
	 *
	 * @throws JWKReadException if there was an error reading the value
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void set(String name, Object value) throws JWKReadException {
		switch(name) {
			case "x5u": {
				if(value instanceof URI) {
					this.x509CertificateURL((URI)value);
				}
				else if(value instanceof URL) {
					try {
						this.x509CertificateURL(((URL)value).toURI());
					} 
					catch(URISyntaxException e) {
						throw new JWKReadException("Invalid x5u which must be a valid URI", e);
					}
				}
				else if(value instanceof CharSequence) {
					this.x509CertificateURL(URI.create((String)value));
				}
				else {
					throw new JWKReadException("x5u can't be converted to URI: " + value.getClass());
				}
				break;
			}
			case "x5c": {
				if(value instanceof String[]) {
					this.x509CertificateChain((String[])value);
				}
				else if(value instanceof Collection) {
					this.x509CertificateChain(((Collection<String>)value).toArray(String[]::new));
				}
				else {
					throw new JWKReadException("x5c can't be converted to String[]: " + value.getClass());
				}
				break;
			}
			case "x5t": {
				this.x509CertificateSHA1Thumbprint((String)value);
				break;
			}
			case "x5t#S256": {
				this.x509CertificateSHA256Thumbprint((String)value);
				break;
			}
			default: {
				super.set(name, value);
				break;
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public D x509CertificateURL(URI x5u) {
		this.x5u = x5u;
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D x509CertificateChain(String[] x5c) {
		this.x5c = x5c;
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D x509CertificateSHA1Thumbprint(String x5t) {
		this.x5t = x5t;
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D x509CertificateSHA256Thumbprint(String x5t_S256) {
		this.x5t_S256 = x5t_S256;
		return (D)this;
	}

	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve()
			.then(this.resolveKid())
			.then(this.resolveX5t_S256())
			.then(this.resolveX5t())
			// Both key and certificate comes from a trusted key store
			.then(Mono.fromRunnable(() -> this.keyTrusted = this.key != null && this.certificate != null))
			.then(this.resolveX5c())
			.then(this.resolveX5u())
			.then();
	}
	
	@Override
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		return super.resolveFromJWKStore()
			.switchIfEmpty(Mono.defer(() -> this.jwkStore.getBy509CertificateSHA1Thumbprint(this.x5t)))
			.switchIfEmpty(Mono.defer(() -> this.jwkStore.getByX509CertificateSHA256Thumbprint(this.x5t_S256)));
	}

	/**
	 * <p>
	 * Tries to resolve the certificate identified by the key id using the JWK key resolver.
	 * </p>
	 * 
	 * <p>
	 * This method queries the key resolver and delegates the implementation specific key and certificate resolution to {@link #resolveKey(java.security.Key) } and
	 * {@link #resolveCertificate(java.security.cert.X509Certificate)} respectively.
	 * </p>
	 * 
	 * @return an empty single publisher which completes in error if the resolved certificate is invalid or inconsistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the certificate
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	private Mono<Void> resolveKid() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return this.keyResolver.resolveCertificateFromKeyId(this.kid)
			.flatMap(tmpCert -> this.resolveCertificate(tmpCert).doOnSuccess(ign -> this.certificate = tmpCert));
	}
	
	/**
	 * <p>
	 * Tries to resolve the private key and certificate identified by the X.509 SHA1 certificate thumbprint using the JWK key resolver.
	 * </p>
	 * 
	 * <p>
	 * This method queries the key resolver and delegates the implementation specific certificate resolution to {@link #resolveCertificate(java.security.cert.X509Certificate)}.
	 * </p>
	 * 
	 * @return an empty single publisher which completes in error if the resolved key or certificate are invalid or inconsistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the certificate
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	private Mono<Void> resolveX5t() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Flux.merge(
				this.keyResolver.resolveKeyFromX509CertificateSHA1Thumbprint(x5t)
					.flatMap(tmpKey -> this.resolveKey(tmpKey)
						.doOnSuccess(ign -> {
							if(this.key != null && !this.key.equals(tmpKey)) {
								throw new JWKBuildException("Resolved private key does not match previously resolved key");
							}
							this.key = tmpKey;
						})
					),
				this.keyResolver.resolveCertificateFromX509CertificateSHA1Thumbprint(this.x5t)
					.flatMap(tmpCert -> this.resolveCertificate(tmpCert).doOnSuccess(ign -> this.certificate = tmpCert))
			)
			.then();
	}
	
	/**
	 * <p>
	 * Tries to resolve the private key and certificate identified by the X.509 SHA256 certificate thumbprint using the JWK key resolver.
	 * </p>
	 * 
	 * <p>
	 * This method queries the key resolver and delegates the implementation specific certificate resolution to {@link #resolveCertificate(java.security.cert.X509Certificate)}.
	 * </p>
	 * 
	 * @return an empty single publisher which completes in error if the resolved key or certificate are invalid or inconsistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the certificate
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	private Mono<Void> resolveX5t_S256() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Flux.merge(
				this.keyResolver.resolveKeyFromX509CertificateSHA1Thumbprint(x5t)
					.flatMap(tmpKey -> this.resolveKey(tmpKey)
						.doOnSuccess(ign -> {
							if(this.key != null && !this.key.equals(tmpKey)) {
								throw new JWKBuildException("Resolved private key does not match previously resolved key");
							}
							this.key = tmpKey;
						})
					),
				this.keyResolver.resolveCertificateFromX509CertificateSHA1Thumbprint(this.x5t)
					.flatMap(tmpCert -> this.resolveCertificate(tmpCert).doOnSuccess(ign -> this.certificate = tmpCert))
			)
			.then();
	}
	
	/**
	 * <p>
	 * Resolves the specified certificate into the builder.
	 * </p>
	 *
	 * <p>
	 * This method basically verifies that the certificate is valid and consistent with the builder's parameters and eventually populates the builder with the certificate key.
	 * </p>
	 * 
	 * @param certificate a certificate
	 * 
	 * @return an empty single publisher which completes in error if the resolved key is invalid or inconsistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the certificate
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	protected Mono<Void> resolveCertificate(X509Certificate certificate) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.justOrEmpty(certificate)
			.flatMap(tmpCert -> { 
				if(tmpCert.getKeyUsage() != null) {
					boolean[] keyUsage = tmpCert.getKeyUsage();
					if(this.use != null) {
						if((this.use.equals(JWK.USE_SIG) && !keyUsage[0]) || (this.use.equals(JWK.USE_ENC) && !(keyUsage[2] || keyUsage[3])) ) {
							throw new JWKBuildException("Resolved X.509 certificate algorithm key usage does not match JWK public key use");
						}
					}
					else {
						if(keyUsage[0]) {
							this.use = JWK.USE_SIG;
						}
						else if(keyUsage[2] || keyUsage[3]) {
							this.use = JWK.USE_ENC;
						}
					}
					if(this.key_ops != null) {
						if( ((this.key_ops.contains(JWK.KEY_OP_ENCRYPT) || this.key_ops.contains(JWK.KEY_OP_DECRYPT)) && !keyUsage[3]) || ((this.key_ops.contains(JWK.KEY_OP_WRAP_KEY) || this.key_ops.contains(JWK.KEY_OP_UNWRAP_KEY)) && !keyUsage[2]) ) {
							throw new JWKBuildException("Resolved X.509 certificate algorithm key usage does not match JWK public key use");
						}
					}
					else {
						if(keyUsage[2]) {
							this.key_ops = Set.of(JWK.KEY_OP_WRAP_KEY, JWK.KEY_OP_UNWRAP_KEY);
						}
						else if(keyUsage[3]) {
							this.key_ops = Set.of(JWK.KEY_OP_ENCRYPT, JWK.KEY_OP_DECRYPT);
						}
					}
				}
				return Mono.empty();
			});
	}
	
	/**
	 * <p>
	 * Tries to resolve the X.509 certificate or certificate chain at the X.509 URL using the JWK URL resolver.
	 * </p>
	 * 
	 * <p>
	 * This method uses the JWK URL resolver to resolve and validate the X.509 certificate or certificates chain at the specified URL and delegates the implementation specific certificate resolution to
	 * {@link #resolveCertificate(java.security.cert.X509Certificate)}.
	 * </p>
	 * 
	 * @return an empty single publisher which completes in error if the resolved certificate or certificates chain is invalid or inconsistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the certificate
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	private Mono<Void> resolveX5u() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return this.urlResolver.resolveX509CertificateURL(this.x5u)
			.filter(certs -> !certs.isEmpty())
			.flatMap(certs -> this.configuration.validate_certificate() ? this.certPathValidator.validate(certs) : Mono.just(certs.getFirst()))
			.flatMap(tmpCert -> this.resolveCertificate(tmpCert).doOnSuccess(ign -> this.certificate = tmpCert));
	}
	
	/**
	 * <p>
	 * Resolves the X.509 certificate or certificates chain specified in the builder using the JWK X.509 certificate path validator.
	 * </p>
	 * 
	 * <p>
	 * This method uses the JWK X.509 certificate path validator to validate the X.509 certificate or certificates chain and delegates the implementation specific certificate resolution to
	 * {@link #resolveCertificate(java.security.cert.X509Certificate)}.
	 * </p>
	 * 
	 * @return an empty single publisher which completes in error if the resolved certificate or certificates chain is invalid or inconsistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the certificate
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	private Mono<Void> resolveX5c() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.justOrEmpty(this.x5c)
			.filter(certs -> certs.length > 0)
			.flatMap(certs -> {
				try {
					CertificateFactory cf =  CertificateFactory.getInstance("X.509");
					List<X509Certificate> certificates = new ArrayList<>();
					for(String encodedCert : certs) {
						certificates.add((X509Certificate)cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(encodedCert))));
					}
					return this.configuration.validate_certificate() ? this.certPathValidator.validate(certificates) : Mono.just(certificates.getFirst());
				}
				catch(CertificateException e) {
					throw new JWKBuildException("Error resolving X.509 certificate chain", e);
				}
			})
			.flatMap(tmpCert -> this.resolveCertificate(tmpCert).doOnSuccess(ign -> this.certificate = tmpCert));
	}
}

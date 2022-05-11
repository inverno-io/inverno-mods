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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JWK key resolver implementation.
 * </p>
 * 
 * <p>
 * This is an overridable bean which can be overriden by injecting a custom {@link JWKKeyResolver} instance when building the JOSE module.
 * </p>
 * 
 * <p>
 * This implementation relies on a {@link KeyStore} to securely load keys and certificates identified by key id, X.509 SHA1 certificate thumbprint or X.509 SHA256 certificate thumbprint.
 * </p>
 * 
 * <p>
 * The key store to use can be specified explicitly or it can loaded from the configuration (see {@link JOSEConfiguration#key_store()}, {@link JOSEConfiguration#key_store_password()} and
 * {@link JOSEConfiguration#key_store_type()}).
 * </p>
 * 
 * <p>
 * Key resolution will be disabled if the key store is missing which happens when no explicit key store has been specified, no key store could be loaded from configuration because of missing
 * parameters or when the optional resource service used to load the configuraiton key store is missing.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Overridable
@Bean( name = "jwkKeyResolver", visibility = Bean.Visibility.PRIVATE )
public class GenericJWKKeyResolver implements @Provide JWKKeyResolver {

	private final JOSEConfiguration configuration;

	private ResourceService resourceService;
	private KeyStore keyStore;
	private char[] password;

	/**
	 * <p>
	 * Creates a generic JWK key resolver.
	 * </p>
	 * 
	 * @param configuration the JOSE module configuration
	 */
	public GenericJWKKeyResolver(JOSEConfiguration configuration) {
		this.configuration = configuration;
	}
	
	/**
	 * <p>
	 * Loads the key store from the configuration if it has not already been set explicitly.
	 * </p>
	 * 
	 * @throws JWKProcessingException if a key store is specified in the configuration and there was an error loading it
	 */
	@Init
	public void init() throws JWKProcessingException {
		if(this.keyStore == null) {
			if(this.configuration.key_store() != null && this.resourceService != null) {
				try (Resource keystoreResource = this.resourceService.getResource(this.configuration.key_store())) {
					this.password = this.configuration.key_store_password() != null ? this.configuration.key_store_password().toCharArray() : new char[0];
					this.keyStore = keystoreResource.openReadableByteChannel()
						.map(channel -> {
							try {
								KeyStore ks = KeyStore.getInstance(this.configuration.key_store_type());
								ks.load(Channels.newInputStream(channel), this.password);

								return ks;
							}
							catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
								throw new JWKProcessingException("Error loading JWK keystore", e);
							}
						})
						.orElseThrow(() -> {
							throw new JWKProcessingException("Error loading JWK keystore, resource does not exist or is not readable: " + this.configuration.key_store());
						});
				}
			}
		}
	}

	/**
	 * <p>
	 * Sets the resource service used to load the key store from the configuration.
	 * </p>
	 * 
	 * @param resourceService a resource service
	 */
	public void setResourceService(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	/**
	 * <p>
	 * Sets the key store.
	 * </p>
	 * 
	 * @param keyStore the key store 
	 * @param password the key store password
	 */
	public void setKeyStore(KeyStore keyStore, char[] password) {
		this.keyStore = keyStore;
		this.password = password;
	}
	
	@Override
	public Mono<? extends Key> resolveKeyFromX509CertificateSHA1Thumbprint(String x5t) throws JWKResolveException {
		return this.resolveKeyFromKeyId(x5t);
	}
	
	@Override
	public Mono<? extends Key> resolveKeyFromX509CertificateSHA256Thumbprint(String x5t_S256) throws JWKResolveException {
		return this.resolveKeyFromKeyId(x5t_S256);
	}
	
	@Override
	public Mono<? extends Key> resolveKeyFromKeyId(String kid) throws JWKResolveException {
		return Mono.justOrEmpty(kid)
			.filter(ign -> this.keyStore != null)
			.mapNotNull(alias -> {
				try {
					return this.keyStore.getKey(alias, this.password);
				} 
				catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
					throw new JWKResolveException("Error accessing keystore", e);
				}
			});
	}
	
	@Override
	public Mono<X509Certificate> resolveCertificateFromX509CertificateSHA1Thumbprint(String x5t) throws JWKResolveException {
		return this.resolveCertificateFromKeyId(x5t);
	}

	@Override
	public Mono<X509Certificate> resolveCertificateFromX509CertificateSHA256Thumbprint(String x5t_S256) throws JWKResolveException {
		return this.resolveCertificateFromKeyId(x5t_S256);
	}

	@Override
	public Mono<X509Certificate> resolveCertificateFromKeyId(String kid) throws JWKResolveException {
		return Mono.justOrEmpty(kid)
			.filter(ign -> this.keyStore != null)
			.mapNotNull(alias -> {
				try {
					Certificate tmpCert = this.keyStore.getCertificate(alias);
					if(tmpCert != null && !(tmpCert instanceof X509Certificate)) {
						throw new JWKResolveException("Certificate is not a X.509 certificate");
					}
					return (X509Certificate)tmpCert;
				} 
				catch (KeyStoreException e) {
					throw new JWKResolveException("Error accessing keystore", e);
				}
			});
	}
}

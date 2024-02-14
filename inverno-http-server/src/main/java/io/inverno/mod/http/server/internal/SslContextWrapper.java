/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.server.internal;

import java.io.IOException;
import java.nio.channels.Channels;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Strategy;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.server.HttpServerConfiguration;
import java.util.Collections;
import javax.net.ssl.TrustManagerFactory;

/**
 * <p>
 * The server SSL context.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE, strategy = Strategy.PROTOTYPE)
@Wrapper
public class SslContextWrapper implements Supplier<SslContext> {
	
	private final HttpServerConfiguration configuration;
	private final ResourceService resourceService;
	
	private CipherSuiteFilter cipherSuiteFilter = SupportedCipherSuiteFilter.INSTANCE;
	
	private SslContext sslContext;

	public SslContextWrapper(HttpServerConfiguration configuration, ResourceService resourceService) {
		this.configuration = configuration;
		this.resourceService = resourceService;
	}
	
	public void setCipherSuiteFilter(CipherSuiteFilter cipherSuiteFilter) {
		this.cipherSuiteFilter = cipherSuiteFilter;
	}
	
	@Init
	public void init() {
		if(this.configuration.tls_key_store() == null) {
			throw new RuntimeException("tls_key_store is missing");
		}
		try (Resource keystoreResource = this.resourceService.getResource(this.configuration.tls_key_store())) {
			keystoreResource.openReadableByteChannel().ifPresentOrElse(
				ksChannel -> {
					try {
						KeyStore ks = KeyStore.getInstance(this.configuration.tls_key_store_type());
						ks.load(Channels.newInputStream(ksChannel), this.configuration.tls_key_store_password().toCharArray());
						
						String keyPassword = this.configuration.tls_key_store_password();
						if(this.configuration.tls_key_alias() != null) {
							if(!ks.containsAlias(this.configuration.tls_key_alias())) {
								throw new IllegalArgumentException("tls_key_store does not contain alias: " + this.configuration.tls_key_alias());
							}
							for (String alias : Collections.list(ks.aliases())) {
								if(!this.configuration.tls_key_alias().equals(alias)) {
									ks.deleteEntry(alias);
								}
							}
							if(this.configuration.tls_key_alias_password() != null) {
								keyPassword = this.configuration.tls_key_alias_password();
							}
						}
						final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
						kmf.init(ks, keyPassword.toCharArray());
	
						SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(kmf);
						SslProvider sslProvider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
						sslContextBuilder.sslProvider(sslProvider);
						
						switch(this.configuration.tls_client_auth()) {
							case REQUESTED: sslContextBuilder.clientAuth(ClientAuth.OPTIONAL);
								break;
							case REQUIRED: sslContextBuilder.clientAuth(ClientAuth.REQUIRE);
								break;
							case NONE:
							default:
						}
						
						if(this.configuration.tls_trust_store() != null) {
							try (Resource trustStoreResource = this.resourceService.getResource(this.configuration.tls_trust_store())) {
								trustStoreResource.openReadableByteChannel().ifPresent(
									tsChannel -> {
									try {
										KeyStore ts = KeyStore.getInstance(this.configuration.tls_trust_store_type());
										ts.load(Channels.newInputStream(tsChannel), this.configuration.tls_trust_store_password().toCharArray());
										
										TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
										tmf.init(ts);
										sslContextBuilder.trustManager(tmf);
									} 
									catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
										throw new RuntimeException("Error initializing SSL context", e);
									}
								});
							}
						}
						else if(this.configuration.tls_trust_manager_factory() != null) {
							sslContextBuilder.trustManager(this.configuration.tls_trust_manager_factory());
						}
						
						if (sslProvider == SslProvider.OPENSSL) {
							sslContextBuilder.ciphers(OpenSsl.availableOpenSslCipherSuites(), this.cipherSuiteFilter);
						}
						else {
							sslContextBuilder.ciphers(OpenSsl.availableOpenSslCipherSuites(), this.cipherSuiteFilter);
							try {
								SSLContext context = SSLContext.getInstance("TLS");
								context.init(null, null, null);
								SSLEngine engine = context.createSSLEngine();
								sslContextBuilder.ciphers(Arrays.asList(engine.getEnabledCipherSuites()));
							} 
							catch (KeyManagementException e) {
								throw new RuntimeException("Error initializing SSL context", e);
							}
						}
						
						if(this.configuration.h2_enabled()) {
							ApplicationProtocolConfig apn = new ApplicationProtocolConfig(
								Protocol.ALPN,
								SelectorFailureBehavior.NO_ADVERTISE,
								SelectedListenerFailureBehavior.ACCEPT,
								ApplicationProtocolNames.HTTP_2, 
								ApplicationProtocolNames.HTTP_1_1
							);
							sslContextBuilder.applicationProtocolConfig(apn);
						}
						
						this.sslContext = sslContextBuilder.build();
					}
					catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
						throw new RuntimeException("Error initializing SSL context", e);
					}
				},
				() -> {
					throw new IllegalStateException("tls_key_store does not exist or is not readable: " + this.configuration.tls_key_store());
				}
			);
		} 
	}

	@Override
	public SslContext get() {
		return this.sslContext;
	}
}

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
package io.inverno.mod.http.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.nio.channels.Channels;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * The SSL context provider.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@Bean(visibility = Visibility.PRIVATE)
public class SslContextProvider {
	
	private static final Logger LOGGER = LogManager.getLogger(SslContextProvider.class);
	
	private CipherSuiteFilter cipherSuiteFilter = SupportedCipherSuiteFilter.INSTANCE;
	
	private final ResourceService resourceService;

	/**
	 * <p>
	 * Creates an SSL context provider.
	 * </p>
	 * 
	 * @param resourceService the resource service
	 */
	public SslContextProvider(ResourceService resourceService) {
		this.resourceService = resourceService;
	}
	
	/**
	 * <p>
	 * Sets the cipher suite filter.
	 * </p>
	 * 
	 * @param cipherSuiteFilter a cipher suite filter
	 */
	public void setCipherSuiteFilter(CipherSuiteFilter cipherSuiteFilter) {
		this.cipherSuiteFilter = cipherSuiteFilter;
	}
	
	/**
	 * <p>
	 * Creates an SSL context.
	 * </p>
	 * 
	 * @param configuration the HTTP client configuration.
	 * 
	 * @return an SSL context
	 */
	public SslContext create(HttpClientConfiguration configuration) {
		// TODO make this non-blocking as this can block
		// TODO Cache keystores and truststores: that's a tough call actually considering these are only created when an endpoint is created
		try {
			SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
			SslProvider sslProvider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
			sslContextBuilder.sslProvider(sslProvider);
		
			if(configuration.tls_key_store() != null) {
				try (Resource keystoreResource = this.resourceService.getResource(configuration.tls_key_store())) {
					keystoreResource.openReadableByteChannel().ifPresentOrElse(
						ksChannel -> {
							try {
								KeyStore ks = KeyStore.getInstance(configuration.tls_key_store_type());
								ks.load(Channels.newInputStream(ksChannel), configuration.tls_key_store_password().toCharArray());

								String keyPassword = configuration.tls_key_store_password();
								if(configuration.tls_key_alias() != null) {
									if(!ks.containsAlias(configuration.tls_key_alias())) {
										throw new IllegalArgumentException("tls_key_store does not contain alias: " + configuration.tls_key_alias());
									}
									for (String alias : Collections.list(ks.aliases())) {
										if(!configuration.tls_key_alias().equals(alias)) {
											ks.deleteEntry(alias);
										}
									}
									if(configuration.tls_key_alias_password() != null) {
										keyPassword = configuration.tls_key_alias_password();
									}
								}
								final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
								kmf.init(ks, keyPassword.toCharArray());
								sslContextBuilder.keyManager(kmf);
							}
							catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
								throw new RuntimeException("Error initializing SSL context", e);
							}
						},
						() -> {
							throw new IllegalStateException("tls_key_store does not exist or is not readable: " + configuration.tls_key_store());
						}
					);
				}
			}
			
			if(configuration.tls_trust_manager_factory() != null) {
				sslContextBuilder.trustManager(configuration.tls_trust_manager_factory());
			}
			else if(configuration.tls_trust_store() != null) {
				try (Resource trustStoreResource = this.resourceService.getResource(configuration.tls_trust_store())) {
					trustStoreResource.openReadableByteChannel().ifPresent(
						tsChannel -> {
						try {
							KeyStore ts = KeyStore.getInstance(configuration.tls_trust_store_type());
							ts.load(Channels.newInputStream(tsChannel), configuration.tls_trust_store_password().toCharArray());

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
			else if(configuration.tls_trust_all()) {
				sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
			}

			if (sslProvider == SslProvider.OPENSSL) {
				sslContextBuilder.ciphers(OpenSsl.availableOpenSslCipherSuites(), this.cipherSuiteFilter);
			}
			else {
				sslContextBuilder.ciphers(OpenSsl.availableOpenSslCipherSuites(), this.cipherSuiteFilter);
				SSLContext context = SSLContext.getInstance("TLS");
				context.init(null, null, null);
				SSLEngine engine = context.createSSLEngine();
				sslContextBuilder.ciphers(Arrays.asList(engine.getEnabledCipherSuites()));
			}
			
			Set<HttpVersion> httpVersions = configuration.http_protocol_versions();
			if(httpVersions == null) {
				httpVersions = HttpClientConfiguration.DEFAULT_HTTP_PROTOCOL_VERSIONS;
			}
			boolean http2 = false;
			List<String> supportedProtocols = new ArrayList<>(2);
			for(HttpVersion version : httpVersions) {
				if(version.isAlpn()) {
					supportedProtocols.add(version.getCode());
					http2 |= version.equals(HttpVersion.HTTP_2_0);
				}
				else {
					LOGGER.warn("ALPN does not support protocol: {}", version.getCode());
				}
			}
			if(http2 || supportedProtocols.size() > 1) {
				ApplicationProtocolConfig apn = new ApplicationProtocolConfig(
					Protocol.ALPN,
					SelectorFailureBehavior.NO_ADVERTISE,
					SelectedListenerFailureBehavior.ACCEPT,
					supportedProtocols
				);
				sslContextBuilder.applicationProtocolConfig(apn);
			}
			
			return sslContextBuilder.build();
		}
		catch(NoSuchAlgorithmException | KeyManagementException| SSLException e) {
			throw new RuntimeException("Error initializing SSL context", e);
		}
	}
}

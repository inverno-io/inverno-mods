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
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * The server SSL context.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE)
public class SslContextProvider {
	
	private static final Logger LOGGER = LogManager.getLogger(SslContextProvider.class);
	
	private CipherSuiteFilter cipherSuiteFilter = SupportedCipherSuiteFilter.INSTANCE;
	
	public void setCipherSuiteFilter(CipherSuiteFilter cipherSuiteFilter) {
		this.cipherSuiteFilter = cipherSuiteFilter;
	}
	
	public SslContext get(HttpClientConfiguration configuration) {
		// TODO make this non-blocking as this can block
		try {
			SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
			SslProvider sslProvider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
			sslContextBuilder.sslProvider(sslProvider);
			
			if(configuration.tls_trust_all()) {
				sslContextBuilder.trustManager(new X509TrustManager() {

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

					}

					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

					}
				});
			}
			else if(configuration.tls_trust_manager_factory() != null) {
				sslContextBuilder.trustManager(configuration.tls_trust_manager_factory());
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

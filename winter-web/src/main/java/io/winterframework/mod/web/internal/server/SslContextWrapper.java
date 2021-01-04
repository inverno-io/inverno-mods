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
package io.winterframework.mod.web.internal.server;

import java.io.FileInputStream;
import java.io.IOException;
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
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Strategy;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.WebConfiguration;
import io.winterframework.core.annotation.Init;
import io.winterframework.core.annotation.Wrapper;

/**
 * 
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE, strategy = Strategy.PROTOTYPE)
@Wrapper
public class SslContextWrapper implements Supplier<SslContext> {

	private WebConfiguration configuration;

	private CipherSuiteFilter cipherSuiteFilter = SupportedCipherSuiteFilter.INSTANCE;
	
	private SslContext sslContext;

	public SslContextWrapper(WebConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public void setCipherSuiteFilter(CipherSuiteFilter cipherSuiteFilter) {
		this.cipherSuiteFilter = cipherSuiteFilter;
	}
	
	@Init
	public void init() {
		try {
			KeyStore ks = KeyStore.getInstance(this.configuration.key_store_type());
			ks.load(new FileInputStream(this.configuration.key_store()),
					this.configuration.key_store_password().toCharArray());
			
			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, this.configuration.key_store_password().toCharArray());

			SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(kmf);
			SslProvider sslProvider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
			sslContextBuilder.sslProvider(sslProvider);

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
			
			ApplicationProtocolConfig apn = new ApplicationProtocolConfig(
				Protocol.ALPN,
				SelectorFailureBehavior.NO_ADVERTISE,
				SelectedListenerFailureBehavior.ACCEPT,
				ApplicationProtocolNames.HTTP_2, 
				ApplicationProtocolNames.HTTP_1_1
			);
			sslContextBuilder.applicationProtocolConfig(apn);
			
			this.sslContext = sslContextBuilder.build();
		} 
		catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new RuntimeException("Error initializing SSL context", e);
		}
	}

	@Override
	public SslContext get() {
		return this.sslContext;
	}
}

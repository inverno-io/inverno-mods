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

package io.inverno.mod.http.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( strategy = Bean.Strategy.PROTOTYPE )
public class GenericHttpClient implements HttpClient {

	private final HttpClientConfiguration configuration;
	private final Reactor reactor;
	private final NetService netService;
	private final SslContextProvider sslContextProvider;
	private final EndpointChannelConfigurer channelConfigurer;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	public GenericHttpClient(
			Reactor reactor,
			NetService netService, 
			SslContextProvider sslContextProvider,
			EndpointChannelConfigurer channelConfigurer,
			HttpClientConfiguration configuration, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter) {
		this.reactor = reactor;
		this.netService = netService;
		this.sslContextProvider = sslContextProvider;
		this.channelConfigurer = channelConfigurer;
		
		this.configuration = configuration;
		
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	@Override
	public EndpointBuilder endpoint(InetSocketAddress remoteAddress) {
		return new GenericEndpointBuilder(remoteAddress);
	}
	
	private class GenericEndpointBuilder implements EndpointBuilder {

		private final InetSocketAddress remoteAddress;

		private InetSocketAddress localAddress;
		private Consumer<HttpClientConfigurationLoader.Configurator> configurer;
		private NetClientConfiguration netConfiguration;
		
		public GenericEndpointBuilder(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
		}
		
		@Override
		public GenericEndpointBuilder localAddress(InetSocketAddress localAddress) {
			this.localAddress = localAddress;
			return this;
		}

		@Override
		public GenericEndpointBuilder configuration(Consumer<HttpClientConfigurationLoader.Configurator> configurer) {
			this.configurer = configurer;
			return this;
		}

		@Override
		public GenericEndpointBuilder netConfiguration(NetClientConfiguration netConfiguration) {
			this.netConfiguration = netConfiguration;
			return this;
		}

		@Override
		public <T extends ExchangeContext> Endpoint<T> build(Class<T> contextType) {
			return new PooledEndpoint<>(
				GenericHttpClient.this.reactor,
				GenericHttpClient.this.netService,
				GenericHttpClient.this.sslContextProvider,
				GenericHttpClient.this.channelConfigurer,
				this.localAddress, 
				this.remoteAddress, 
				HttpClientConfigurationLoader.load(GenericHttpClient.this.configuration, this.configurer), 
				this.netConfiguration,
				contextType, 
				GenericHttpClient.this.headerService, 
				GenericHttpClient.this.parameterConverter
			);
		}
	}
}

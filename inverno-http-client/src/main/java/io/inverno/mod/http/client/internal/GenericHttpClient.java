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
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfiguration;
import java.net.InetSocketAddress;
import io.inverno.mod.http.client.InterceptableExchange;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;

/**
 * <p>
 * The HTTP/1.x and HTTP/2 client.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@Bean( name = "httpClient", strategy = Bean.Strategy.PROTOTYPE )
public class GenericHttpClient implements @Provide HttpClient {

	private final HttpClientConfiguration configuration;
	private final Reactor reactor;
	private final NetService netService;
	private final SslContextProvider sslContextProvider;
	private final EndpointChannelConfigurer channelConfigurer;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	private final MultipartEncoder<Part<?>> multipartBodyEncoder;
	private final Part.Factory partFactory;
	
	/**
	 * <p>
	 * Creates a generic HTTP client.
	 * </p>
	 *
	 * @param reactor               the reactor
	 * @param netService            the net service
	 * @param sslContextProvider    the SSL context provider
	 * @param channelConfigurer     the endpoint channel configurer
	 * @param configuration         the HTTP client configuration
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 */
	public GenericHttpClient(
			Reactor reactor,
			NetService netService, 
			SslContextProvider sslContextProvider,
			EndpointChannelConfigurer channelConfigurer,
			HttpClientConfiguration configuration, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter,
			MultipartEncoder<Parameter> urlEncodedBodyEncoder,
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory) {
		this.reactor = reactor;
		this.netService = netService;
		this.sslContextProvider = sslContextProvider;
		this.channelConfigurer = channelConfigurer;
		
		this.configuration = configuration;
		
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
	}
	
	@Override
	public EndpointBuilder endpoint(InetSocketAddress remoteAddress) {
		return new GenericEndpointBuilder(remoteAddress);
	}
	
	/**
	 * <p>
	 * Generic {@link EndpointBuilder} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A> the exchange context type
	 */
	private class GenericEndpointBuilder<A extends ExchangeContext> implements EndpointBuilder<A, Exchange<A>, InterceptableExchange<A>> {

		private final InetSocketAddress remoteAddress;

		private InetSocketAddress localAddress;
		private HttpClientConfiguration configuration;
		private NetClientConfiguration netConfiguration;
		private ExchangeInterceptor<? super A, InterceptableExchange<A>> exchangeInterceptor;
		
		/**
		 * <p>
		 * Creates a generic endpoint builder.
		 * </p>
		 * 
		 * @param remoteAddress the endpoint remote address
		 */
		public GenericEndpointBuilder(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
		}
		
		@Override
		public GenericEndpointBuilder<A> localAddress(InetSocketAddress localAddress) {
			this.localAddress = localAddress;
			return this;
		}

		@Override
		public GenericEndpointBuilder<A> configuration(HttpClientConfiguration configuration) {
			this.configuration = configuration;
			return this;
		}

		@Override
		public GenericEndpointBuilder<A> netConfiguration(NetClientConfiguration netConfiguration) {
			this.netConfiguration = netConfiguration;
			return this;
		}

		@Override
		public HttpClient.EndpointBuilder<A, Exchange<A>, InterceptableExchange<A>> interceptor(ExchangeInterceptor<? super A, InterceptableExchange<A>> exchangeInterceptor) {
			this.exchangeInterceptor = exchangeInterceptor;
			return this;
		}
		
		@Override
		public Endpoint<A> build() {
			return new PooledEndpoint<>(
				GenericHttpClient.this.reactor,
				GenericHttpClient.this.netService,
				GenericHttpClient.this.sslContextProvider,
				GenericHttpClient.this.channelConfigurer,
				this.localAddress, 
				this.remoteAddress, 
				this.configuration != null ? this.configuration : GenericHttpClient.this.configuration,
				this.netConfiguration,
				GenericHttpClient.this.headerService, 
				GenericHttpClient.this.parameterConverter,
				GenericHttpClient.this.urlEncodedBodyEncoder,
				GenericHttpClient.this.multipartBodyEncoder,
				GenericHttpClient.this.partFactory,
				this.exchangeInterceptor
			);
		}
	}
}

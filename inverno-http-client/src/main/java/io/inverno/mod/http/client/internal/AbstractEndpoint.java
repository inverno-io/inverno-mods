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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.EndpointConnectException;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.InterceptedExchange;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.http1x.Http1xWebSocketConnection;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base {@link Endpoint} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the exchange context type
 */
public abstract class AbstractEndpoint<A extends ExchangeContext> implements Endpoint<A> {
	
	private static final Logger LOGGER = LogManager.getLogger(AbstractEndpoint.class);

	private final InetSocketAddress localAddress;
	private final InetSocketAddress remoteAddress;
	protected final HttpClientConfiguration configuration;

	private final NetService netService;
	private final SslContextProvider sslContextProvider;
	private final EndpointChannelConfigurer channelConfigurer;
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	private final MultipartEncoder<Part<?>> multipartBodyEncoder;
	private final Part.Factory partFactory;
	private final ExchangeInterceptor<A, InterceptedExchange<A>> exchangeInterceptor;
	
	private final Bootstrap bootstrap;

	private Bootstrap webSocketBootstrap;

	/**
	 * <p>
	 * Creates a endpoint targeting the specified remote address.
	 * </p>
	 * 
	 * @param netService            the net service
	 * @param sslContextProvider    the SSL context provider
	 * @param channelConfigurer     the endpoint channel configurer
	 * @param localAddress          the local address
	 * @param remoteAddress         the remote address
	 * @param configuration         the HTTP client configuration
	 * @param netConfiguration      the net configuration
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 * @param exchangeInterceptor   an optional exchange interceptor
	 */
	public AbstractEndpoint(
			NetService netService, 
			SslContextProvider sslContextProvider,
			EndpointChannelConfigurer channelConfigurer,
			InetSocketAddress localAddress,
			InetSocketAddress remoteAddress, 
			HttpClientConfiguration configuration, 
			NetClientConfiguration netConfiguration,
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter,
			MultipartEncoder<Parameter> urlEncodedBodyEncoder,
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory,
			ExchangeInterceptor<A, InterceptedExchange<A>> exchangeInterceptor) {
		this.netService = netService;
		this.sslContextProvider = sslContextProvider;
		this.channelConfigurer = channelConfigurer;
		
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.configuration = configuration;
		
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
		this.exchangeInterceptor = exchangeInterceptor;
		
		if(this.configuration.client_event_loop_group_size() != null) {
			this.bootstrap = this.netService.createClient(this.remoteAddress, netConfiguration, this.configuration.client_event_loop_group_size());
		}
		else {
			this.bootstrap = this.netService.createClient(this.remoteAddress, netConfiguration);
		}
		
		if(this.localAddress != null) {
			this.bootstrap.localAddress(this.localAddress);
		}
		
		if(this.configuration.proxy_host() != null && this.configuration.proxy_password() != null) {
			this.bootstrap.resolver(netService.getResolver());
		}

		this.bootstrap
			.handler(new EndpointChannelInitializer(
				this.sslContextProvider, 
				this.channelConfigurer, 
				this.configuration,
				this.remoteAddress
			));
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public Mono<? extends Exchange<A>> exchange(Method method, String requestTarget, A context) throws IllegalArgumentException {
		if(StringUtils.isBlank(requestTarget)) {
			throw new IllegalArgumentException("Blank request target");
		}
		if(!requestTarget.startsWith("/")) {
			throw new IllegalArgumentException("Request target must be absolute");
		}
		return Mono.fromSupplier(() -> new EndpointExchange<>(
			this.headerService,
			this.parameterConverter,
			this,
			context,
			new EndpointRequest(this.headerService, this.parameterConverter, this.urlEncodedBodyEncoder, this.multipartBodyEncoder, this.partFactory, method, requestTarget)
		));
	}

	/**
	 * <p>
	 * Returns the endpoint's exchange interceptor.
	 * </p>
	 *
	 * @return an exchange interceptor or null
	 */
	public ExchangeInterceptor<A, InterceptedExchange<A>> getExchangeInterceptor() {
		return this.exchangeInterceptor;
	}

	/**
	 * <p>
	 * Obtains an HTTP connection handle.
	 * </p>
	 * 
	 * <p>
	 * Whether a new connection or a pooled connection is returned is implementation specific.
	 * </p>
	 * 
	 * @return a mono emitting an HTTP connection handle
	 */
	public abstract Mono<HttpConnection.Handle> connection();

	/**
	 * <p>
	 * Obtains a WebSocket connection.
	 * </p>
	 *
	 * <p>
	 * This method actually opens a socket to the HTTP server using HTTP/1.1 for the WebSocket protocol handshake.
	 * </p>
	 *
	 * @return a mono emitting a new HTTP connection
	 */
	public Mono<WebSocketConnection> webSocketConnection() {
		return this.createWebSocketConnection();
	}

	/**
	 * <p>
	 * Creates a new WebSocket connection.
	 * </p>
	 * 
	 * <p>
	 * This method actually opens a socket to the HTTP server using HTTP/1.1 for the WebSocket protocol handshake.
	 * </p>
	 * 
	 * @return a mono emitting a new HTTP connection
	 */
	protected Mono<WebSocketConnection> createWebSocketConnection() {
		if(this.webSocketBootstrap == null) {
			HttpClientConfiguration webSocketConfiguration = this.configuration;
			if(this.configuration.tls_enabled()) {
				webSocketConfiguration = HttpClientConfigurationLoader.load(this.configuration, c -> c.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1)));
			}
			this.webSocketBootstrap = this.bootstrap
				.clone()
				.handler(new EndpointWebSocketChannelInitializer(
					this.sslContextProvider, 
					this.channelConfigurer,
					webSocketConfiguration,
					this.remoteAddress
				));
		}
		
		return Mono.defer(() -> {
			Sinks.One<Http1xWebSocketConnection> connectionSink = Sinks.one();
			ChannelFuture connectionFuture = this.webSocketBootstrap.connect(this.remoteAddress);
			connectionFuture.addListener(res -> {
				if(res.isSuccess()) {
					AbstractEndpoint.this.channelConfigurer.completeWsConnection(connectionFuture.channel().pipeline())
						.addListener(connectionActive -> {
							if(connectionActive.isSuccess()) {
								Http1xWebSocketConnection connection = (Http1xWebSocketConnection)connectionActive.getNow();
								LOGGER.info("WebSocket HTTP/1.1 Client ({}) connected to {}", AbstractEndpoint.this.netService.getTransportType().toString().toLowerCase(), (connection.isTls() ? "wss://" : "ws://") + AbstractEndpoint.this.remoteAddress.getHostString() + ":" + AbstractEndpoint.this.remoteAddress.getPort());
								connectionSink.tryEmitValue(connection);
							}
							else {
								connectionSink.tryEmitError(new EndpointConnectException("Failed to connect to " + (AbstractEndpoint.this.configuration.tls_enabled() ? "wss://" : "ws://") + AbstractEndpoint.this.remoteAddress.getHostString() + ":" + AbstractEndpoint.this.remoteAddress.getPort(), res.cause()));
							}
						});
				}
				else {
					connectionSink.tryEmitError(new EndpointConnectException("Failed to connect to " + (this.configuration.tls_enabled() ? "wss://" : "ws://") + this.remoteAddress.getHostString() + ":" + this.remoteAddress.getPort(), res.cause()));
				}
			});
			return connectionSink.asMono();
		});
	}
	
	/**
	 * <p>
	 * Creates a new HTTP connection.
	 * </p>
	 * 
	 * <p>
	 * This method actually opens a socket to the HTTP server.
	 * </p>
	 * 
	 * <p>
	 * The resulting connection can be an HTTP/1.1 or HTTP/2 connection depending on module's configuration and the HTTP server.
	 * </p>
	 * 
	 * @return a mono emitting a new HTTP connection
	 */
	protected Mono<HttpConnection> createConnection() {
		return Mono.defer(() -> {
			Sinks.One<HttpConnection> connectionSink = Sinks.one();
			ChannelFuture connectionFuture = this.bootstrap.connect(this.remoteAddress);
			connectionFuture.addListener(res -> {
				if(res.isSuccess()) {
					AbstractEndpoint.this.channelConfigurer.completeConnection(connectionFuture.channel().pipeline())
						.addListener(connectionActive -> {
							if(connectionActive.isSuccess()) {
								HttpConnection connection = (HttpConnection)connectionActive.getNow();
								LOGGER.info("HTTP/{}.{} Client ({}) connected to {}", connection.getProtocol().getMajor(), connection.getProtocol().getMinor(), AbstractEndpoint.this.netService.getTransportType().toString().toLowerCase(), (connection.isTls() ? "https://" : "http://") + AbstractEndpoint.this.remoteAddress.getHostString() + ":" + AbstractEndpoint.this.remoteAddress.getPort());
								connectionSink.tryEmitValue(connection);
							}
							else {
								connectionSink.tryEmitError(new EndpointConnectException("Failed to connect to " + (this.configuration.tls_enabled() ? "https://" : "http://") + this.remoteAddress.getHostString() + ":" + this.remoteAddress.getPort(), connectionActive.cause()));
							}
						});
				}
				else {
					connectionSink.tryEmitError(new EndpointConnectException("Failed to connect to " + (this.configuration.tls_enabled() ? "https://" : "http://") + this.remoteAddress.getHostString() + ":" + this.remoteAddress.getPort(), res.cause()));
				}
			});
			return connectionSink.asMono();
		});
	}
}

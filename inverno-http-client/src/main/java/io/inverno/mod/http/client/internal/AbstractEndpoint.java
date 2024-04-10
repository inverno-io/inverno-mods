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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.EndpointConnectException;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.internal.http1x.Http1xWebSocketConnection;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import io.inverno.mod.http.client.InterceptableExchange;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import java.net.SocketAddress;

/**
 * <p>
 * Base {@link Endpoint} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
	private final ExchangeInterceptor<? super A, InterceptableExchange<A>> exchangeInterceptor;
	
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
	 * @param configuration         the HTTP client configurartion
	 * @param netConfiguration      the net configuration
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 * @param exchangeInterceptor   an optional exchange intercetptor
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
			ExchangeInterceptor<? super A, InterceptableExchange<A>> exchangeInterceptor) {
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

		this.bootstrap
			.handler(new EndpointChannelInitializer(
				this.sslContextProvider, 
				this.channelConfigurer, 
				this.remoteAddress, 
				this.configuration
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
	public Mono<? extends Exchange<A>> exchange(Method method, String requestTarget, A context) {
		return Mono.fromSupplier(() -> {
			EndpointRequest request = new EndpointRequest(this.headerService, this.parameterConverter, this.urlEncodedBodyEncoder, this.multipartBodyEncoder, this.partFactory, method, requestTarget);
			if(this.configuration.send_user_agent()) {
				request.headers(headers -> headers.set(Headers.NAME_USER_AGENT, this.configuration.user_agent()));
			}
			return new EndpointExchange(this, this.headerService, this.parameterConverter, context, request, this.exchangeInterceptor);
		});
	}
	
	/**
	 * <p>
	 * Obtains an HTTP connection.
	 * </p>
	 * 
	 * <p>
	 * Whether a new connection or a pooled connection is returned is implementation specific.
	 * </p>
	 * 
	 * @return a mono emitting an HTTP connection
	 */
	public abstract Mono<HttpConnection> connection();
	
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
			this.webSocketBootstrap = this.bootstrap
			.handler(new EndpointWebSocketChannelInitializer(
				this.sslContextProvider, 
				this.channelConfigurer, 
				this.remoteAddress, 
				this.configuration
			));
		}
		
		return Mono.defer(() -> {
			Sinks.One<Http1xWebSocketConnection> connectionSink = Sinks.one();
			ChannelFuture connectionFuture = this.bootstrap.connect(this.remoteAddress);
			connectionFuture.addListener(res -> {
				if(res.isSuccess()) {
					// if connection is already in the pipeline we can proceed otherwise we add this in case of protocol nego for instance
					Http1xWebSocketConnection connection = (Http1xWebSocketConnection)connectionFuture.channel().pipeline().get("connection");
					LOGGER.info("WebSocket HTTP/1.1 Client ({}) connected to {}", AbstractEndpoint.this.netService.getTransportType().toString().toLowerCase(), (connection.isTls() ? "wss://" : "ws://") + AbstractEndpoint.this.remoteAddress.getHostString() + ":" + AbstractEndpoint.this.remoteAddress.getPort());
					connectionSink.tryEmitValue(connection);
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
					connectionFuture.channel().pipeline().addLast("connectionSink", new ChannelInboundHandlerAdapter() {
						@Override
						public void channelActive(ChannelHandlerContext ctx) throws Exception {
							super.channelActive(ctx);
							HttpConnection connection = (HttpConnection)ctx.channel().pipeline().get("connection");

							LOGGER.info("HTTP/{}.{} Client ({}) connected to {}", connection.getProtocol().getMajor(), connection.getProtocol().getMinor(), AbstractEndpoint.this.netService.getTransportType().toString().toLowerCase(), (connection.isTls() ? "https://" : "http://") + AbstractEndpoint.this.remoteAddress.getHostString() + ":" + AbstractEndpoint.this.remoteAddress.getPort());
							connectionSink.tryEmitValue(connection);
							ctx.pipeline().remove(this);
						}

						@Override
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
							connectionSink.tryEmitError(new EndpointConnectException("Failed to connect to " + (AbstractEndpoint.this.configuration.tls_enabled() ? "https://" : "http://") + AbstractEndpoint.this.remoteAddress.getHostString() + ":" + AbstractEndpoint.this.remoteAddress.getPort(), cause));
							ctx.pipeline().remove(this);
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

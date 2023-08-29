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
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.EndpointConnectException;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.internal.http1x.Http1xWebSocketConnection;
import io.inverno.mod.http.client.ws.WebSocketExchange;
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
import java.net.SocketAddress;

/**
 * <p>
 * Base {@link Endpoint} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public abstract class AbstractEndpoint implements Endpoint {
	
	private static final Logger LOGGER = LogManager.getLogger(AbstractEndpoint.class);

	private final InetSocketAddress localAddress;
	private final InetSocketAddress remoteAddress;
	protected final HttpClientConfiguration configuration;
	
	private final NetService netService;
	private final SslContextProvider sslContextProvider;
	private final EndpointChannelConfigurer channelConfigurer;
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Bootstrap bootstrap;
	private Bootstrap webSocketBootstrap;
	
	/**
	 * <p>
	 * Creates a endpoint targeting the specified remote address.
	 * </p>
	 *
	 * @param netService         the net service
	 * @param sslContextProvider the SSL context provider
	 * @param channelConfigurer  the endpoint channel configurer
	 * @param localAddress       the local address
	 * @param remoteAddress      the remote address
	 * @param configuration      the HTTP client configurartion
	 * @param netConfiguration   the net configuration
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
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
			ObjectConverter<String> parameterConverter) {
		this.netService = netService;
		this.sslContextProvider = sslContextProvider;
		this.channelConfigurer = channelConfigurer;
		
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.configuration = configuration;
		
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
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
	public <A extends ExchangeContext> Request<A, Exchange<A>, InterceptableExchange<A>> request(Method method, String requestTarget, A context) {
		EndpointRequest<A> request = new EndpointRequest<>(this, this.headerService, this.parameterConverter, method, requestTarget, context);
		if(this.configuration.send_user_agent()) {
			request.headers().set(Headers.NAME_USER_AGENT, this.configuration.user_agent());
		}
		return request;
	}

	@Override
	public <A extends ExchangeContext> Mono<Exchange<A>> send(HttpClient.Request<A, Exchange<A>, InterceptableExchange<A>> request) {
		HttpClientRequest<A> httpClientRequest;
		try {
			httpClientRequest = (HttpClientRequest<A>)request;
		}
		catch(ClassCastException e) {
			throw new IllegalArgumentException("Not an " + HttpClientRequest.class.getCanonicalName());
		}
		
		if(httpClientRequest.interceptor != null) {
			// Create InterceptableExchange, intercept then proceed
			return Mono.defer(() -> {
				GenericInterceptableResponse response = new GenericInterceptableResponse(this.headerService, this.parameterConverter);
				GenericInterceptableExchange<A, HttpClientRequest<A>> interceptableExchange = new GenericInterceptableExchange<>(httpClientRequest.context, httpClientRequest, response);

				return httpClientRequest.interceptor.intercept(interceptableExchange)
					.flatMap(interceptedExchange -> this.connection()
						.flatMap(connection -> connection.send(
							interceptedExchange.context(), 
							interceptedExchange.request().getMethod(), 
							interceptedExchange.request().getAuthority(), 
							interceptedExchange.request().headers().getAll(), 
							interceptedExchange.request().getPath(), 
							httpClientRequest.requestBodyConfigurer,
							interceptableExchange.request().body().map(interceptableBody -> ((GenericInterceptableRequestBody)interceptableBody).getTransformer()).orElse(null),
							interceptableExchange.response().body().getTransformer()
						))
					)
					.doOnNext(interceptableExchange::setExchange)
					.switchIfEmpty(Mono.just(interceptableExchange));
			});
		}
		else {
			return this.connection()
				.flatMap(connection -> connection.send(
					httpClientRequest.context, 
					httpClientRequest.method, 
					httpClientRequest.authority, 
					httpClientRequest.requestHeaders.getAll(), 
					httpClientRequest.path, 
					httpClientRequest.requestBodyConfigurer
				));
		}
	}

	@Override
	public <A extends ExchangeContext> WebSocketRequest<A, WebSocketExchange<A>, InterceptableExchange<A>> webSocketRequest(String requestTarget, A context) {
		EndpointWebSocketRequest<A> request = new EndpointWebSocketRequest<>(this, this.headerService, this.parameterConverter, requestTarget, context);
		if(this.configuration.send_user_agent()) {
			request.headers().set(Headers.NAME_USER_AGENT, this.configuration.user_agent());
		}
		return request;
	}

	@Override
	public <A extends ExchangeContext> Mono<WebSocketExchange<A>> send(HttpClient.WebSocketRequest<A, WebSocketExchange<A>, InterceptableExchange<A>> request) {
		HttpClientWebSocketRequest<A> httpClientWebSocketRequest;
		try {
			httpClientWebSocketRequest = (HttpClientWebSocketRequest<A>)request;
		}
		catch(ClassCastException e) {
			throw new IllegalArgumentException("Not an " + HttpClientWebSocketRequest.class.getCanonicalName());
		}
		
		if(httpClientWebSocketRequest.interceptor != null) {
			GenericInterceptableResponse response = new GenericInterceptableResponse(this.headerService, this.parameterConverter);
			GenericInterceptableExchange<A, HttpClientWebSocketRequest<A>> interceptableExchange = new GenericInterceptableExchange<>(httpClientWebSocketRequest.context, httpClientWebSocketRequest, response);

			httpClientWebSocketRequest.interceptor.intercept(interceptableExchange);

			return httpClientWebSocketRequest.interceptor.intercept(interceptableExchange)
				.flatMap(interceptedExchange -> this.createWebSocketConnection()
					.flatMap(connection -> connection.handshake(
						interceptedExchange.context(), 
						interceptedExchange.request().getAuthority(), 
						interceptedExchange.request().headers().getAll(), 
						interceptedExchange.request().getPath(),
						httpClientWebSocketRequest.getSubProtocol()
					))
				);
		}
		else {
			return this.createWebSocketConnection()
				.flatMap(connection -> connection.handshake(
					httpClientWebSocketRequest.context, 
					httpClientWebSocketRequest.authority, 
					httpClientWebSocketRequest.requestHeaders.getAll(), 
					httpClientWebSocketRequest.path,
					httpClientWebSocketRequest.getSubProtocol()
				));
		}
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
	protected Mono<Http1xWebSocketConnection> createWebSocketConnection() {
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
					// if connection is already in the pipeline we can proceed otherwise we add this in case of protocol nego for instance
					HttpConnection connection = (HttpConnection)connectionFuture.channel().pipeline().get("connection");
					if(connection != null) {
						LOGGER.info("HTTP/{}.{} Client ({}) connected to {}", connection.getProtocol().getMajor(), connection.getProtocol().getMinor(), AbstractEndpoint.this.netService.getTransportType().toString().toLowerCase(), (connection.isTls() ? "https://" : "http://") + AbstractEndpoint.this.remoteAddress.getHostString() + ":" + AbstractEndpoint.this.remoteAddress.getPort());
						connectionSink.tryEmitValue(connection);
					}
					else {
						// Protocol nego
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
				}
				else {
					connectionSink.tryEmitError(new EndpointConnectException("Failed to connect to " + (this.configuration.tls_enabled() ? "https://" : "http://") + this.remoteAddress.getHostString() + ":" + this.remoteAddress.getPort(), res.cause()));
				}
			});
			return connectionSink.asMono();
		});
	}
}

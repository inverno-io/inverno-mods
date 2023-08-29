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
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.PreExchange;
import io.inverno.mod.http.client.PreparedRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.InetSocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public abstract class AbstractEndpoint<A extends ExchangeContext> implements Endpoint<A> {
	
	private static final Logger LOGGER = LogManager.getLogger(AbstractEndpoint.class);

	protected final Class<A> contextType;
	
	private final InetSocketAddress localAddress;
	private final InetSocketAddress remoteAddress;
	protected final HttpClientConfiguration configuration;
	private final NetClientConfiguration netConfiguration;
	
	private final NetService netService;
	private final SslContextProvider sslContextProvider;
	private final EndpointChannelConfigurer channelConfigurer;
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Bootstrap bootstrap;
	
	public AbstractEndpoint(
			NetService netService, 
			SslContextProvider sslContextProvider,
			EndpointChannelConfigurer channelConfigurer,
			InetSocketAddress localAddress,
			InetSocketAddress remoteAddress, 
			HttpClientConfiguration configuration, 
			NetClientConfiguration netConfiguration,
			Class<A> contextType, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter) {
		this.netService = netService;
		this.sslContextProvider = sslContextProvider;
		this.channelConfigurer = channelConfigurer;
		
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.configuration = configuration;
		this.netConfiguration = netConfiguration;
		this.contextType = contextType;
		
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		if(this.configuration.client_event_loop_group_size() != null) {
			this.bootstrap = this.netService.createClient(this.remoteAddress, this.netConfiguration, this.configuration.client_event_loop_group_size());
		}
		else {
			this.bootstrap = this.netService.createClient(this.remoteAddress, this.netConfiguration);
		}
		
		if(this.localAddress != null) {
			this.bootstrap.localAddress(this.localAddress);
		}

		this.bootstrap
			.handler(new EndpointChannelInitializer(this.sslContextProvider, this.channelConfigurer, this.remoteAddress, this.configuration));
	}
	
	@Override
	public PreparedRequest<A, Exchange<A>, PreExchange<A>> request(Method method, String requestTarget) {
		GenericPreparedRequest request = new GenericPreparedRequest(this, this.headerService, this.parameterConverter, method, requestTarget);
		if(this.configuration.send_user_agent()) {
			request.headers().set(Headers.NAME_USER_AGENT, this.configuration.user_agent());
		}
		return request;
	}
	
	/**
	 * take connection from pool
	 * the connection can be HTTP/1.x or HTTP/2
	 * the connection MUST be closed explicitly so it can be returned to the pool closed.
	 * 
	 * Let's forget about the pool for now and create a new connection
	 * - it must be provided by a connectionProvider well it can be by the endpoint itself
	 * - we could create an AbstractEndpoint which create connection and then extends this to create SingleEndpoint (create connection on each request) or PooledEndpoint (stack connection in a pool)
	 * 
	 * @return 
	 */
	public abstract Mono<HttpConnection> connection();
	
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
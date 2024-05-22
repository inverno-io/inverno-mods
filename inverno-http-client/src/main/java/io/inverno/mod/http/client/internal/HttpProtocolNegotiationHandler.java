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

import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.ConnectionTimeoutException;
import io.inverno.mod.http.client.EndpointConnectException;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.Set;

/**
 * <p>
 * HTTP protocol negotiation handler.
 * </p>
 * 
 * <p>
 * This is used to negotiate the protocol (ALPN) when connection is estalished using TLS protocol.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class HttpProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {

	private final EndpointChannelConfigurer channelConfigurer;
	private final HttpClientConfiguration configuration;
	
	private final Set<HttpVersion> supportedProtocols;
	
	/**
	 * <p>
	 * Creates an HTTP protocol negotiation handler.
	 * </p>
	 *
	 * @param channelConfigurer the endpoint channel configurer
	 * @param configuration     the HTTP client configuration
	 */
	public HttpProtocolNegotiationHandler(EndpointChannelConfigurer channelConfigurer, HttpClientConfiguration configuration) {
		super(ApplicationProtocolNames.HTTP_1_1);
		this.channelConfigurer = channelConfigurer;
		this.configuration = configuration;
		
		Set<HttpVersion> protocols = this.configuration.http_protocol_versions();
		this.supportedProtocols = protocols != null ? protocols : HttpClientConfiguration.DEFAULT_HTTP_PROTOCOL_VERSIONS;
	}

	@Override
	protected void handshakeFailure(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.fireExceptionCaught(cause);
		super.handshakeFailure(ctx, cause);
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		try {
			super.userEventTriggered(ctx, evt);
		}
		finally {
			if(evt instanceof IdleStateEvent) {
				this.handshakeFailure(ctx, new ConnectionTimeoutException("Idle timeout: " + ((IdleStateEvent)evt).state()));
			}
		}
	}
	
	@Override
	protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
		ChannelPipeline pipeline = ctx.pipeline();
		if(ApplicationProtocolNames.HTTP_2.equals(protocol) && this.supportedProtocols.contains(HttpVersion.HTTP_2_0)) {
			this.channelConfigurer.configureHttp2(pipeline, this.configuration);
        }
		else if(ApplicationProtocolNames.HTTP_1_1.equals(protocol) && this.supportedProtocols.contains(HttpVersion.HTTP_1_1)) {
			this.channelConfigurer.configureHttp1x(pipeline, HttpVersion.HTTP_1_1, this.configuration);
        }
		else {
			throw new EndpointConnectException("Unsupported protocol: " + protocol);
		}
		ctx.fireChannelActive();
	}
}

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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.http.server.HttpServerException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * <p>
 * HTTP protocol negotiation handler.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class HttpProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {

	private final HttpServerChannelConfigurer channelConfigurer;
	
	/**
	 * <p>
	 * Creates an HTTP protocol negotiation handler.
	 * </p>
	 * 
	 * @param channelConfigurer the channel configurer
	 */
	public HttpProtocolNegotiationHandler(HttpServerChannelConfigurer channelConfigurer) {
		super(ApplicationProtocolNames.HTTP_1_1);
		this.channelConfigurer = channelConfigurer;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		try {
			super.userEventTriggered(ctx, evt);
		}
		finally {
			if(evt instanceof IdleStateEvent) {
				this.handshakeFailure(ctx, new HttpServerException("Idle timeout: " + ((IdleStateEvent)evt).state()));
			}
		}
	}

	@Override
	protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
		ChannelPipeline pipeline = ctx.pipeline();
		if(ApplicationProtocolNames.HTTP_2.equals(protocol)) {
			this.channelConfigurer.configureHttp2(pipeline);
        }
		else if(ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
			this.channelConfigurer.configureHttp1x(pipeline);
        }
		else {
			throw new IllegalStateException("Unsupported protocol: " + protocol);
		}
	}
}

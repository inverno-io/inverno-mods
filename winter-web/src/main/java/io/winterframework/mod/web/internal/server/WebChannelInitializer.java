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

import java.util.function.Supplier;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.core.annotation.Lazy;
import io.winterframework.mod.commons.net.NetService;
import io.winterframework.mod.web.WebConfiguration;
import io.winterframework.mod.web.internal.server.http1x.Http1xChannelHandler;
import io.winterframework.mod.web.internal.server.http1x.Http1xRequestDecoder;
import io.winterframework.mod.web.internal.server.http1x.Http1xResponseEncoder;
import io.winterframework.mod.web.internal.server.http2.H2cUpgradeHandler;
import io.winterframework.mod.web.internal.server.http2.Http2ChannelHandler;

/**
 * 
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
@Sharable
public class WebChannelInitializer extends ChannelInitializer<SocketChannel> {

	private WebConfiguration configuration;
	
	private Supplier<ApplicationProtocolNegotiationHandler> protocolNegociationHandlerSupplier;
	private SslContext sslContext;
	private ByteBufAllocator directAllocator;

	private Supplier<Http1xChannelHandler> http1xChannelHandlerFactory;
	private Supplier<Http2ChannelHandler> http2ChannelHandlerFactory;
	
	public WebChannelInitializer(
		WebConfiguration configuration,
		NetService netService,
		@Lazy Supplier<SslContext> sslContextSupplier, 
		@Lazy Supplier<ApplicationProtocolNegotiationHandler> protocolNegociationHandlerSupplier,
		Supplier<Http1xChannelHandler> http1xChannelHandlerFactory,
		Supplier<Http2ChannelHandler> http2ChannelHandlerFactory) {
		this.configuration = configuration;
		this.directAllocator = netService.getDirectByteBufAllocator();
		this.http1xChannelHandlerFactory = http1xChannelHandlerFactory;
		this.http2ChannelHandlerFactory = http2ChannelHandlerFactory;
		this.protocolNegociationHandlerSupplier = protocolNegociationHandlerSupplier;
		
		if(this.configuration.ssl_enabled()) {
			this.sslContext = sslContextSupplier.get();			
		}
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if(this.configuration.ssl_enabled()) {
			ch.pipeline().addLast(this.sslContext.newHandler(ch.alloc()));
	        ch.pipeline().addLast(this.protocolNegociationHandlerSupplier.get());
		}
		else {
			if(this.configuration.h2c_enabled()) {
				ch.pipeline().addLast(new H2cUpgradeHandler(this.configuration, this.http2ChannelHandlerFactory));
				ch.pipeline().addLast(this.http1xChannelHandlerFactory.get());
			}
			else {
				ch.pipeline().addLast(new Http1xRequestDecoder());
				ch.pipeline().addLast(new Http1xResponseEncoder(this.directAllocator));
				ch.pipeline().addLast(this.http1xChannelHandlerFactory.get());
			}
		}
	}
}

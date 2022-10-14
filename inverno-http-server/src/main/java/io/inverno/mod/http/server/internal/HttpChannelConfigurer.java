/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.core.annotation.Lazy;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.internal.http1x.Http1xChannelHandler;
import io.inverno.mod.http.server.internal.http1x.Http1xRequestDecoder;
import io.inverno.mod.http.server.internal.http1x.Http1xResponseEncoder;
import io.inverno.mod.http.server.internal.http2.H2cUpgradeHandler;
import io.inverno.mod.http.server.internal.http2.Http2ChannelHandler;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.compression.Zstd;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 * A configurer used to configure a channel pipeline for HTTP/1x and/or HTTP/2.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE)
public class HttpChannelConfigurer {

	private final HttpServerConfiguration configuration;
	
	private SslContext sslContext;
	private final ByteBufAllocator allocator;
	private final ByteBufAllocator directAllocator;

	private final Supplier<Http1xChannelHandler> http1xChannelHandlerFactory;
	private final Supplier<Http2ChannelHandler> http2ChannelHandlerFactory;
	
	private final CompressionOptions[] compressionOptions;
	
	/**
	 * <p>
	 * Creates a HTTP channel configurer.
	 * </p>
	 * 
	 * @param configuration                      the HTTP server configuration
	 * @param netService                         the Net service
	 * @param sslContextSupplier                 a SSL context supplier
	 * @param http1xChannelHandlerFactory        a HTTP1.x channel handler factory
	 * @param http2ChannelHandlerFactory         a HTTP/2 channel handler factory
	 */
	public HttpChannelConfigurer(
		HttpServerConfiguration configuration,
		NetService netService,
		@Lazy Supplier<SslContext> sslContextSupplier, 
		Supplier<Http1xChannelHandler> http1xChannelHandlerFactory,
		Supplier<Http2ChannelHandler> http2ChannelHandlerFactory) {
		this.configuration = configuration;
		this.allocator = netService.getByteBufAllocator();
		this.directAllocator = netService.getDirectByteBufAllocator();
		
		this.http1xChannelHandlerFactory = http1xChannelHandlerFactory;
		this.http2ChannelHandlerFactory = http2ChannelHandlerFactory;
		
		if(this.configuration.tls_enabled()) {
			this.sslContext = sslContextSupplier.get();			
		}
		
		List<CompressionOptions> compressionOptionsList = new ArrayList<>();

		compressionOptionsList.add(StandardCompressionOptions.deflate(this.configuration.compression_deflate_compressionLevel(), this.configuration.compression_deflate_windowBits(), this.configuration.compression_deflate_memLevel()));
		compressionOptionsList.add(StandardCompressionOptions.gzip(this.configuration.compression_gzip_compressionLevel(), this.configuration.compression_gzip_windowBits(), this.configuration.compression_gzip_memLevel()));
		if(Zstd.isAvailable()) {
			compressionOptionsList.add(StandardCompressionOptions.zstd(this.configuration.compression_zstd_compressionLevel(), this.configuration.compression_zstd_blockSize(), this.configuration.compression_zstd_maxEncodeSize()));
		}
		
		// Brotli lib is currently an unnamed module so we can't configure it...
		/*if(Brotli.isAvailable()) {
			compressionOptionsList.add(StandardCompressionOptions.brotli(new Encoder.Parameters().setQuality(this.configuration.compression_brotli_quality()).setMode(this.configuration.compression_brotli_mode()).setWindow(this.configuration.compression_brotli_window())));
		}*/
		
		this.compressionOptions = compressionOptionsList.stream().toArray(CompressionOptions[]::new);
	}
	
	/**
	 * <p>
	 * Configures the specified pipeline based on HTTP server configuration.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 */
	public void configure(ChannelPipeline pipeline) {
		if(this.configuration.tls_enabled()) {
			SslHandler sslHandler = this.sslContext.newHandler(this.allocator);
			sslHandler.setHandshakeTimeoutMillis(this.configuration.tls_handshake_timeout());
			pipeline.addLast("sslHandler", sslHandler);
			if(this.configuration.h2_enabled()) {
				pipeline.addLast("protocolNegotiationHandler", new HttpProtocolNegotiationHandler(this));
			}
			else {
				this.configureHttp1x(pipeline);
			}
		}
		else {
			if(this.configuration.h2c_enabled()) {
				this.configureH2C(pipeline);
			}
			else {
				this.configureHttp1x(pipeline);				
			}
		}
	}
	
	/**
	 * <p>
	 * Configures the specified pipeline to handle HTTP/1.x requests.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 * 
	 * @return the HTTP/1.x handler
	 */
	public Http1xChannelHandler configureHttp1x(ChannelPipeline pipeline) {
		this.initHttp1x(pipeline);
		return this.handleHttp1x(pipeline);
	}
	
	/**
	 * <p>
	 * Initializes the specified pipeline with basic handlers required to handle
	 * HTTP/1.x requests.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 */
	private void initHttp1x(ChannelPipeline pipeline) {
		pipeline.addLast("http1xDecoder", new Http1xRequestDecoder());
		pipeline.addLast("http1xEncoder", new Http1xResponseEncoder(this.directAllocator));
		if (this.configuration.decompression_enabled()) {
			pipeline.addLast("http1xDecompressor", new HttpContentDecompressor(false));
		}
		if (this.configuration.compression_enabled()) {
			pipeline.addLast("http1xCompressor", new HttpContentCompressor(this.configuration.compression_contentSizeThreshold(), this.compressionOptions));
		}
	}
	
	/**
	 * <p>
	 * Finalizes HTTP/1.x pipeline configuration by adding the HTTP/1.x handler.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 * 
	 * @return the HTTP/1.x handler
	 */
	private Http1xChannelHandler handleHttp1x(ChannelPipeline pipeline) {
		Http1xChannelHandler handler = this.http1xChannelHandlerFactory.get();
		pipeline.addLast("http1xHandler", handler);
		return handler;
	}
	
	/**
	 * <p>
	 * Configures the specified pipeline to handle HTTP/2 requests.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 * 
	 * @return the HTTP/2 handler
	 */
	public Http2ChannelHandler configureHttp2(ChannelPipeline pipeline) {
		return this.handleHttp2(pipeline);
	}
	
	/**
	 * <p>
	 * Finalizes HTTP/2 pipeline configuration by adding the HTTP/2 handler.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 * 
	 * @return the HTTP/2 handler
	 */
	private Http2ChannelHandler handleHttp2(ChannelPipeline pipeline) {
		Http2ChannelHandler handler = this.http2ChannelHandlerFactory.get();
		pipeline.addLast("http2Handler", handler);
		return handler;
	}
	
	/**
	 * <p>
	 * Configures the specified pipeline to handle HTTP/1.x request and HTTP/2 over cleartext upgrade.
	 * </p>
	 *
	 * @param pipeline the pipeline to configure
	 */
	public void configureH2C(ChannelPipeline pipeline) {
		this.initHttp1x(pipeline);
		pipeline.addLast("h2cUpgradeHandler", new H2cUpgradeHandler(this));
		this.handleHttp1x(pipeline);
	}
	
	/**
	 * <p>
	 * Starts the HTTP/2 upgrade to handle HTTP/2 request over cleartext.
	 * </p>
	 * 
	 * <p>
	 * This basically removes all HTTP/1.x related handlers except the HTTP/1.x decoder which might still be required to decode upgrading request HTTP content.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 * 
	 * @return the HTTP/2 handler
	 */
	public Http2ChannelHandler startHttp2Upgrade(ChannelPipeline pipeline) {
		pipeline.remove("http1xEncoder");
		if (this.configuration.decompression_enabled()) {
			pipeline.remove("http1xDecompressor");
		}
		if (this.configuration.compression_enabled()) {
			pipeline.remove("http1xCompressor");
		}
		pipeline.remove("http1xHandler");
		return this.handleHttp2(pipeline);
	}
	
	/**
	 * <p>
	 * Completes the HTTP/2 upgrade.
	 * </p>
	 * 
	 * <p>
	 * This basically removes HTTP/1.x decoder and H2C upgrade handler.
	 * </p>
	 * 
	 * @param pipeline the pipeline to configure
	 */
	public void completeHttp2Upgrade(ChannelPipeline pipeline) {
		pipeline.remove("http1xDecoder");
		pipeline.remove("h2cUpgradeHandler");
	}
}

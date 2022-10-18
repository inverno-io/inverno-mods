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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.AbstractExchange;
import io.inverno.mod.http.server.internal.http1x.ws.GenericWebSocketFrame;
import io.inverno.mod.http.server.internal.http1x.ws.GenericWebSocketMessage;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * HTTP1.x channel handler implementation.
 * </p>
 * 
 * <p>
 * This is the entry point of a HTTP client connection to the HTTP server using
 * version 1.x of the HTTP protocol.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class Http1xChannelHandler extends ChannelDuplexHandler implements Http1xConnectionEncoder, AbstractExchange.Handler {

	private final HttpServerConfiguration configuration;
	private final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartDecoder<Parameter> urlEncodedBodyDecoder; 
	private final MultipartDecoder<Part> multipartBodyDecoder;
	
	private final GenericWebSocketFrame.GenericFactory webSocketFrameFactory;
	private final GenericWebSocketMessage.GenericFactory webSocketMessageFactory;
	
	private Http1xExchange requestingExchange;
	private Http1xExchange respondingExchange;
	
	private Http1xExchange exchangeQueue;
	
	private boolean read;
	private boolean flush;
	
	/**
	 * <p>
	 * Creates a HTTP1.x channel handler.
	 * </p>
	 *
	 * @param configuration           the server configuration
	 * @param controller              the server controller
	 * @param headerService           the header service
	 * @param parameterConverter      a string object converter
	 * @param urlEncodedBodyDecoder   the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder    the multipart/form-data body decoder
	 * @param webSocketFrameFactory   the WebSocket frame factory
	 * @param webSocketMessageFactory the WebSocket message factory
	 */
	public Http1xChannelHandler(
			HttpServerConfiguration configuration,
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller,
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter,
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			GenericWebSocketFrame.GenericFactory webSocketFrameFactory,
			GenericWebSocketMessage.GenericFactory webSocketMessageFactory) {
		this.configuration = configuration;
		this.controller = controller;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		
		this.webSocketFrameFactory = webSocketFrameFactory;
		this.webSocketMessageFactory = webSocketMessageFactory;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof HttpObject) {
			this.read = true;
			if(msg instanceof HttpRequest) {
				HttpRequest httpRequest = (HttpRequest)msg;
				if(httpRequest.decoderResult().isFailure()) {
					this.onDecoderError(ctx, httpRequest.protocolVersion(), httpRequest);
					return;
				}
				this.requestingExchange = new Http1xExchange(
					this.configuration,
					ctx, 
					httpRequest.protocolVersion(), 
					httpRequest, 
					this,
					this.headerService, 
					this.parameterConverter, 
					this.urlEncodedBodyDecoder, 
					this.multipartBodyDecoder, 
					this.controller,
					this.webSocketFrameFactory,
					this.webSocketMessageFactory
				);
				if(this.exchangeQueue == null) {
					this.exchangeQueue = this.requestingExchange;
					this.requestingExchange.start(this);
				}
				else {
					this.exchangeQueue.next = this.requestingExchange;
					this.exchangeQueue = this.requestingExchange;
				}
			}
			else if(this.requestingExchange != null) {
				HttpVersion version = this.requestingExchange.version;
				if(msg == LastHttpContent.EMPTY_LAST_CONTENT) {
					this.requestingExchange.request().data().ifPresent(sink -> sink.tryEmitComplete());
				}
				else {
					HttpContent httpContent = (HttpContent)msg;
					if(httpContent.decoderResult().isFailure()) {
						this.onDecoderError(ctx, version, httpContent);
						return;
					}
					this.requestingExchange.request().data().ifPresentOrElse(
						sink -> {
							if(sink.tryEmitNext(httpContent.content()) != Sinks.EmitResult.OK) {
								httpContent.release();
							}
						}, 
						() -> httpContent.release()
					);
					if(httpContent instanceof LastHttpContent) {
						this.requestingExchange.request().data().ifPresent(sink -> sink.tryEmitComplete());
					}
				}
			}
			else {
				// This can happen when an exchange has been disposed before we actually
				// received all the data in that case we have to dismiss the content and wait
				// for the next request
				((HttpContent)msg).release();
			}
		}
		else {
			// This is required for WebSocket
			super.channelRead(ctx, msg);
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		if(this.read) {
			this.read = false;
			if(this.flush) {
				ctx.flush();
				this.flush = false;
			}
		}
	}

	private void onDecoderError(ChannelHandlerContext ctx, HttpVersion version, HttpObject httpObject) {
		Throwable cause = httpObject.decoderResult().cause();
		if (cause instanceof TooLongFrameException) {
			String causeMsg = cause.getMessage();
			HttpResponseStatus status;
			if(causeMsg.startsWith("An HTTP line is larger than")) {
				status = HttpResponseStatus.REQUEST_URI_TOO_LONG;
			} 
			else if(causeMsg.startsWith("HTTP header is larger than")) {
				status = HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE;
			} 
			else {
				status = HttpResponseStatus.BAD_REQUEST;
			}
			ChannelPromise writePromise = ctx.newPromise();
			ctx.write(new DefaultFullHttpResponse(version, status), writePromise);
			writePromise.addListener(res -> {
				ctx.fireExceptionCaught(cause);
			});
		} 
		else {
			ctx.fireExceptionCaught(cause);
		}
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		// TODO idle
		ctx.fireUserEventTriggered(evt);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause instanceof WebSocketHandshakeException || cause instanceof CorruptedWebSocketFrameException) {
			// Delegate this to the WebSocket protocol handler
			super.exceptionCaught(ctx, cause);
		}
		else {
			if(this.respondingExchange != null) {
				this.respondingExchange.dispose();
				ChannelPromise errorPromise = ctx.newPromise();
				this.respondingExchange.finalizeExchange(errorPromise, () -> ctx.close());
				errorPromise.tryFailure(cause);
			}
			else {
				ctx.close();
			}
		}
	}
	
	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		ctx.close();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO this created DB connections not returned to pool
		// If the purpose is to clean resources, I think we should see why this happens before the responding exchange response publisher did not finish
		// one explanation could be that response events are not published on the channel event loop: the connection might be closed/end while the onComplete events hasn't been processed
		// In any case this is disturbing and not easy to troubleshoot, we'll see in the future if this is a real issue or not
		/*if(this.respondingExchange != null) {
			this.respondingExchange.dispose();
		}*/
		
		if(this.exchangeQueue != null) {
			this.exchangeQueue.next = null;
		}
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
//		super.channelWritabilityChanged(ctx);
	}
	
	@Override
	public ChannelFuture writeFrame(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		if(this.read) {
			this.flush = true;
			return ctx.write(msg, promise);
		}
		else {
			return ctx.writeAndFlush(msg, promise);
		}
	}
	
	@Override
	public void exchangeStart(ChannelHandlerContext ctx, AbstractExchange exchange) {
		this.respondingExchange = (Http1xExchange)exchange;
	}
	
	@Override
	public void exchangeError(ChannelHandlerContext ctx, Throwable t) {
		// If we get there it means we weren't able to properly handle the error before
		if(this.flush) {
			ctx.flush();
		}
		// We have to release data...
		if(this.respondingExchange.next != null) {
			this.respondingExchange.next.dispose();
		}
		// ...and close the connection
		ctx.close();
	}
	
	@Override
	public void exchangeComplete(ChannelHandlerContext ctx) {
		if(this.respondingExchange.keepAlive) {
			if(this.respondingExchange.next != null) {
				this.respondingExchange.next.start(this);
			}
			else {
				this.exchangeQueue = null;
				this.respondingExchange = null;
			}
		}
		else {
			if(this.respondingExchange.next != null) {
				this.respondingExchange.next.dispose();
			}
			ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
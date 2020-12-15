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
package io.winterframework.mod.web.internal.server.http1x;

import org.reactivestreams.Subscription;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.server.AbstractExchange.ExchangeSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Sinks;

/**
 * @author jkuhn
 *
 */
public class Http1xChannelHandler extends ChannelDuplexHandler {

	private Http1xExchange requestingExchange;
	private Http1xExchange respondingExchange;
	private Sinks.Many<Http1xExchange> exchangeSink;
//	private BaseSubscriber<Http1xExchange> exchangeSubscriber;
	private ChannelExchangeSubscriber exchangeSubscriber;
	
	private ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler;
	private ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler; 
	private HeaderService headerService;
	private RequestBodyDecoder<Parameter> urlEncodedBodyDecoder; 
	private RequestBodyDecoder<Part> multipartBodyDecoder;
	
	public Http1xChannelHandler(
			ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, 
			ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler, 
			HeaderService headerService, 
			RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, 
			RequestBodyDecoder<Part> multipartBodyDecoder) {
		this.rootHandler = rootHandler;
		this.errorHandler = errorHandler;
		this.headerService = headerService;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}
	
/*	private static final byte[] STATIC_PLAINTEXT = "Hello, World!".getBytes(CharsetUtil.UTF_8);
	
	private static final ByteBuf STATIC_PLAINTEXT_BYTEBUF;
	private static final int STATIC_PLAINTEXT_LEN = STATIC_PLAINTEXT.length;

	static {
		ByteBuf tmpBuf = Unpooled.directBuffer(STATIC_PLAINTEXT_LEN);
		tmpBuf.writeBytes(STATIC_PLAINTEXT);
		STATIC_PLAINTEXT_BYTEBUF = Unpooled.unreleasableBuffer(tmpBuf);
	}
	
	private static final CharSequence PLAINTEXT_CLHEADER_VALUE = AsciiString.cached(String.valueOf(STATIC_PLAINTEXT_LEN));

	private static final String LENGTH = String.valueOf(STATIC_PLAINTEXT_LEN);
	
	private static final String TEXT_PLAIN = "text/plain";
	
	@Override
	public void channelRead(ChannelHandlerContext chctx, Object msg) {
		if(msg instanceof HttpRequest) {
			Http1xResponseHeaders headers = new Http1xResponseHeaders(this.headerService);
			headers
				.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
				.add(HttpHeaderNames.CONTENT_LENGTH, PLAINTEXT_CLHEADER_VALUE);
			
			final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, STATIC_PLAINTEXT_BYTEBUF.duplicate(), headers.getHttpHeaders(), EmptyHttpHeaders.INSTANCE);
			
			chctx.write(response, chctx.voidPromise());
		}
	}*/
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.exchangeSubscriber = new ChannelExchangeSubscriber();
		this.exchangeSink = Sinks.many().unicast().onBackpressureBuffer();
		this.exchangeSink.asFlux().subscribe(this.exchangeSubscriber);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//		System.out.println("Channel read");
		if(msg instanceof HttpRequest) {
			HttpRequest httpRequest = (HttpRequest)msg;
			if(httpRequest.decoderResult() != DecoderResult.SUCCESS) {
				this.onDecoderError(ctx, httpRequest);
				return;
			}
			Http1xRequest request = new Http1xRequest(ctx, new Http1xRequestHeaders(ctx, httpRequest, this.headerService), this.urlEncodedBodyDecoder, this.multipartBodyDecoder);
			Http1xResponse response = new Http1xResponse(ctx, this.headerService);
			this.requestingExchange = new Http1xExchange(ctx, this.rootHandler, this.errorHandler, request, response);
			this.exchangeSink.tryEmitNext(this.requestingExchange);
		}
		else if(this.requestingExchange != null) {
			if(msg == LastHttpContent.EMPTY_LAST_CONTENT) {
				this.requestingExchange.request().data().ifPresent(sink -> sink.tryEmitComplete());
			}
			else {
				HttpContent httpContent = (HttpContent)msg;
				if(httpContent.decoderResult() != DecoderResult.SUCCESS) {
					this.onDecoderError(ctx, httpContent);
					return;
				}
				this.requestingExchange.request().data().ifPresentOrElse(emitter -> emitter.tryEmitNext(httpContent.content()), () -> httpContent.release());
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

	private void onDecoderError(ChannelHandlerContext ctx, HttpObject httpObject) {
		DecoderResult result = httpObject.decoderResult();
		Throwable cause = result.cause();
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
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status), writePromise);
			writePromise.addListener(res -> {
				ctx.fireExceptionCaught(cause);
			});
		} 
		else {
			ctx.fireExceptionCaught(cause);
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//		System.out.println("Channel read complete");
		if(this.requestingExchange != null) {
			this.requestingExchange.onReadComplete();
			this.requestingExchange = null;
		}
		ctx.flush();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//		System.out.println("User Event triggered");
		// TODO idle
		ctx.fireUserEventTriggered(evt);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//		System.out.println("Exception caught");
		ctx.close();
	}
	
	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//		System.out.println("close");
		ctx.close();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//		System.out.println("channel inactive");
		this.exchangeSubscriber.dispose();
		this.exchangeSink.tryEmitComplete();
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
//		super.channelWritabilityChanged(ctx);
//		System.out.println("Channel writability changed");
	}
	
	private final class ChannelExchangeSubscriber extends BaseSubscriber<Http1xExchange> implements ExchangeSubscriber {
		
		public void onExchangeComplete() {
			respondingExchange = null;
			this.request(1);
		}
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			this.request(1);
		}
		
		@Override
		protected void hookOnNext(Http1xExchange exchange) {
			respondingExchange = exchange;
			exchange.start(this);
		}
		
		protected void hookFinally(reactor.core.publisher.SignalType type) {
			if(respondingExchange != null) {
				respondingExchange.dispose();
			}
		};
	}
}
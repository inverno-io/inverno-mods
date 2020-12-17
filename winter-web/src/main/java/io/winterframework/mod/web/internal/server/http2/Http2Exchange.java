/**
 * 
 */
package io.winterframework.mod.web.internal.server.http2;

import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.server.AbstractExchange;
import io.winterframework.mod.web.internal.server.AbstractRequest;
import io.winterframework.mod.web.internal.server.AbstractResponse;

/**
 * @author jkuhn
 *
 */
public class Http2Exchange extends AbstractExchange {

	private final Http2Stream stream;
	private final Http2ConnectionEncoder encoder;
	
	private final Consumer<Http2ResponseHeaders> headersConfigurer;

	public Http2Exchange(
			ChannelHandlerContext context, 
			ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, 
			ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler, 
			AbstractRequest request, 
			AbstractResponse response, 
			Http2Stream stream, 
			Http2ConnectionEncoder encoder) {
		super(context, rootHandler, errorHandler, request, response);
		this.stream = stream;
		this.encoder = encoder;
		
		this.headersConfigurer = http2Headers -> {
			this.response.getCookies().getAll().stream()
				.forEach(header -> http2Headers.add(header.getHeaderName(), header.getHeaderValue()));
		};
	}
	
	@Override
	protected void onStart(Subscription subscription) {
		subscription.request(1);
	}
	
	@Override
	protected void onNextMany(ByteBuf value) {
		try {
			if(!this.response.getHeaders().isWritten()) {
				Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.getHeaders();
				this.headersConfigurer.accept(headers);
				this.encoder.writeHeaders(this.context, this.stream.id(), headers.getHttpHeaders(), 0, false, this.context.voidPromise());
				this.response.getHeaders().setWritten(true);
			}
			// TODO implement back pressure with the flow controller
			/*this.encoder.flowController().listener(new Listener() {
				
				@Override
				public void writabilityChanged(Http2Stream stream) {
					
				}
			});*/
			
			ChannelPromise prm = this.context.newPromise();
			prm.addListener(new GenericFutureListener<Future<Void>>() {
				public void operationComplete(Future<Void> future) throws Exception {
					request(1);
				};
			});
			this.encoder.writeData(this.context, this.stream.id(), value, 0, false, prm);
			this.context.channel().flush();
		}
		// TODO errors
		finally {
			this.exchangeSubscriber.exchangeNext(this.context, value);
		}
	}
	
	@Override
	protected void onCompleteWithError(Throwable throwable) {
		// TODO
		// either we have written headers or we have not
		// What kind of error can be sent if we have already sent a 200 OK in the response headers
		
		// The stream can be opened or closed here:
		// - if closed => client side have probably ended the stream (RST_STREAM or close connection)
		// - if not closed => we should send a 5xx error or other based on the exception
		throwable.printStackTrace();
		this.exchangeSubscriber.exchangeError(this.context, throwable);
	}
	
	@Override
	protected void onCompleteEmpty() {
		this.onCompleteMany();
	}
	
	@Override
	protected void onCompleteSingle(ByteBuf value) {
		try {
			if(!this.response.getHeaders().isWritten()) {
				Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.getHeaders();
				this.headersConfigurer.accept(headers);
				this.encoder.writeHeaders(this.context, this.stream.id(), headers.getHttpHeaders(), 0, false, this.context.voidPromise());
				this.response.getHeaders().setWritten(true);
			}
			// TODO implement back pressure with the flow controller
			/*this.encoder.flowController().listener(new Listener() {
				
				@Override
				public void writabilityChanged(Http2Stream stream) {
					
				}
			});*/
			
			ChannelPromise prm = this.context.newPromise();
			prm.addListener(new GenericFutureListener<Future<Void>>() {
				public void operationComplete(Future<Void> future) throws Exception {
					request(1);
				};
			});
			this.encoder.writeData(this.context, this.stream.id(), value, 0, true, prm);
			this.context.channel().flush();
		}
		// TODO errors
		finally {
			this.exchangeSubscriber.exchangeNext(this.context, value);
			this.exchangeSubscriber.exchangeComplete(this.context);
		}
	}
	
	@Override
	protected void onCompleteMany() {
		try {
			if(!this.response.getHeaders().isWritten()) {
				Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.getHeaders();
				this.headersConfigurer.accept(headers);
				this.encoder.writeHeaders(this.context, this.stream.id(), headers.getHttpHeaders(), 0, true, this.context.voidPromise());
				this.response.getHeaders().setWritten(true);
			}
			else {
				this.encoder.writeData(this.context, this.stream.id(), Unpooled.EMPTY_BUFFER, 0, true, this.context.voidPromise());
			}
			this.context.channel().flush();
			this.exchangeSubscriber.exchangeComplete(this.context);
		} 
		catch (Exception e) {
			this.exchangeSubscriber.exchangeError(this.context, e);
		}
	}
}

/**
 * 
 */
package io.winterframework.mod.web.internal.server.http2;

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.server.AbstractExchange;
import io.winterframework.mod.web.internal.server.GenericErrorExchange;

/**
 * @author jkuhn
 *
 */
public class Http2Exchange extends AbstractExchange {

	private final Http2Stream stream;
	private final Http2ConnectionEncoder encoder;
	private final HeaderService headerService;
	
	public Http2Exchange(
			ChannelHandlerContext context, 
			Http2Stream stream, 
			Http2Headers httpHeaders, 
			Http2ConnectionEncoder encoder,
			HeaderService headerService,
			RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, 
			RequestBodyDecoder<Part> multipartBodyDecoder,
			ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, 
			ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler
		) {
		super(context, rootHandler, errorHandler, new Http2Request(context, new Http2RequestHeaders(headerService, httpHeaders), urlEncodedBodyDecoder, multipartBodyDecoder), new Http2Response(context, headerService));
		this.stream = stream;
		this.encoder = encoder;
		this.headerService = headerService;
	}
	
	@Override
	protected ErrorExchange<ResponseBody, Throwable> createErrorExchange(Throwable error) {
		return new GenericErrorExchange(this.request, new Http2Response(this.context, this.headerService), error);
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
				this.encoder.writeHeaders(this.context, this.stream.id(), headers.getInternalHeaders(), 0, false, this.context.voidPromise());
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
			this.handler.exchangeNext(this.context, value);
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
		this.handler.exchangeError(this.context, throwable);
	}
	
	@Override
	protected void onCompleteEmpty() {
		this.onCompleteMany();
	}
	
	@Override
	protected void onCompleteSingle(ByteBuf value) {
		if(!this.response.getHeaders().isWritten()) {
			Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.getHeaders();
			this.encoder.writeHeaders(this.context, this.stream.id(), headers.getInternalHeaders(), 0, false, this.context.voidPromise());
			this.response.getHeaders().setWritten(true);
		}
		Http2ResponseTrailers trailers = (Http2ResponseTrailers)this.response.getTrailers();
		this.encoder.writeData(this.context, this.stream.id(), value, 0, trailers != null, this.context.voidPromise());
		if(trailers != null) {
			this.encoder.writeHeaders(this.context, this.stream.id(), trailers.getInternalTrailers(), 0, true, this.context.voidPromise());	
		}
		this.context.channel().flush();
		this.handler.exchangeNext(this.context, value);
		this.handler.exchangeComplete(this.context);
	}
	
	@Override
	protected void onCompleteMany() {
		if(!this.response.getHeaders().isWritten()) {
			Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.getHeaders();
			this.encoder.writeHeaders(this.context, this.stream.id(), headers.getInternalHeaders(), 0, true, this.context.voidPromise());
			this.response.getHeaders().setWritten(true);
		}
		else {
			this.encoder.writeData(this.context, this.stream.id(), Unpooled.EMPTY_BUFFER, 0, true, this.context.voidPromise());
		}
		this.context.channel().flush();
		this.handler.exchangeComplete(this.context);
	}
}

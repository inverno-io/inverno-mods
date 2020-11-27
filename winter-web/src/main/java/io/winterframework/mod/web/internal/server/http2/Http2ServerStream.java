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
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.server.AbstractHttpServerExchange;
import io.winterframework.mod.web.internal.server.AbstractRequest;
import io.winterframework.mod.web.internal.server.GenericResponse;

/**
 * @author jkuhn
 *
 */
public class Http2ServerStream extends AbstractHttpServerExchange {

	private final Http2Stream stream;
	private final Http2ConnectionEncoder encoder;
	
	private final Consumer<Http2Headers> headersConfigurer;

	public Http2ServerStream(ChannelHandlerContext context, RequestHandler<RequestBody, ResponseBody, Void> rootHandler, RequestHandler<Void, ResponseBody, Throwable> errorHandler, AbstractRequest request, GenericResponse response, Http2Stream stream, Http2ConnectionEncoder encoder) {
		super(context, rootHandler, errorHandler, request, response);
		this.stream = stream;
		this.encoder = encoder;
		
		this.headersConfigurer = http2Headers -> {
			this.response.getHeaders().getAllAsList().stream()
				.forEach(header -> http2Headers.add(header.getHeaderName(), header.getHeaderValue()));
			
			this.response.getCookies().getAll().stream()
				.forEach(header -> http2Headers.add(header.getHeaderName(), header.getHeaderValue()));
		};
	}
	
	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		request(1);
	}
	
	@Override
	protected void hookOnNext(ByteBuf value) {
		if(!this.response.getHeaders().isWritten()) {
			Http2Headers responseHeaders = new DefaultHttp2Headers();
			this.headersConfigurer.accept(responseHeaders);
			this.encoder.writeHeaders(this.context, this.stream.id(), responseHeaders, 0, false, this.context.newPromise());
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
	
	@Override
	protected void hookOnError(Throwable throwable) {
		// TODO
		// either we have written headers or we have not
		// What kind of error can be sent if we have already sent a 200 OK in the response headers
		
		// The stream can be opened or closed here:
		// - if closed => client side have probably ended the stream (RST_STREAM or close connection)
		// - if not closed => we should send a 5xx error or other based on the exception
		throwable.printStackTrace();
	}
	
	@Override
	protected void hookOnComplete() {
		if(!this.response.getHeaders().isWritten()) {
			Http2Headers responseHeaders = new DefaultHttp2Headers();
			this.headersConfigurer.accept(responseHeaders);
			this.encoder.writeHeaders(this.context, this.stream.id(), responseHeaders, 0, true, this.context.newPromise());
			this.response.getHeaders().setWritten(true);
		}
		else {
			this.encoder.writeData(this.context, this.stream.id(), Unpooled.EMPTY_BUFFER, 0, true, this.context.newPromise());
		}
		this.context.channel().flush();
	}
	
}

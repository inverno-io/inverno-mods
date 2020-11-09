/**
 * 
 */
package io.winterframework.mod.web.internal.server.http2;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.winterframework.mod.web.internal.server.AbstractHttpServerExchange;
import io.winterframework.mod.web.internal.server.AbstractRequest;
import io.winterframework.mod.web.internal.server.GenericResponse;

/**
 * @author jkuhn
 *
 */
public class Http2ServerStream<A> extends AbstractHttpServerExchange<A> {

	private Http2Stream stream;
	private Http2ConnectionEncoder encoder;
	
	private Consumer<Http2Headers> headersConfigurer;

	public Http2ServerStream(AbstractRequest<A> request, GenericResponse response, Http2Stream stream, ChannelHandlerContext context, Http2ConnectionEncoder encoder) {
		super(request, response, context);
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
	protected void hookOnNext(ByteBuf value) {
		if(!this.response.getHeaders().isWritten()) {
			Http2Headers responseHeaders = new DefaultHttp2Headers();
			/*this.response.getHeaders().getAll().values().stream()
				.flatMap(List::stream)
				.forEach(header -> responseHeaders.add(header.getHeaderName(), header.getHeaderValue()));*/
			this.headersConfigurer.accept(responseHeaders);
			this.encoder.writeHeaders(this.context, this.stream.id(), responseHeaders, 0, false, this.context.newPromise());
			this.response.getHeaders().setWritten(true);
		}
		this.encoder.writeData(this.context, this.stream.id(), value, 0, false, this.context.newPromise());
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
			/*this.response.getHeaders().getAll().values().stream()
				.flatMap(List::stream)
				.forEach(header -> responseHeaders.add(header.getHeaderName(), header.getHeaderValue()));*/
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

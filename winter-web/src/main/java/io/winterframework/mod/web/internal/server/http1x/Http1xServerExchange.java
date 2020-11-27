/**
 * 
 */
package io.winterframework.mod.web.internal.server.http1x;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.winterframework.mod.commons.resource.MediaTypes;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.Charsets;
import io.winterframework.mod.web.internal.server.AbstractHttpServerExchange;
import io.winterframework.mod.web.internal.server.AbstractRequest;
import io.winterframework.mod.web.internal.server.GenericResponse;
import io.winterframework.mod.web.internal.server.GenericResponseHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xServerExchange extends AbstractHttpServerExchange {

	private boolean manageChunked;
	
	private Consumer<HttpResponse> headersConfigurer;
	
	public Http1xServerExchange(ChannelHandlerContext context, RequestHandler<RequestBody, ResponseBody, Void> rootHandler, RequestHandler<Void, ResponseBody, Throwable> errorHandler, AbstractRequest request, GenericResponse response) {
		super(context, rootHandler, errorHandler, request, response);
		
		this.headersConfigurer = httpResponse -> {
			GenericResponseHeaders headers = this.response.getHeaders();
			
			headers.getAllAsList().stream()
				.forEach(header -> {
					if(header.getHeaderName().equals(Headers.PSEUDO_STATUS)) {
						httpResponse.setStatus(HttpResponseStatus.valueOf(Integer.parseInt(header.getHeaderValue())));
					}
					else {
						httpResponse.headers().add(header.getHeaderName(), header.getHeaderValue());
					}
				});
			
			this.response.getCookies().getAll().stream()
				.forEach(header -> {
					httpResponse.headers().add(header.getHeaderName(), header.getHeaderValue());
				});
		};
	}

	@Override
	protected void hookOnNext(ByteBuf value) {
		try {
			GenericResponseHeaders headers = this.response.getHeaders();
			if(!headers.isWritten()) {
				Set<String> transferEncodings = headers.<Header>getAll(Headers.TRANSFER_ENCODING).stream().map(Header::getHeaderValue).collect(Collectors.toSet());
				if(headers.getSize() == null && !transferEncodings.contains("chunked")) {
					headers.add(Headers.TRANSFER_ENCODING, "chunked");
					this.manageChunked = headers.getContentType() != null && !headers.getContentType().getMediaType().equals(MediaTypes.TEXT_EVENT_STREAM);
				}
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				this.headersConfigurer.accept(response);
				this.context.write(response, this.context.newPromise());
				headers.setWritten(true);
			}
			if(this.manageChunked) {
				// We must handle chunked transfer encoding
				ByteBuf chunked_header = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(Integer.toHexString(value.readableBytes()) + "\r\n", Charsets.orDefault(headers.getContentType().getCharset())));
				ByteBuf chunked_trailer = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("\r\n", Charsets.orDefault(headers.getContentType().getCharset())));
				
				this.context.write(new DefaultHttpContent(Unpooled.wrappedBuffer(chunked_header, value, chunked_trailer)), this.context.newPromise());
			}
			else {
				this.context.write(new DefaultHttpContent(value), this.context.newPromise());
			}
			this.context.channel().flush();
		}
		catch(Exception e) {
			// TODO handle exception properly
			e.printStackTrace();
		}
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
		try {
			GenericResponseHeaders headers = this.response.getHeaders();
			if(!headers.isWritten()) {
				HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				this.headersConfigurer.accept(response);
				this.context.write(response, this.context.newPromise());
				headers.setWritten(true);
			}
			else {
				this.context.write(LastHttpContent.EMPTY_LAST_CONTENT, this.context.newPromise());
			}
			this.context.channel().flush();
		}
		catch(Exception e) {
			// TODO handle exception properly
			e.printStackTrace();
		}
	}
}

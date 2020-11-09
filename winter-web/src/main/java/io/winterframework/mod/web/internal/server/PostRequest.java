/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.net.SocketAddress;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHeaders;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * @author jkuhn
 *
 */
public class PostRequest extends AbstractRequest<RequestBody> {

	private GenericRequestBody requestBody;
	
	private FluxSink<ByteBuf> data;
	
	public PostRequest(SocketAddress remoteAddress, RequestHeaders requestHeaders, GenericRequestParameters requestParameters, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder, boolean releaseData) {
		super(remoteAddress, requestHeaders, requestParameters);
		
		Flux<ByteBuf> requestBodyData = Flux.create(emitter -> {
			this.data = emitter;
		});
		
		if(releaseData) {
			requestBodyData = requestBodyData.flatMap(chunk -> {
				return Flux.just(chunk).doFinally(sgn -> {
					chunk.release();
				});
			});
		}
		
		this.requestBody = new GenericRequestBody(
			requestHeaders.<Headers.ContentType>get(Headers.CONTENT_TYPE),
			urlEncodedBodyDecoder, 
			multipartBodyDecoder, 
			requestBodyData
		);
	}

	@Override
	public GenericRequestBody body() {
		return this.requestBody;
	}

	@Override
	public Optional<FluxSink<ByteBuf>> data() {
		return Optional.ofNullable(this.data);
	}
}

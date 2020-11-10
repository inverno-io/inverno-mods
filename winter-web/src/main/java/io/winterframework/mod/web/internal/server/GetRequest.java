/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.net.SocketAddress;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHeaders;
import reactor.core.publisher.FluxSink;

/**
 * @author jkuhn
 *
 */
public class GetRequest extends AbstractRequest {

	public GetRequest(SocketAddress remoteAddress, RequestHeaders requestHeaders, GenericRequestParameters requestParameters) {
		super(remoteAddress, requestHeaders, requestParameters);
	}

	@Override
	public Optional<RequestBody> body() {
		return Optional.empty();
	}

	@Override
	public Optional<FluxSink<ByteBuf>> data() {
		return Optional.empty();
	}

}

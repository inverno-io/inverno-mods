/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.net.SocketAddress;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.RequestHeaders;
import reactor.core.publisher.FluxSink;

/**
 * @author jkuhn
 *
 */
public class GetRequest extends AbstractRequest<Void> {

	public GetRequest(SocketAddress remoteAddress, RequestHeaders requestHeaders, GenericRequestParameters requestParameters) {
		super(remoteAddress, requestHeaders, requestParameters);
	}

	@Override
	public Void body() {
		return null;
	}

	@Override
	public Optional<FluxSink<ByteBuf>> data() {
		return Optional.empty();
	}

}

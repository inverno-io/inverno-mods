/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.net.SocketAddress;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestCookies;
import io.winterframework.mod.web.RequestHeaders;
import io.winterframework.mod.web.RequestParameters;
import reactor.core.publisher.FluxSink;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractRequest implements Request<RequestBody, Void> {

	private SocketAddress remoteAddress;
	private RequestHeaders requestHeaders;
	private GenericRequestParameters requestParameters;
	private GenericRequestCookies requestCookies;
	
	public AbstractRequest(SocketAddress remoteAddress, RequestHeaders requestHeaders, GenericRequestParameters requestParameters) {
		this.remoteAddress = remoteAddress;
		this.requestHeaders = requestHeaders;
		this.requestParameters = requestParameters;
		this.requestCookies = new GenericRequestCookies(requestHeaders);
	}
	
	@Override
	public RequestHeaders headers() {
		return this.requestHeaders;
	}

	@Override
	public RequestParameters parameters() {
		return this.requestParameters;
	}

	@Override
	public RequestCookies cookies() {
		return this.requestCookies;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public Void context() {
		return null;
	}
	
	public abstract Optional<FluxSink<ByteBuf>> data();
}

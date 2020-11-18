/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public interface HttpServerExchange {
	
	ChannelHandlerContext getContext();

	RequestHandler<RequestBody, Void, ResponseBody> getHandler();

	void setHandler(RequestHandler<RequestBody, Void, ResponseBody> handler);

	Request<RequestBody, Void> request();
	
	Response<ResponseBody> response();
	
	Mono<Void> init();
	
	void dispose();
	
	boolean isDisposed();
}
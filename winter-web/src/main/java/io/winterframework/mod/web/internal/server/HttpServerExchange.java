/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public interface HttpServerExchange extends Exchange<RequestBody, ResponseBody> {
	
	ChannelHandlerContext getContext();

	ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> getRootHandler();
	
	ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> getErrorHandler();

	Mono<Void> init();
	
	void dispose();
	
	boolean isDisposed();
}

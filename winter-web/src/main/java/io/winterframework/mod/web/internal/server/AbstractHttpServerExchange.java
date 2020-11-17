/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractHttpServerExchange extends BaseSubscriber<ByteBuf> implements HttpServerExchange {

	protected AbstractRequest request;
	protected GenericResponse response;
	protected ChannelHandlerContext context;
	
	protected RequestHandler<RequestBody, Void, ResponseBody> handler;
	
	// TODO this can definitely be used for stats
	private Mono<Void> exchangeMono;
	
	public AbstractHttpServerExchange(AbstractRequest request, GenericResponse response, ChannelHandlerContext context) {
		this.request = request;
		this.response = response;
		this.context = context;
	}
	
	@Override
	public ChannelHandlerContext getContext() {
		return this.context;
	}

	@Override
	public RequestHandler<RequestBody, Void, ResponseBody> getHandler() {
		return this.handler;
	}

	@Override
	public void setHandler(RequestHandler<RequestBody, Void, ResponseBody> handler) {
		this.handler = handler;
	}

	@Override
	public AbstractRequest request() {
		return this.request;
	}

	@Override
	public GenericResponse response() {
		return this.response;
	}

	@Override
	public Mono<Void> init() {
		if(this.exchangeMono != null) {
			throw new IllegalStateException("Exchange already started");
		}
		
		this.exchangeMono = Mono.create(emitter -> {
			Mono.just(this.response)
				.map(response -> {
					RequestHandler<RequestBody, Void, ResponseBody> handler = this.handler;
					if(handler == null) {
						// There's no route to handle the request => 404
						// We should decide what to do: the handler might not throw exception now but in
						// a later stage when consuming data flux or rendering the response so we might
						// need to handle all these in a common way therefore we might prefer bubbling
						// an error in the response:
						// 1. handler.handle() throw an exception
						//   - we do not subscribe to the response data flux so we can use a white label handler (using a new response? what about request data flux?
						// 2. response data flux is on error
						//   - if response headers were not sent yet, we can still do the same thing: replace the response with something else
						//   - if response headers were already sent, we need to see what http plans for this => RESET
						handler = (req, resp) -> {
							resp.headers(headers -> headers.status(404)).body().empty();
						};
					}
					handler.handle(this.request, response);
					
					return response;
				})
				// apparently this seems to work after all 
				.flatMapMany(response -> response.data()/*.publishOn(Schedulers.fromExecutor(this.context.channel().eventLoop()))*/)
				.doFinally(sgn -> emitter.success())
				//.subscribe(this.responseDataSubscriber);
				.subscribe(this);
		});
		
		return this.exchangeMono;
	}

	@Override
	public void dispose() {
		// TODO what happens when the exchangeMono is subscribed to after cancel has been called?
		if(this.exchangeMono != null && !this.isDisposed()) {
			super.dispose();
		}
	}

	@Override
	public boolean isDisposed() {
		return this.exchangeMono != null && super.isDisposed();
	}

}

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
import reactor.core.scheduler.Schedulers;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractHttpServerExchange extends BaseSubscriber<ByteBuf> implements HttpServerExchange {

	protected final ChannelHandlerContext context;
	protected final RequestHandler<RequestBody, ResponseBody, Void> rootHandler;
	protected final RequestHandler<Void, ResponseBody, Throwable> errorHandler;
	
	protected final AbstractRequest request;
	protected final GenericResponse response;
	
	// TODO this can definitely be used for stats
	private Mono<Void> exchangeMono;
	
	public AbstractHttpServerExchange(ChannelHandlerContext context, RequestHandler<RequestBody, ResponseBody, Void> rootHandler, RequestHandler<Void, ResponseBody, Throwable> errorHandler, AbstractRequest request, GenericResponse response) {
		this.context = context;
		this.rootHandler = rootHandler;
		this.errorHandler = errorHandler;
		this.request = request;
		this.response = response;
	}
	
	@Override
	public ChannelHandlerContext getContext() {
		return this.context;
	}

	@Override
	public RequestHandler<RequestBody, ResponseBody, Void> getRootHandler() {
		return this.rootHandler;
	}
	
	@Override
	public RequestHandler<Void, ResponseBody, Throwable> getErrorHandler() {
		return this.errorHandler;
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
					// TODO There's no route to handle the request => 404
					// We should decide what to do: the handler might not throw exception now but in
					// a later stage when consuming data flux or rendering the response so we might
					// need to handle all these in a common way therefore we might prefer bubbling
					// an error in the response:
					// 1. handler.handle() throw an exception
					//   - we do not subscribe to the response data flux so we can use a white label handler (using a new response? what about request data flux?
					// 2. response data flux is on error
					//   - if response headers were not sent yet, we can still do the same thing: replace the response with something else
					//   - if response headers were already sent, we need to see what http plans for this => RESET
					
					try {
						this.rootHandler.handle(this.request, response);
					}
					catch(Throwable t) {
						// We could also set the flux on error and call the error handler
						this.errorHandler.handle(this.request.map(body -> null, ign -> t), response);
					}
					
					return response;
				})
				// apparently this seems to work after all 
				.flatMapMany(response -> response.data().publishOn(Schedulers.fromExecutor(this.context.channel().eventLoop())))
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

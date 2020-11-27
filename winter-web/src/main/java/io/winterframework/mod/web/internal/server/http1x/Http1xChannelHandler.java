/**
 * 
 */
package io.winterframework.mod.web.internal.server.http1x;

import java.util.function.Supplier;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Strategy;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.core.annotation.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
@Bean(strategy = Strategy.PROTOTYPE, visibility = Visibility.PRIVATE)
public class Http1xChannelHandler extends ChannelDuplexHandler {

	private Supplier<Http1xServerExchangeBuilder> http1xServerExchangeBuilderSupplier;
	
	private Http1xServerExchange currentServerExchange;
	private FluxSink<Mono<Http1xServerExchange>> serverExchangeSink;
	
	public Http1xChannelHandler(@Lazy Supplier<Http1xServerExchangeBuilder> http1xServerExchangeBuilderSupplier) {
		this.http1xServerExchangeBuilderSupplier = http1xServerExchangeBuilderSupplier;
		
		Flux.<Mono<Http1xServerExchange>>create(emitter -> {
			this.serverExchangeSink = emitter;
		})
		.concatMap(serverExchangeMono -> {
			return serverExchangeMono.flatMap(serverExchange -> {
				return serverExchange.init()
					.doOnSubscribe(subscription -> {
						this.currentServerExchange = serverExchange;
					})
					.doFinally(sgn -> {
						if(!serverExchange.getContext().channel().config().isAutoRead()) {
							serverExchange.getContext().channel().config().setAutoRead(true);
						}
						this.currentServerExchange = null;
					});
			});
		})
		.subscribe();
	}
	
//	private List<Object> messages = new LinkedList<>();
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//		System.out.println("Channel read");
		
//		this.messages.add(msg);
		
		if(msg instanceof HttpRequest) {
			this.serverExchangeSink.next(this.http1xServerExchangeBuilderSupplier.get()
				.request((HttpRequest)msg)
				.build(ctx)
			);
		}
		else if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {
			if(this.currentServerExchange != null) {
				this.currentServerExchange.request().data().ifPresent(emitter -> emitter.complete());
				if(this.currentServerExchange != null && !this.currentServerExchange.isDisposed()) {
					// Disable the auto read if there's a pending request
					// TODO This disables the auto read if there's a pending request but we might want to buffer data or do some proper back pressure (drop requests...)
					ctx.channel().config().setAutoRead(false);
				}
			}
		}
		else if (msg instanceof HttpContent) {
			HttpContent content = (HttpContent)msg;
			if(this.currentServerExchange == null) {
				// This can happen when an exchange has been disposed before we actually
				// received all the data in that case we have to dismiss the content and wait
				// for another request
				content.release();
			}
			else {
				this.currentServerExchange.request().data().ifPresentOrElse(emitter -> emitter.next(content.content()), () -> content.release());
				if(this.currentServerExchange != null && msg instanceof LastHttpContent) {
					this.currentServerExchange.request().data().ifPresent(emitter -> emitter.complete());
					if(this.currentServerExchange != null && !this.currentServerExchange.isDisposed()) {
						// TODO This disables the auto read if there's a pending request but we might want to buffer data or do some proper backpressure (drop requests...)
						ctx.channel().config().setAutoRead(false);
					}
				}
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		super.channelReadComplete(ctx);
//		System.out.println("Channel read complete");
		ctx.flush();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
//		System.out.println("User Event triggered");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
//		System.out.println("Exception caught");
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
//		System.out.println("Channel inactive");
		this.serverExchangeSink.complete();
		if(this.currentServerExchange != null && !this.currentServerExchange.isDisposed()) {
			this.currentServerExchange.dispose();
		}
		
		/*StringBuilder report = new StringBuilder();

		int requestId = 0;
		int requestContents = 0;
		int requestUnreleased = 0;
		boolean requestHasLast = false;
		for(Object msg : this.messages) {
			if(msg instanceof HttpRequest) {
				if(requestId > 0 && requestUnreleased > 0) {
					report.append("  request: " + requestId + "\n");
					report.append("    contents: " + requestContents + "\n");
					report.append("    unreleased: " + requestUnreleased + "\n");
					report.append("    hasLast: " + requestHasLast + "\n");
				}
				requestId++;
				requestContents = 0;
				requestUnreleased = 0;
				requestHasLast = false;
			}
			else if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {
				requestHasLast = true;
			}
			else if (msg instanceof HttpContent) {
				requestContents++;
				ByteBuf content = ((HttpContent)msg).content();
				if(content.refCnt() > 0) {
					content.release();
					requestUnreleased++;
				}
			}
		}
		
		if(requestUnreleased > 0) {
			report.append("  request: " + requestId + "\n");
			report.append("    contents: " + requestContents + "\n");
			report.append("    unreleased: " + requestUnreleased + "\n");
			report.append("    hasLast: " + requestHasLast + "\n");
		}
		System.out.println("requests: " + requestId + "\n" +report);*/
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		super.channelWritabilityChanged(ctx);
//		System.out.println("Channel writability changed");
	}

}
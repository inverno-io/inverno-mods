/*
 * Copyright 2022 Jeremy KUHN
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public abstract class AbstractExchange<A extends AbstractRequest, B extends AbstractResponse, C extends AbstractExchange<A, B, C>> implements Exchange<ExchangeContext> {

	protected final ChannelHandlerContext context;
	protected final EventLoop eventLoop;
	private final MonoSink<Exchange<ExchangeContext>> exchangeSink;
	private final ExchangeContext exchangeContext;
	protected final A request;
	protected final Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer;
	
	protected B response;
	protected AbstractExchange.Handler handler;
	protected Disposable disposable;

	public AbstractExchange(ChannelHandlerContext context, MonoSink<Exchange<ExchangeContext>> exchangeSink, ExchangeContext exchangeContext, A request, Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer) {
		this.context = context;
		this.eventLoop = this.context.channel().eventLoop();
		this.exchangeSink = exchangeSink;
		this.exchangeContext = exchangeContext;
		this.request = request;
		this.responseBodyTransformer = responseBodyTransformer;
	}
	
	public ChannelHandlerContext getChannelContext() {
		return this.context;
	}
	
	protected Future<?> executeInEventLoop(Runnable runnable) {
		if(this.eventLoop.inEventLoop()) {
			try {
				runnable.run();
				return this.eventLoop.newSucceededFuture(null);
			}
			catch(Throwable e) {
				return this.eventLoop.newFailedFuture(e);
			}
		}
		else {
			return this.eventLoop.submit(runnable);
		}
	}
	
	protected <T> Future<T> executeInEventLoop(Callable<T> callable) {
		if(this.eventLoop.inEventLoop()) {
			try {
				return this.eventLoop.newSucceededFuture(callable.call());
			}
			catch(Throwable e) {
				return this.eventLoop.newFailedFuture(e);
			}
		}
		else {
			return this.eventLoop.submit(callable);
		}
	}
	
	@Override
	public A request() {
		return request;
	}
	
	@Override
	public B response() {
		return this.response;
	}

	public final void setResponse(B response) {
		this.response = response;
		if(this.responseBodyTransformer != null) {
			this.response.body().transform(this.responseBodyTransformer);
		}
		if(this.exchangeSink != null) {
			this.exchangeSink.success(this);
		}
	}
	
	@Override
	public ExchangeContext context() {
		return this.exchangeContext;
	}

	public void start(AbstractExchange.Handler<A, B, C> handler) {
		if(this.handler != null) {
			throw new IllegalStateException("Exchange already started");
		}
		this.handler = Objects.requireNonNull(handler);
		Future<?> startFuture = this.executeInEventLoop(() -> {
			this.doStart();
		});
		if(this.exchangeSink != null) {
			startFuture.addListener(future -> {
				this.handler.exchangeError(this, future.cause());
			});
		}
	}
	
	protected abstract void doStart() throws HttpClientException;
	
	public void complete() {
		this.handler.exchangeComplete(this);
	}
	
	public void dispose() {
		this.dispose(null);
	}
	
	public void dispose(Throwable error) {
		// fail the exchange
		if(this.exchangeSink != null) {
			this.exchangeSink.error(error);
		}
		
		// dispose the subscription: data or file
		if(this.disposable != null) {
			this.disposable.dispose();
		}
		
		// We must dispose the response in order to drain the data publisher if it wasn't subscribed.
		if(this.response != null) {
			if(error != null && this.response.data() != null) {
				this.response.data().tryEmitError(error);
			}
			this.response.dispose();
		}
	}
	
	public static interface Handler<A extends AbstractRequest, B extends AbstractResponse, C extends AbstractExchange<A, B, C>> {
		
		static Handler DEFAULT = new Handler() {};
		
		// The exchange is started just before the request is written
		default void exchangeStart(C exchange) {
			
		}
		
		// the request has been fully sent
		default void requestComplete(C exchange) {
			
		}

		default void exchangeComplete(C exchange) {
			
		}
		
		default void exchangeError(C exchange, Throwable t) {
			this.exchangeComplete(exchange);
		}
	}
}

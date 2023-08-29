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
import io.inverno.mod.http.base.HttpVersion;
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
 * <p>
 * Base {@link Exchange} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the request type
 * @param <B> the response type
 * @param <C> the exchange type
 */
public abstract class AbstractExchange<A extends AbstractRequest, B extends AbstractResponse, C extends AbstractExchange<A, B, C>> implements Exchange<ExchangeContext> {

	protected final ChannelHandlerContext context;
	protected final EventLoop eventLoop;
	private final MonoSink<Exchange<ExchangeContext>> exchangeSink;
	private final ExchangeContext exchangeContext;
	protected final HttpVersion protocol;
	protected final A request;
	protected final Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer;
	
	protected B response;
	protected AbstractExchange.Handler handler;
	protected Disposable disposable;

	/**
	 * <p>
	 * Creates a client exchange.
	 * </p>
	 *
	 * @param context                 the channel handler context
	 * @param exchangeSink            the exchange sink
	 * @param exchangeContext         the exchange context
	 * @param protocol                the HTTP protocol version
	 * @param request                 the HTTP request
	 * @param responseBodyTransformer a response body transformer
	 */
	public AbstractExchange(
			ChannelHandlerContext context, 
			MonoSink<Exchange<ExchangeContext>> exchangeSink, 
			ExchangeContext exchangeContext,
			HttpVersion protocol,
			A request, 
			Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer) {
		this.context = context;
		this.eventLoop = this.context.channel().eventLoop();
		this.exchangeSink = exchangeSink;
		this.exchangeContext = exchangeContext;
		this.protocol = protocol;
		this.request = request;
		this.responseBodyTransformer = responseBodyTransformer;
	}
	
	@Override
	public HttpVersion getProtocol() {
		return this.protocol;
	}
	
	@Override
	public A request() {
		return request;
	}
	
	@Override
	public B response() {
		return this.response;
	}
	
	@Override
	public ExchangeContext context() {
		return this.exchangeContext;
	}
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 *
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event loop, otherwise it is scheduled in the event loop.
	 * </p>
	 *
	 * @param runnable the task to execute
	 * 
	 * @return a future which completes once the task completes
	 */
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
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 *
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event loop, otherwise it is scheduled in the event loop.
	 * </p>
	 *
	 * @param <T> that task result type
	 * @param callable the task to execute
	 * 
	 * @return a future which completes once the task completes and returns the task result
	 */
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
	
	/**
	 * <p>
	 * Starts the processing of the exchange with the specified callback handler.
	 * </p>
	 * 
	 * <p>
	 * This methods basically delegates the sending of the request to {@link #doStart() }.
	 * </p>
	 * 
	 * @param handler an exchange callback handler
	 * 
	 * @throws IllegalStateException if the exchange was already started
	 */
	public void start(AbstractExchange.Handler<A, B, C> handler) throws IllegalStateException {
		if(this.handler != null) {
			throw new IllegalStateException("Exchange already started");
		}
		this.handler = Objects.requireNonNull(handler);
		Future<?> startFuture = this.executeInEventLoop(() -> {
			this.doStart();
		});
		if(this.exchangeSink != null) {
			startFuture.addListener(future -> {
				if(!future.isSuccess()) {
					this.handler.exchangeError(this, future.cause());
				}
			});
		}
	}
	
	/**
	 * <p>
	 * Starts the processing of the exchange by sending the request to the server.
	 * </p>
	 * 
	 * @throws HttpClientException if there was a error starting the exchange.
	 */
	protected abstract void doStart() throws HttpClientException;

	/**
	 * <p>
	 * Sets the response in the exchange.
	 * </p>
	 * 
	 * <p>
	 * This basically notifies the exchange sink (i.e. the exchange is ready to be processed)
	 * </p>
	 * 
	 * @param response the response
	 * 
	 * @throws IllegalStateException if the response was already set
	 */
	public final void setResponse(B response) throws IllegalStateException {
		if(this.response != null) {
			throw new IllegalStateException("Response already set");
		}
		this.response = response;
		if(this.responseBodyTransformer != null) {
			this.response.body().transform(this.responseBodyTransformer);
		}
		if(this.exchangeSink != null) {
			this.exchangeSink.success(this);
		}
	}
	
	/**
	 * <p>
	 * Notifies that the exchange is complete.
	 * </p>
	 * 
	 * <p>
	 * This basically means that the last content of the response has been received.
	 * </p>
	 * 
	 * <p>
	 * This method in turn notifies the handler that the exchange is complete, resulting in the exchange to get disposed and the underlying connection being recycled (and that's the important part we
	 * must free the connection as soon as it is not needed anymore). It is important for the response data publisher to be subscribed before otherwise data gets drained and can no longer be consumed.
	 * The response data publisher must then be susbscribed when the exchange is emitted as this would be executed in the event loop so we are sure this notification is received after.
	 * </p>
	 */
	public void notifyComplete() {
		this.handler.exchangeComplete(this);
	}
	
	/**
	 * <p>
	 * Disposes the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method delegates to {@link #dispose(java.lang.Throwable) } with a null error.
	 * </p>
	 */
	public void dispose() {
		this.dispose(null);
	}
	
	/**
	 * <p>
	 * Disposes the exchange with the specified error.
	 * </p>
	 * 
	 * <p>
	 * This method cleans up exchange outstanding resources, it especially disposes the response which in turns drains received data if needed.
	 * </p>
	 * 
	 * <p>
	 * A non-null error indicates that the exchange did not complete successfully and that the error should be emitted when possible (e.g. in the response data publisher).
	 * </p>
	 * 
	 * @param error an error or null
	 * 
	 * @see AbstractResponse#dispose(java.lang.Throwable) 
	 */
	public void dispose(Throwable error) {
		// dispose the subscription: data or file
		if(this.disposable != null) {
			this.disposable.dispose();
		}
		
		// We must dispose the response in order to drain the data publisher if it wasn't subscribed.
		if(this.response != null) {
			this.response.dispose(error);
		}
		else {
			this.exchangeSink.error(error != null ? error : new HttpClientException("Exchange disposed"));
		}
	}
	
	/**
	 * <p>
	 * Exchange callbacks handler.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 * 
	 * @param <A>
	 * @param <B>
	 * @param <C> 
	 */
	public static interface Handler<A extends AbstractRequest, B extends AbstractResponse, C extends AbstractExchange<A, B, C>> {
		
		/**
		 * The default handler.
		 */
		static Handler DEFAULT = new Handler() {};

		/**
		 * <p>
		 * Notifies that the exchange has started.
		 * <p>
		 * 
		 * @param exchange the exchange
		 */
		default void exchangeStart(C exchange) {
			
		}

		/**
		 * <p>
		 * Notifies that the request has been fully sent.
		 * </p>
		 * 
		 * @param exchange the exchange
		 */
		default void requestComplete(C exchange) {
			
		}

		/**
		 * <p>
		 * Notifies that the exchange has completed.
		 * </p>
		 * 
		 * <p>
		 * This means that the response has been fully received.
		 * </p>
		 * 
		 * @param exchange the exchange
		 */
		default void exchangeComplete(C exchange) {
			
		}
		
		/**
		 * <p>
		 * Notifies that an error was raised during the processing of the exchange.
		 * </p>
		 * 
		 * @param exchange the exchange
		 * @param t        an error
		 */
		default void exchangeError(C exchange, Throwable t) {
			this.exchangeComplete(exchange);
		}
	}
}

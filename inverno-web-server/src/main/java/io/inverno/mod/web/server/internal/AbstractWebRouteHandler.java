/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base Web route handler used as resource in an HTTP router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebRouteHandler
 * @see ErrorWebRouteHandler
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 */
abstract class AbstractWebRouteHandler<A extends ExchangeContext, B extends Exchange<A>> implements ReactiveExchangeHandler<A, B> {

	/**
	 * The key pointing to the exchange context in the reactive context.
	 */
	private static final String CONTEXT_EXCHANGE_KEY = "exchange";

	private String responseContentType;
	private ReactiveExchangeHandler<A, B> handler;
	private List<ExchangeInterceptor<A,B>> interceptors;

	private Mono<Void> interceptedHandlerChain;

	/**
	 * <p>
	 * Creates a Web route handler with no exchange handler.
	 * </p>
	 *
	 * <p>
	 * This constructor can be used by implementors who can't provide an exchange handler during init and rely on {@link #setHandler(ReactiveExchangeHandler)} to provide the required exchange handler.
	 * </p>
	 */
	protected AbstractWebRouteHandler() {
		this.interceptors = List.of();
	}

	/**
	 * <p>
	 * Creates a Web route handler with the specified exchange handler.
	 * </p>
	 *
	 * @param handler an exchange handler
	 */
	protected AbstractWebRouteHandler(ReactiveExchangeHandler<A, B> handler) {
		this.interceptors = List.of();
		this.setHandler(handler);
	}

	/**
	 * <p>
	 * Sets the media type produced by the route.
	 * </p>
	 *
	 * <p>
	 * This information is used to set the response {@code content-type} header when processing an exchange and allow the data conversion service to pick the right media type converter.
	 * </p>
	 *
	 * @param responseContentType the media type produced by the route.
	 */
	public void setResponseContentType(String responseContentType) {
		this.responseContentType = responseContentType;
	}

	/**
	 * <p>
	 * Returns the media type produced by the route.
	 * </p>
	 *
	 * @return a media type or null
	 */
	public String getResponseContentType() {
		return responseContentType;
	}

	/**
	 * <p>
	 * Returns the exchange interceptors applied to the exchange handler.
	 * </p>
	 *
	 * @return a list of exchange interceptors
	 */
	public List<ExchangeInterceptor<A, B>> getInterceptors() {
		return interceptors;
	}

	/**
	 * <p>
	 * Sets the exchange interceptors to apply to the exchange handler.
	 * </p>
	 *
	 * @param interceptors a list of exchange interceptors
	 */
	public void setInterceptors(List<ExchangeInterceptor<A, B>> interceptors) {
		this.interceptors = interceptors != null ? interceptors : List.of();
		if(this.handler != null) {
			this.setHandler(this.handler);
		}
	}

	/**
	 * <p>
	 * Returns the exchange handler.
	 * </p>
	 *
	 * @return an exchange handler
	 */
	public ReactiveExchangeHandler<A, B> getHandler() {
		return handler;
	}

	/**
	 * <p>
	 * Sets the exchange handler.
	 * </p>
	 *
	 * <p>
	 * Implementors must use that method to provide an exchange handler in the constructor when it is not possible to provide one it during init.
	 * </p>
	 *
	 * @param handler an exchange handler.
	 */
	protected final void setHandler(ReactiveExchangeHandler<A, B> handler) {
		if(!this.interceptors.isEmpty()) {
			Mono<B> interceptorChain = Mono.deferContextual(context -> Mono.just(context.<B>get(CONTEXT_EXCHANGE_KEY)));
			for(ExchangeInterceptor<A, B> interceptor : this.interceptors) {
				interceptorChain = interceptorChain.flatMap(interceptor::intercept);
			}
			this.interceptedHandlerChain = interceptorChain.flatMap(handler::defer);
			this.handler = handler;
		}
		else {
			this.handler = handler;
			this.interceptedHandlerChain = null;
		}
	}

	@Override
	public Mono<Void> defer(B exchange) throws HttpException {
		if(this.responseContentType != null) {
			exchange.response().headers(headers -> headers.set(Headers.NAME_CONTENT_TYPE, this.responseContentType));
		}

		if(this.interceptedHandlerChain != null) {
			return this.interceptedHandlerChain.contextWrite(ctx -> ctx.put(CONTEXT_EXCHANGE_KEY, exchange));
		}
		else {
			return this.handler.defer(exchange);
		}
	}
}

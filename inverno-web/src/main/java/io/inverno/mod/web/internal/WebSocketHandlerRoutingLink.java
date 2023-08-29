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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.ws.WebSocketException;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebSocketRoute;
import io.inverno.mod.web.spi.InterceptableRoute;
import java.util.LinkedList;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible for the WebSocket route handler.
 * </p>
 *
 * <p>
 * This link must appear at the end of a routing chain and holds the actual WebSocket upgrade and exchange processing logic.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of WebSocket route
 */
class WebSocketHandlerRoutingLink<A extends ExchangeContext, B extends WebSocketRoute<A>> extends RoutingLink<A, WebExchange<A>, WebSocketHandlerRoutingLink<A, B>, B> {

	private static final String CONTEXT_EXCHANGE_KEY = "exchange";
	
	private WebSocketExchangeHandler<A, Web2SocketExchange<A>> webSocketHandler;
	
	private ReactiveExchangeHandler<A, WebExchange<A>> handler;
	
	private List<ExchangeInterceptor<A, WebExchange<A>>> interceptors;
	
	private Mono<Void> interceptedHandlerChain;
	
	private boolean disabled;
	
	/**
	 * <p>
	 * Creates a WebSocket handler routing link.
	 * </p>
	 */
	public WebSocketHandlerRoutingLink() {
		super(WebSocketHandlerRoutingLink::new);
	}
	
	@Override
	public WebSocketHandlerRoutingLink<A, B> setRoute(B route) {
		this.webSocketHandler = route.getWebSocketHandler();
		this.handler = exchange -> Mono.fromRunnable(
			() -> exchange.webSocket(route.getSubProtocol())
				.orElseThrow(() -> new WebSocketException("WebSocket upgrade not supported"))
				.handler(this.webSocketHandler)
		);
		
		if(route instanceof InterceptableRoute) {
			this.setInterceptors(((InterceptableRoute<A, WebExchange<A>>)route).getInterceptors());
		}
		
		return this;
	}
	
	private void setInterceptors(List<? extends ExchangeInterceptor<A, WebExchange<A>>> interceptors) {
		this.interceptors = new LinkedList<>();
		this.interceptedHandlerChain = null;
		if(interceptors != null) {
			this.interceptors.addAll(interceptors);
		}

		if(!this.interceptors.isEmpty() && this.handler != null) {
			Mono<WebExchange<A>> interceptorChain = Mono.deferContextual(context -> Mono.just(context.<WebExchange<A>>get(CONTEXT_EXCHANGE_KEY)));
			for(ExchangeInterceptor<A, WebExchange<A>> interceptor : this.interceptors) {
				interceptorChain = interceptorChain.flatMap(interceptor::intercept);
			}
			this.interceptedHandlerChain = interceptorChain.flatMap(this.handler::defer);
		}
	}

	@Override
	public void enableRoute(B route) {
		if(this.handler != null) {
			this.disabled = false;
		}
	}

	@Override
	public void disableRoute(B route) {
		if(this.handler != null) {
			this.disabled = true;
		}
	}

	@Override
	public void removeRoute(B route) {
		this.handler = null;
	}

	@Override
	public boolean hasRoute() {
		return this.handler != null;
	}

	@Override
	public boolean isDisabled() {
		return this.disabled;
	}

	@Override
	public <F extends RouteExtractor<A, WebExchange<A>, B>> void extractRoute(F extractor) {
		if (!(extractor instanceof WebSocketRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not a WebSocket route extractor");
		}
		if(extractor instanceof InterceptableRouteExtractor) {
			((InterceptableRouteExtractor<A, WebExchange<A>, B, ?>) extractor).interceptors(this.interceptors, this::setInterceptors);
		}
		((WebSocketRouteExtractor<A, ?, ?>)extractor).webSocketHandler(this.webSocketHandler, this.disabled);
		super.extractRoute(extractor);
	}
	
	@Override
	public Mono<Void> defer(WebExchange<A> exchange) throws HttpException {
		if(this.handler == null) {
			throw new RouteNotFoundException();
		}
		if(this.disabled) {
			throw new DisabledRouteException();
		}
		
		if(this.interceptedHandlerChain != null) {
			return this.interceptedHandlerChain.contextWrite(ctx -> ctx.put(CONTEXT_EXCHANGE_KEY, exchange));
		}
		else {
			return this.handler.defer(exchange);
		}
	}
}

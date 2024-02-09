/*
 * Copyright 2020 Jeremy KUHN
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
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebSocketRoute;
import io.inverno.mod.web.server.spi.InterceptableRoute;
import io.inverno.mod.web.server.spi.Route;
import java.util.LinkedList;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible for the route handler.
 * </p>
 *
 * <p>
 * This link must appear at the end of a routing chain and holds the actual request processing logic.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
class HandlerRoutingLink<A extends ExchangeContext, B extends Exchange<A>, C extends Route<A, B>> extends RoutingLink<A, B, HandlerRoutingLink<A, B, C>, C> {

	private static final String CONTEXT_EXCHANGE_KEY = "exchange";
	
	private ReactiveExchangeHandler<A, B> handler;
	
	private List<ExchangeInterceptor<A,B>> interceptors;
	
	private Mono<Void> interceptedHandlerChain;
	
	private boolean disabled;
	
	private RoutingLink<A, WebExchange<A>, ?, WebSocketRoute<A>> webSocketLink;
	
	/**
	 * <p>
	 * Creates a handler routing link.
	 * </p>
	 */
	public HandlerRoutingLink() {
		super(HandlerRoutingLink::new);
	}

	@SuppressWarnings("unchecked")
	@Override
	public HandlerRoutingLink<A, B, C> setRoute(C route) {
		if(route instanceof WebSocketRoute) {
			if(this.webSocketLink == null) {
				this.webSocketLink = new WebSocketProtocolRoutingLink<A, WebExchange<A>, WebSocketRoute<A>>();
				this.webSocketLink.connect(new WebSocketHandlerRoutingLink<>());
			}
			this.webSocketLink.setRoute((WebSocketRoute<A>) route);
		}
		else {
			this.handler = route.getHandler();
			if(route instanceof InterceptableRoute) {
				this.setInterceptors(((InterceptableRoute<A, B>)route).getInterceptors());
			}
		}
		return this;
	}
	
	private void setInterceptors(List<? extends ExchangeInterceptor<A,B>> interceptors) {
		this.interceptors = new LinkedList<>();
		this.interceptedHandlerChain = null;
		if(interceptors != null) {
			this.interceptors.addAll(interceptors);
		}

		if(!this.interceptors.isEmpty() && this.handler != null) {
			Mono<B> interceptorChain = Mono.deferContextual(context -> Mono.just(context.<B>get(CONTEXT_EXCHANGE_KEY)));
			for(ExchangeInterceptor<A,B> interceptor : this.interceptors) {
				interceptorChain = interceptorChain.flatMap(interceptor::intercept);
			}
			this.interceptedHandlerChain = interceptorChain.flatMap(this.handler::defer);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void enableRoute(C route) {
		if(route instanceof WebSocketRoute) {
			if(this.webSocketLink != null) {
				this.webSocketLink.enableRoute((WebSocketRoute<A>)route);
			}
		}
		else {
			if(this.handler != null) {
				this.disabled = false;
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void disableRoute(C route) {
		if(route instanceof WebSocketRoute) {
			if(this.webSocketLink != null) {
				this.webSocketLink.disableRoute((WebSocketRoute<A>)route);
			}
		}
		else {
			if(this.handler != null) {
				this.disabled = true;
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void removeRoute(C route) {
		if(route instanceof WebSocketRoute) {
			if(this.webSocketLink != null) {
				this.webSocketLink.removeRoute((WebSocketRoute<A>)route);
				if(!this.webSocketLink.hasRoute()) {
					this.webSocketLink = null;
				}
			}
		}
		else {
			this.handler = null;
		}
	}
	
	@Override
	public boolean hasRoute() {
		return this.handler != null || (this.webSocketLink != null && this.webSocketLink.hasRoute());
	}
	
	@Override
	public boolean isDisabled() {
		return this.disabled && (this.webSocketLink == null || this.webSocketLink.isDisabled());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <F extends RouteExtractor<A, B, C>> void extractRoute(F extractor) {
		if(this.handler != null) {
			if(extractor instanceof InterceptableRouteExtractor) {
				((InterceptableRouteExtractor<A, B, C, ?>) extractor).interceptors(this.interceptors, this::setInterceptors);
			}
			extractor.handler(this.handler, this.disabled);
		}
		if(this.webSocketLink != null) {
			this.webSocketLink.extractRoute((RouteExtractor<A, WebExchange<A>, WebSocketRoute<A>>)extractor);
		}
		super.extractRoute(extractor);
	}
	
	@Override
	public Mono<Void> defer(B exchange) {
		
		// If we have a webSocketLink, we have a web socket route (GET method + no consume + no produce)
		// If this is a websocket upgrade request, we must delegate to the webSocketLink otherwise we let the regular handler (if any) deal with it.
		
		// If we get there and we have a webSocketLink, it means we have a GET request with no consume and no produce
		// If this is an upgrade: we have a websocket request and we must delegate to the websocketLink
		if(this.webSocketLink != null && exchange.request().headers().get(Headers.NAME_UPGRADE).filter(value -> value.equals(Headers.VALUE_WEBSOCKET)).isPresent()) {
			if(!(exchange instanceof WebExchange)) {
				// This is not very nice but at least it is safe
				throw new InternalServerErrorException("WebSocket upgrade requires a WebExchange");
			}
			return this.webSocketLink.defer((WebExchange<A>)exchange);
		}
		else {
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
}

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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.Route;

/**
 * <p>
 * A routing link responsible for the route handler.
 * </p>
 * 
 * <p>
 * This link must appear at the end of a routing chain and holds the actual
 * request processing logic.
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

	private ExchangeHandler<A, B> handler;
	
	private boolean disabled;
	
	/**
	 * <p>
	 * Creates a handler routing link.
	 * </p>
	 */
	public HandlerRoutingLink() {
		super(HandlerRoutingLink::new);
	}
	
	@Override
	public HandlerRoutingLink<A, B, C> setRoute(C route) {
		this.handler = route.getHandler();
		return this;
	}
	
	@Override
	public <F extends RouteExtractor<A, B, C>> void extractRoute(F extractor) {
		super.extractRoute(extractor);
		extractor.handler(this.handler, this.disabled);
	}
	
	@Override
	public void enableRoute(C route) {
		if(this.handler != null) {
			this.disabled = false;
		}
	}
	
	@Override
	public void disableRoute(C route) {
		if(this.handler != null) {
			this.disabled = true;
		}
	}
	
	@Override
	public void removeRoute(C route) {
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
	public void handle(B exchange) throws HttpException {
		if(this.handler == null) {
			throw new RouteNotFoundException();
		}
		if(this.disabled) {
			throw new DisabledRouteException();
		}
		this.handler.handle(exchange);
	}
}

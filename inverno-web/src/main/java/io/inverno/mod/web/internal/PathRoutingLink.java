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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.spi.PathAware;
import io.inverno.mod.web.spi.Route;
import java.util.HashMap;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible to route an exchange based on the absolute normalized path as defined by {@link PathAware}.
 * </p>
 *
 * <p>
 * This link operates on routes defined with a static path, routes defined with parameterized path are handled by the {@link PathPatternRoutingLink}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
class PathRoutingLink<A extends ExchangeContext, B extends Exchange<A>, C extends PathAware & Route<A, B>> extends RoutingLink<A, B, PathRoutingLink<A, B, C>, C> {

	private final Map<String, RoutingLink<A, B, ?, C>> handlers;

	/**
	 * <p>
	 * Creates a path routing link.
	 * </p>
	 */
	public PathRoutingLink() {
		super(PathRoutingLink::new);
		this.handlers = new HashMap<>();
	}

	@Override
	public PathRoutingLink<A, B, C> setRoute(C route) {
		String path = route.getPath();
		if (path != null) {
			// Exact match
			this.setRoute(path, route);
		} 
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}

	private void setRoute(String path, C route) {
		if (this.handlers.containsKey(path)) {
			this.handlers.get(path).setRoute(route);
		} 
		else {
			this.handlers.put(path, this.nextLink.createNextLink().setRoute(route));
		}
	}

	@Override
	public void enableRoute(C route) {
		String path = route.getPath();
		if (path != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(path);
			if (handler != null) {
				handler.enableRoute(route);
			}
			// route doesn't exist so let's do nothing
		} 
		else {
			this.nextLink.enableRoute(route);
		}
	}

	@Override
	public void disableRoute(C route) {
		String path = route.getPath();
		if (path != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(path);
			if (handler != null) {
				handler.disableRoute(route);
			}
			// route doesn't exist so let's do nothing
		} 
		else {
			this.nextLink.disableRoute(route);
		}
	}

	@Override
	public void removeRoute(C route) {
		String path = route.getPath();
		if (path != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(path);
			if (handler != null) {
				handler.removeRoute(route);
				if (!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good
					this.handlers.remove(path);
				}
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.removeRoute(route);
		}
	}

	@Override
	public boolean hasRoute() {
		return !this.handlers.isEmpty() || this.nextLink.hasRoute();
	}

	@Override
	public boolean isDisabled() {
		return this.handlers.values().stream().allMatch(RoutingLink::isDisabled) && this.nextLink.isDisabled();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <F extends RouteExtractor<A, B, C>> void extractRoute(F extractor) {
		if (!(extractor instanceof PathAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not path aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((PathAwareRouteExtractor<A, B, C, ?>) extractor).path(e.getKey()));
		});
		super.extractRoute(extractor);
	}

	@Override
	public Mono<Void> defer(B exchange) {
		if (this.handlers.isEmpty()) {
			return this.nextLink.defer(exchange);
		} 
		else {
			// Path in the request headers is normalized as per API specification
			RoutingLink<A, B, ?, C> handler = this.handlers.get(exchange.request().getPathAbsolute());
			if (handler == null) {
				handler = this.nextLink;
			}
			return handler.defer(exchange);
		}
	}
}

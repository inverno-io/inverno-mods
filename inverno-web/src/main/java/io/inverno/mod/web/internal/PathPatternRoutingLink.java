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

import io.inverno.mod.base.net.URIMatcher;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.PathParameters;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.spi.PathAware;
import io.inverno.mod.web.spi.Route;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible to route an exchange based on the absolute normalized path as defined by {@link PathAware}.
 * </p>
 *
 * <p>
 * This link operates on routes defined with a parameterized path using the corresponding {@link URIPattern} to match an exchange path, routes defined with static path are handled by the
 * {@link PathRoutingLink}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
class PathPatternRoutingLink<A extends ExchangeContext, B extends Exchange<A>, C extends PathAware & Route<A, B>> extends RoutingLink<A, B, PathPatternRoutingLink<A, B, C>, C> {

	private final Map<URIPattern, RoutingLink<A, B, ?, C>> handlers;

	/**
	 * <p>
	 * Creates a path pattern routing link.
	 * </p>
	 */
	public PathPatternRoutingLink() {
		super(PathPatternRoutingLink::new);
		this.handlers = new HashMap<>();
	}

	@Override
	public PathPatternRoutingLink<A, B, C> setRoute(C route) {
		URIPattern pathPattern = route.getPathPattern();
		if (pathPattern != null) {
			if (this.handlers.containsKey(pathPattern)) {
				this.handlers.get(pathPattern).setRoute(route);
			} 
			else {
				this.handlers.put(pathPattern, this.nextLink.createNextLink().setRoute(route));
			}
		} 
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}

	@Override
	public void enableRoute(C route) {
		URIPattern pathPattern = route.getPathPattern();
		if (pathPattern != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(pathPattern);
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
		URIPattern pathPattern = route.getPathPattern();
		if (pathPattern != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(pathPattern);
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
		URIPattern pathPattern = route.getPathPattern();
		if (pathPattern != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(pathPattern);
			if (handler != null) {
				handler.removeRoute(route);
				if (!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good
					this.handlers.remove(pathPattern);
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
			e.getValue().extractRoute(((PathAwareRouteExtractor<A, B, C, ?>) extractor).pathPattern(e.getKey()));
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
			String normalizedPath = exchange.request().getPathAbsolute();

			URIMatcher bestMatchMatcher = null;
			RoutingLink<A, B, ?, C> bestMatchHandler = null;
			for (Entry<URIPattern, RoutingLink<A, B, ?, C>> e : this.handlers.entrySet()) {
				URIMatcher matcher = e.getKey().matcher(normalizedPath);
				if (matcher.matches() && (bestMatchMatcher == null || matcher.compareTo(bestMatchMatcher) > 0)) {
					bestMatchMatcher = matcher;
					bestMatchHandler = e.getValue();
				}
			}
			if (bestMatchHandler != null) {
				Map<String, String> rawPathParameters = bestMatchMatcher.getParameters();
				if (!rawPathParameters.isEmpty()) {
					if (exchange instanceof WebExchange) {
						PathParameters requestPathParameters = ((WebExchange<?>) exchange).request().pathParameters();
						if (requestPathParameters instanceof MutablePathParameters) {
							((MutablePathParameters) requestPathParameters).putAll(bestMatchMatcher.getParameters());
						}
					}
				}
				return bestMatchHandler.defer(exchange);
			} 
			else {
				return this.nextLink.defer(exchange);
			}
		}
	}
}

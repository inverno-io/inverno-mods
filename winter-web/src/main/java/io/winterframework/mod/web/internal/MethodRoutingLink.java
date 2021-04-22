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
package io.winterframework.mod.web.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.http.base.MethodNotAllowedException;
import io.winterframework.mod.http.base.HttpException;
import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.web.MethodAwareRoute;

/**
 * <p>
 * A routing link responsible to route an exchange based on the HTTP method as
 * defined by {@link MethodAwareRoute}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of exchange handled by the route
 * @param <B> the route type
 */
class MethodRoutingLink<A extends Exchange, B extends MethodAwareRoute<A>> extends RoutingLink<A, MethodRoutingLink<A, B>, B> {

	private Map<Method, RoutingLink<A, ?, B>> handlers;
	private Map<Method, RoutingLink<A, ?, B>> enabledHandlers;

	/**
	 * <p>
	 * Creates a method routing link.
	 * </p>
	 */
	public MethodRoutingLink() {
		super(MethodRoutingLink::new);
		this.handlers = new HashMap<>();
		this.enabledHandlers = Map.of();
	}

	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream()
			.filter(e -> !e.getValue().isDisabled())
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public MethodRoutingLink<A, B> setRoute(B route) {
		Method method = route.getMethod();
		if (method != null) {
			if (this.handlers.containsKey(method)) {
				this.handlers.get(method).setRoute(route);
			}
			else {
				this.handlers.put(method, this.nextLink.createNextLink().setRoute(route));
			}
			this.updateEnabledHandlers();
		}
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}

	@Override
	public void enableRoute(B route) {
		Method method = route.getMethod();
		if (method != null) {
			RoutingLink<A, ?, B> handler = this.handlers.get(method);
			if (handler != null) {
				handler.enableRoute(route);
				this.updateEnabledHandlers();
			}
			// route doesn't exist so let's do nothing
		} 
		else {
			this.nextLink.enableRoute(route);
		}
	}

	@Override
	public void disableRoute(B route) {
		Method method = route.getMethod();
		if (method != null) {
			RoutingLink<A, ?, B> handler = this.handlers.get(method);
			if (handler != null) {
				handler.disableRoute(route);
				this.updateEnabledHandlers();
			}
			// route doesn't exist so let's do nothing
		} 
		else {
			this.nextLink.disableRoute(route);
		}
	}

	@Override
	public void removeRoute(B route) {
		Method method = route.getMethod();
		if (method != null) {
			RoutingLink<A, ?, B> handler = this.handlers.get(method);
			if (handler != null) {
				handler.removeRoute(route);
				if (!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good
					this.handlers.remove(method);
					this.updateEnabledHandlers();
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
	public <F extends RouteExtractor<A, B>> void extractRoute(F extractor) {
		if (!(extractor instanceof MethodAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not method aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((MethodAwareRouteExtractor<A, B, ?>) extractor).method(e.getKey()));
		});
		super.extractRoute(extractor);
	}

	@Override
	public void handle(A exchange) throws HttpException {
		if (this.handlers.isEmpty()) {
			this.nextLink.handle(exchange);
		} 
		else {
			RoutingLink<A, ?, B> handler = this.enabledHandlers.get(exchange.request().getMethod());
			if (handler != null) {
				handler.handle(exchange);
			} 
			else if (this.enabledHandlers.isEmpty()) {
				this.nextLink.handle(exchange);
			} 
			else {
				throw new MethodNotAllowedException(this.handlers.keySet());
			}
		}
	}
}

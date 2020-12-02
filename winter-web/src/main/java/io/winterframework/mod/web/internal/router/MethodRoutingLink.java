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
package io.winterframework.mod.web.internal.router;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.MethodNotAllowedException;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.MethodAwareRoute;

/**
 * @author jkuhn
 *
 */
class MethodRoutingLink<A, B, C extends Exchange<A, B>, D extends MethodAwareRoute<A, B, C>> extends RoutingLink<A, B, C, MethodRoutingLink<A, B, C, D>, D> {

	private Map<Method, RoutingLink<A, B, C, ?, D>> handlers;
	private Map<Method, RoutingLink<A, B, C, ?, D>> enabledHandlers;
	
	/**
	 * @param supplier
	 */
	public MethodRoutingLink() {
		super(MethodRoutingLink::new);
		this.handlers = new HashMap<>();
		this.enabledHandlers = Map.of();
	}
	
	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream().filter(e -> !e.getValue().isDisabled()).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public MethodRoutingLink<A, B, C, D> setRoute(D route) {
		Method method = route.getMethod();
		if(method != null) {
			if(this.handlers.containsKey(method)) {
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
	public void enableRoute(D route) {
		Method method = route.getMethod();
		if(method != null) {
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(method);
			if(handler != null) {
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
	public void disableRoute(D route) {
		Method method = route.getMethod();
		if(method != null) {
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(method);
			if(handler != null) {
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
	public void removeRoute(D route) {
		Method method = route.getMethod();
		if(method != null) {
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(method);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
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
	public <F extends RouteExtractor<A, B, C, D>> void extractRoute(F extractor) {
		if(!(extractor instanceof MethodAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not method aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((MethodAwareRouteExtractor<A, B, C, D, ?>)extractor).method(e.getKey()));
		});
		super.extractRoute(extractor);
	}
	
	@Override
	public void handle(C exchange) throws WebException {
		RoutingLink<A, B, C, ?, D> handler = this.enabledHandlers.get(exchange.request().headers().getMethod());
		if(handler != null) {
			handler.handle(exchange);
		}
		else if(this.enabledHandlers.isEmpty()) {
			this.nextLink.handle(exchange);
		}
		else {
			throw new MethodNotAllowedException(this.handlers.keySet());
		}
	}
}

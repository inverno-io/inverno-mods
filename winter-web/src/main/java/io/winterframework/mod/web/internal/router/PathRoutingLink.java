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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.winterframework.mod.web.BadRequestException;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.PathAwareRoute;

/**
 * @author jkuhn
 *
 */
class PathRoutingLink<A, B, C extends Exchange<A, B>, D extends PathAwareRoute<A, B, C>> extends RoutingLink<A, B, C, PathRoutingLink<A, B, C, D>, D> {
	
	private Map<String, RoutingLink<A, B, C, ?, D>> handlers;
	
	public PathRoutingLink() {
		super(PathRoutingLink::new);
		this.handlers = new HashMap<>();
	}
	
	@Override
	public PathRoutingLink<A, B, C, D> setRoute(D route) {
		String path = route.getPath();
		if(path != null && route.getPathPattern() == null) {
			// Exact match
			this.setRoute(path, route);
		}
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}
	
	private void setRoute(String path, D route) {
		if(this.handlers.containsKey(path)) {
			this.handlers.get(path).setRoute(route);
		}
		else {
			this.handlers.put(path, this.nextLink.createNextLink().setRoute(route));
		}
	}
	
	@Override
	public void enableRoute(D route) {
		String path = route.getPath();
		if(path != null && route.getPathPattern() == null) {
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(path);
			if(handler != null) {
				handler.enableRoute(route);
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.enableRoute(route);
		}
	}
	
	@Override
	public void disableRoute(D route) {
		String path = route.getPath();
		if(path != null && route.getPathPattern() == null) {
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(path);
			if(handler != null) {
				handler.disableRoute(route);
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.disableRoute(route);
		}
	}
	
	@Override
	public void removeRoute(D route) {
		String path = route.getPath();
		if(path != null && route.getPathPattern() == null) {
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(path);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
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
	public <F extends RouteExtractor<A, B, C, D>> void extractRoute(F extractor) {
		if(!(extractor instanceof PathAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not path aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((PathAwareRouteExtractor<A, B, C, D, ?>)extractor).path(e.getKey()));
		});
		super.extractRoute(extractor);
	}

	@Override
	public void handle(C exchange) throws WebException {
		if(this.handlers.isEmpty()) {
			this.nextLink.handle(exchange);
		}
		else {
			String normalizedPath;
			try {
				normalizedPath = new URI(exchange.request().headers().getPath()).normalize().getPath().toString();
			} 
			catch (URISyntaxException e) {
				throw new BadRequestException("Bad URI", e);
			}
			
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(normalizedPath);
			if(handler == null) {
				handler = this.nextLink;
			}
			handler.handle(exchange);
		}
	}
}

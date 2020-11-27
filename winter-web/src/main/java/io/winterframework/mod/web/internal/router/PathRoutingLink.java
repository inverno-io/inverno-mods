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
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.PathAwareRoute;

/**
 * @author jkuhn
 *
 */
class PathRoutingLink<A, B, C, D extends PathAwareRoute<A, B, C>> extends RoutingLink<A, B, C, PathRoutingLink<A, B, C, D>, D> {

	private Map<String, RoutingLink<A, B, C, ?, D>> handlers;
	
	public PathRoutingLink() {
		super(PathRoutingLink::new);
		this.handlers = new HashMap<>();
	}
	
	@Override
	public PathRoutingLink<A, B, C, D> addRoute(D route) {
		if(route.getPath() != null && route.getPathPattern() == null) {
			// Exact match
			this.addRoute(route.getPath(), route);
			if(route.isMatchTrailingSlash()) {
				// We want to match either ... or .../
				if(route.getPath().endsWith("/")) {
					this.addRoute(route.getPath().substring(0, route.getPath().length() - 1), route);
				}
				else {
					this.addRoute(route.getPath() + "/", route);
				}
			}
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}
	
	private void addRoute(String path, D route) {
		if(this.handlers.containsKey(path)) {
			this.handlers.get(path).addRoute(route);
		}
		else {
			this.handlers.put(path, this.nextLink.createNextLink().addRoute(route));
		}
	}
	
	@Override
	public void handle(Request<A, C> request, Response<B> response) throws WebException {
		String normalizedPath;
		try {
			normalizedPath = new URI(request.headers().getPath()).normalize().getPath().toString();
		} 
		catch (URISyntaxException e) {
			throw new BadRequestException("Bad URI", e);
		}
		
		RoutingLink<A, B, C, ?, D> requestHandler = this.handlers.get(normalizedPath);
		if(requestHandler == null) {
			requestHandler = this.nextLink;
		}
		requestHandler.handle(request, response);
	}
}

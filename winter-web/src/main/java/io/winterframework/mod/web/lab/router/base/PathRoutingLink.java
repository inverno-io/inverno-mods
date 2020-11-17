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
package io.winterframework.mod.web.lab.router.base;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;

/**
 * @author jkuhn
 *
 */
public class PathRoutingLink extends RoutingLink<PathRoutingLink> {

	private Map<String, RoutingLink<?>> handlers;
	
	public PathRoutingLink() {
		super(PathRoutingLink::new);
		this.handlers = new HashMap<>();
	}
	
	@Override
	public PathRoutingLink addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
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
	
	private void addRoute(String path, BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		if(this.handlers.containsKey(path)) {
			this.handlers.get(path).addRoute(route);
		}
		else {
			this.handlers.put(path, this.nextLink.createNextLink().addRoute(route));
		}
	}
	
	@Override
	public void handle(Request<RequestBody, BaseContext> request, Response<ResponseBody> response) {
		String normalizedPath;
		try {
			normalizedPath = new URI(request.headers().getPath()).normalize().getPath().toString();
		} 
		catch (URISyntaxException e) {
			throw new RoutingException(500, e);
		}
		
		RoutingLink<?> requestHandler = this.handlers.get(normalizedPath);
		if(requestHandler == null) {
			requestHandler = this.nextLink;
		}
		requestHandler.handle(request, response);
	}
}

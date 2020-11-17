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

import java.util.HashMap;
import java.util.Map;

import io.winterframework.mod.web.Method;
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
public class MethodRoutingLink extends RoutingLink<MethodRoutingLink> {

	private Map<Method, RoutingLink<?>> handlers;
	
	/**
	 * @param supplier
	 */
	public MethodRoutingLink() {
		super(MethodRoutingLink::new);
		this.handlers = new HashMap<>();
	}

	@Override
	public MethodRoutingLink addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		if(route.getMethods() != null && !route.getMethods().isEmpty()) {
			for(Method method : route.getMethods()) {
				if(this.handlers.containsKey(method)) {
					this.handlers.get(method).addRoute(route);
				}
				else {
					this.handlers.put(method, this.nextLink.createNextLink().addRoute(route));
				}
			}
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}
	
	@Override
	public void handle(Request<RequestBody, BaseContext> request, Response<ResponseBody> response) {
		RoutingLink<?> requestHandler = this.handlers.get(request.headers().getMethod());
		if(requestHandler != null) {
			requestHandler.handle(request, response);
		}
		else if(this.handlers.isEmpty()) {
			this.nextLink.handle(request, response);
		}
		else {
			throw new RoutingException(405, "Method Not Allowed");
		}
	}
}

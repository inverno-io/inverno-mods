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

import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.Route;

/**
 * @author jkuhn
 *
 */
class HandlerRoutingLink<A, B, C, D extends Route<A, B, C>> extends RoutingLink<A, B, C, HandlerRoutingLink<A, B, C, D>, D> { // extends RoutingLink<HandlerRoutingLink> {

	private RequestHandler<A, B, C> requestHandler;
	
	public HandlerRoutingLink() {
		super(HandlerRoutingLink::new);
	}
	
	public HandlerRoutingLink<A, B, C, D> addRoute(D route) {
		// Should throw a duplicate exception if the handler is already set?
		// Let's trust Winter compiler for duplicate detection and override handlers at runtime
		this.requestHandler = route.getHandler();
		return this;
	}

	@Override
	public void handle(Request<A, C> request, Response<B> response) throws WebException {
		if(this.requestHandler == null) {
			throw new NotFoundException();
		}
		this.requestHandler.handle(request, response);
	}
}

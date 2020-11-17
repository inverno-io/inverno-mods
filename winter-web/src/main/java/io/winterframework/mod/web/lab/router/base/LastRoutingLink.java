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

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;

/**
 * @author jkuhn
 *
 */
public class LastRoutingLink extends RoutingLink<LastRoutingLink> {

	private RequestHandler<RequestBody, BaseContext, ResponseBody> requestHandler;
	
	public LastRoutingLink() {
		super(LastRoutingLink::new);
	}
	
	public LastRoutingLink addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		// Should throw a duplicate exception if not null?
		// Let's trust Winter compiler for duplicate detection and override handlers at runtime
		this.requestHandler = route.getHandler();
		return this;
	}

	@Override
	public void handle(Request<RequestBody, BaseContext> request, Response<ResponseBody> response) {
		if(this.requestHandler == null) {
			throw new RoutingException(404, "Not Found");
		}
		this.requestHandler.handle(request, response);
	}
}

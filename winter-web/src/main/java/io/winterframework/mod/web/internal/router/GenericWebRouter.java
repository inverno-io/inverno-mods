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

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.router.WebContext;
import io.winterframework.mod.web.router.WebRoute;
import io.winterframework.mod.web.router.WebRouteManager;
import io.winterframework.mod.web.router.WebRouter;

/**
 * @author jkuhn
 *
 */
public class GenericWebRouter implements WebRouter<RequestBody, ResponseBody, WebContext> {

	private RoutingLink<RequestBody, ResponseBody, WebContext, ?, WebRoute<RequestBody, ResponseBody, WebContext>> firstLink;
	
	public GenericWebRouter() {
		AcceptCodec acceptCodec = new AcceptCodec(false);
		ContentTypeCodec contentTypeCodec = new ContentTypeCodec();
		AcceptLanguageCodec acceptLanguageCodec = new AcceptLanguageCodec(false);
		
		this.firstLink = new PathRoutingLink<>();
		
		this.firstLink
			.connect(new PathPatternRoutingLink<>())
			.connect(new MethodRoutingLink<>())
			.connect(new ConsumesRoutingLink<>(acceptCodec))
			.connect(new ProducesRoutingLink<>(acceptCodec, contentTypeCodec))
			.connect(new LanguageRoutingLink<>(acceptLanguageCodec))
			.connect(new HandlerRoutingLink<>());
	}

	@Override
	public WebRouteManager<RequestBody, ResponseBody, WebContext> route() {
		return new GenericWebRouteManager(this);
	}

	void addRoute(WebRoute<RequestBody, ResponseBody, WebContext> route) {
		this.firstLink.addRoute(route);
	}

	void disableRoute(WebRoute<RequestBody, ResponseBody, WebContext> route) {
		
	}

	void enableRoute(WebRoute<RequestBody, ResponseBody, WebContext> route) {
		
	}

	void removeRoute(WebRoute<RequestBody, ResponseBody, WebContext> route) {
		
	}
	
	@Override
	public void handle(Request<RequestBody, Void> request, Response<ResponseBody> response) throws WebException {
		this.firstLink.handle(request.mapContext(ign -> new GenericWebContext()), response);
	}
}

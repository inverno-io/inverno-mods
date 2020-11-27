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
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.router.ErrorRoute;
import io.winterframework.mod.web.router.ErrorRouteManager;
import io.winterframework.mod.web.router.ErrorRouter;

/**
 * @author jkuhn
 *
 */
public class GenericErrorRouter implements ErrorRouter {

	private RoutingLink<Void, ResponseBody, Throwable, ?, ErrorRoute> firstLink;
	
	public GenericErrorRouter() {
		AcceptCodec acceptCodec = new AcceptCodec(false);
		ContentTypeCodec contentTypeCodec = new ContentTypeCodec();
		AcceptLanguageCodec acceptLanguageCodec = new AcceptLanguageCodec(false);
		
		this.firstLink = new ThrowableRoutingLink();
		this.firstLink.connect(new ProducesRoutingLink<>(acceptCodec, contentTypeCodec))
			.connect(new LanguageRoutingLink<>(acceptLanguageCodec))
			.connect(new HandlerRoutingLink<>());
	}

	@Override
	public ErrorRouteManager route() {
		return new GenericErrorRouteManager(this);
	}
	
	void addRoute(ErrorRoute route) {
		this.firstLink.addRoute(route);
	}
	
	void disableRoute(ErrorRoute route) {
		
	}

	void enableRoute(ErrorRoute route) {
		
	}

	void removeRoute(ErrorRoute route) {
		
	}

	@Override
	public void handle(Request<Void, Throwable> request, Response<ResponseBody> response) throws WebException {
		ErrorRouter.super.handle(request, response);
		this.firstLink.handle(request, response);
	}
}

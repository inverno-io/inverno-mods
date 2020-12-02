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

import java.util.Set;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRoute;
import io.winterframework.mod.web.router.WebRouteManager;
import io.winterframework.mod.web.router.WebRouter;

/**
 * @author jkuhn
 *
 */
public class GenericWebRouter implements WebRouter<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> {

	private RoutingLink<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>, ?, WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> firstLink;
	
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

	void setRoute(WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> route) {
		this.firstLink.setRoute(route);
	}
	
	void enableRoute(WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> route) {
		this.firstLink.enableRoute(route);
	}
	
	void disableRoute(WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> route) {
		this.firstLink.disableRoute(route);
	}

	void removeRoute(WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> route) {
		this.firstLink.removeRoute(route);
	}
	
	@Override
	public Set<WebRoute<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>>> getRoutes() {
		GenericWebRouteExtractor routeExtractor = new GenericWebRouteExtractor(this);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public WebRouteManager<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> route() {
		return new GenericWebRouteManager(this);
	}

	@Override
	public void handle(Exchange<RequestBody, ResponseBody> exchange) throws WebException {
		this.firstLink.handle(new GenericWebExchange(exchange));
	}
}

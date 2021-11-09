/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * A web route manager used to manage web routes in a web intercepting router.
 * </p>
 *
 * <p>
 * It is created by a web intercepting router and allows to define, enable, disable, remove and find error routes in a web router. Interceptors configured in the originating intercepting router are
 * applied when defining a new route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 */
public interface WebInterceptedRouteManager<A extends ExchangeContext> extends WebRouteManager<A> {

	@Override
	public WebInterceptedRouteManager<A> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;

	@Override
	default public WebInterceptedRouteManager<A> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	@Override
	public WebInterceptedRouteManager<A> method(Method method);
	
	@Override
	public WebInterceptedRouteManager<A> consumes(String mediaRange);
	
	@Override
	public WebInterceptedRouteManager<A> produces(String mediaType);
	
	@Override
	public WebInterceptedRouteManager<A> language(String language);

	@Override
	public WebInterceptedRouter<A> handler(WebExchangeHandler<? super A> handler);
}

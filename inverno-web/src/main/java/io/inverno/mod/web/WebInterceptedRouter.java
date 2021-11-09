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

import io.inverno.mod.web.spi.InterceptedRouter;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import java.util.function.Consumer;

/**
 * <p>
 * A Web intercepting router attaches interceptors to route handler based on the parameters of the Web route including the path or path pattern, the method, the content type and the accepted content
 * type and language.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 */
public interface WebInterceptedRouter<A extends ExchangeContext> extends WebRouter<A>, InterceptedRouter<A, WebExchange<A>, WebRouter<A>, WebInterceptedRouter<A>, WebRouteManager<A>, WebInterceptorManager<A>, WebRoute<A>, Exchange<A>> {

	@Override
	public WebInterceptedRouteManager<A> route();

	@Override
	public WebInterceptedRouter<A> route(Consumer<WebRouteManager<A>> routeConfigurer);
}

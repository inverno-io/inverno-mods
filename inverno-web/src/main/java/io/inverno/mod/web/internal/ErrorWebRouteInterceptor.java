/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.ErrorWebRoute;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ErrorAware;
import io.inverno.mod.web.spi.PathAware;

/**
 * <p>
 * An Error Web route interceptor specifies criteria that determine whether a web exchange interceptor should be applied on an error web route.
 * <p>
 *
 * <p>
 * It basically supports the following criteria:
 * </p>
 *
 * <ul>
 * <li>the type of the error thrown during the regular processing of a request</li>
 * <li>the path to the resource which can be parameterized as defined by {@link URIBuilder}.</li>
 * <li>the content type of the resource</li>
 * <li>the language tag of the resource</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the exchange context
 */
interface ErrorWebRouteInterceptor<A extends ExchangeContext> extends
	ErrorAwareRouteInterceptor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteInterceptor<A>>,
	PathAwareRouteInterceptor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteInterceptor<A>>,
	AcceptAwareRouteInterceptor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteInterceptor<A>>,
	RouteInterceptor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteInterceptor<A>> {

	@Override
	default ErrorWebRouteInterceptor<A> matches(ErrorWebRoute<A> route) {
		ErrorWebRouteInterceptor<A> matcher = this.matches((ErrorAware) route);
		if(matcher != null) {
			matcher = matcher.matches((PathAware)route);
		}
		if(matcher != null) {
			matcher = matcher.matches((AcceptAware)route);
		}
		return matcher;
	}
}

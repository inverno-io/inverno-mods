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
package io.inverno.mod.web.internal;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ContentAware;
import io.inverno.mod.web.spi.MethodAware;
import io.inverno.mod.web.spi.PathAware;

/**
 * <p>
 * A Web route interceptor specifies criteria that determine whether a web exchange interceptor should be applied on a web route.
 * <p>
 *
 * <p>
 * It basically supports the following criteria:
 * </p>
 *
 * <ul>
 * <li>the path to the resource which can be parameterized as defined by {@link URIBuilder}.</li>
 * <li>the HTTP method used to access the resource</li>
 * <li>the media range defining the content types accepted by the resource as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.</li>
 * <li>the content type of the resource</li>
 * <li>the language tag of the resource</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @param <A> the type of the exchange context
 */
interface WebRouteInterceptor<A extends ExchangeContext> extends 
	PathAwareRouteInterceptor<A, WebExchange<A>, WebRoute<A>, WebRouteInterceptor<A>>, 
	MethodAwareRouteInterceptor<A, WebExchange<A>, WebRoute<A>, WebRouteInterceptor<A>>, 
	ContentAwareRouteInterceptor<A, WebExchange<A>, WebRoute<A>, WebRouteInterceptor<A>>, 
	AcceptAwareRouteInterceptor<A, WebExchange<A>, WebRoute<A>, WebRouteInterceptor<A>>, 
	RouteInterceptor<A, WebExchange<A>, WebRoute<A>, WebRouteInterceptor<A>> {

	@Override
	default WebRouteInterceptor<A> matches(WebRoute<A> route) {
		WebRouteInterceptor<A> matcher = this.matches((PathAware)route);
		if(matcher != null) {
			matcher = matcher.matches((MethodAware)route);
		}
		if(matcher != null) {
			matcher = matcher.matches((ContentAware)route);
		}
		if(matcher != null) {
			matcher = matcher.matches((AcceptAware)route);
		}
		return matcher;
	}
}

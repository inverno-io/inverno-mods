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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.server.spi.AcceptAware;
import io.inverno.mod.web.server.spi.Route;

/**
 * <p>
 * A route interceptor for intercepting accept aware routes.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange intercepted by the interceptor
 * @param <C> the route type
 * @param <D> the route interceptor type
 */
interface AcceptAwareRouteInterceptor<A extends ExchangeContext, B extends Exchange<A>, C extends Route<A, B>, D extends AcceptAwareRouteInterceptor<A, B, C, D>> extends RouteInterceptor<A, B, C, D> {

	/**
	 * <p>
	 * Determines whether the specified accept aware is matched by the route interceptor.
	 * </p>
	 * 
	 * @param acceptAware an accept aware
	 * 
	 * @return a route interceptor if the accept aware is a match, null otherwise
	 */
	default D matches(AcceptAware acceptAware) {
		D matcher = this.matchesContentType(acceptAware);
		if(matcher != null) {
			matcher = matcher.matchesContentLanguage(acceptAware);
		}
		return matcher;
	}
	
	/**
	 * <p>
	 * Determines whether the content type produced by the specified accept aware is matched by the route interceptor.
	 * </p>
	 *
	 * @param acceptAware an accept aware
	 *
	 * @return a route interceptor if the accept aware is a match, null otherwise
	 */
	D matchesContentType(AcceptAware acceptAware);
	
	/**
	 * <p>
	 * Determines whether the language produced by specified accept aware is matched by the route interceptor.
	 * </p>
	 * 
	 * @param acceptAware an accept aware
	 * 
	 * @return a route interceptor if the accept aware is a match, null otherwise
	 */
	D matchesContentLanguage(AcceptAware acceptAware);
}

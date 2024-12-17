/*
 * Copyright 2020 Jeremy Kuhn
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

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.internal.router.InternalWebRouter;

/**
 * <p>
 * Generic {@link WebRoute} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the exchange context type
 */
public class GenericWebRoute<A extends ExchangeContext> extends AbstractWebRoute<A, WebExchange<A>, WebRouteHandler<A>, InternalWebRouter.Route<A>> implements WebRoute<A> {

	/**
	 * <p>
	 * Creates a generic Web route.
	 * </p>
	 *
	 * @param route an internal route
	 */
	public GenericWebRoute(InternalWebRouter.Route<A> route) {
		super(route);
	}

	@Override
	public String getPath() {
		return this.route.getPath();
	}

	@Override
	public URIPattern getPathPattern() {
		return this.route.getPathPattern();
	}

	@Override
	public Method getMethod() {
		return this.route.getMethod();
	}

	@Override
	public String getConsume() {
		return this.route.getContentType();
	}

	@Override
	public String getProduce() {
		return this.route.getAccept();
	}

	@Override
	public String getLanguage() {
		return this.route.getLanguage();
	}

	@Override
	public ReactiveExchangeHandler<A, WebExchange<A>> getHandler() {
		return this.route.get().getHandler();
	}

	@Override
	public String toString() {
		return this.route.toString();
	}
}

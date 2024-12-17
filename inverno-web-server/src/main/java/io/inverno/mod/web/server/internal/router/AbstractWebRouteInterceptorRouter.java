/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal.router;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.router.Router;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Base internal interceptor router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 * @param <C> the internal route type
 * @param <D> the internal interceptor route type
 * @param <E> the internal interceptor route manager type
 * @param <F> the internal interceptor router type
 * @param <G> the internal interceptor route matcher
 */
abstract class AbstractWebRouteInterceptorRouter<
		A extends ExchangeContext,
		B extends Exchange<A>,
		C extends AbstractWebRoute<?, ?, ?, ?, ?, ?>,
		D extends AbstractWebRouteInterceptorRoute<A, B, ?>,
		E extends AbstractWebRouteInterceptorRouteManager<A, B, C, D, E, F, G>,
		F extends AbstractWebRouteInterceptorRouter<A, B, C, D, E, F, G>,
		G extends AbstractWebRouteInterceptorRouteMatcher<A, B, C, D>
	> implements Router<ExchangeInterceptor<A, B>, C, D, E, F> {

	private final F parent;

	private List<D> routes;

	/**
	 * <p>
	 * Creates an internal interceptor router
	 * </p>
	 */
	protected AbstractWebRouteInterceptorRouter() {
		this(null);
	}

	/**
	 * <p>
	 * Creates an internal interceptor router
	 * </p>
	 *
	 * @param parent the parent router
	 */
	protected AbstractWebRouteInterceptorRouter(F parent) {
		this.parent = parent;
		this.routes = new ArrayList<>();
	}

	/**
	 * <p>
	 * Creates an internal interceptor route.
	 * </p>
	 *
	 * @param parentRoute the parent route
	 *
	 * @return an internal interceptor route
	 */
	protected abstract D createRoute(D parentRoute);

	/**
	 * <p>
	 * Creates an internal interceptor route manager.
	 * </p>
	 *
	 * @return an internal interceptor route manager
	 */
	protected abstract E createRouteManager();

	/**
	 * <p>
	 * Creates an internal interceptor route matcher.
	 * </p>
	 *
	 * @return an internal interceptor route matcher
	 */
	protected abstract G createRouteMatcher(D interceptorRoute, C webRoute);

	@Override
	public final E route() {
		return this.createRouteManager();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final F route(Consumer<E> configurer) {
		if(configurer != null) {
			E routeManager = this.createRouteManager();
			configurer.accept(routeManager);
			return routeManager.router;
		}
		return (F)this;
	}

	@Override
	public final Set<D> getRoutes() {
		Set<D> result = new HashSet<>();
		if(this.parent != null) {
			result.addAll(this.parent.getRoutes());
		}
		result.addAll(this.routes);
		return result;
	}

	/**
	 * <p>
	 * Adds internal interceptor routes to the router.
	 * </p>
	 *
	 * @param routes a list of internal interceptor routes.
	 */
	public final void addRoutes(List<D> routes) {
		this.routes.addAll(routes);
	}

	@Override
	public final ExchangeInterceptor<A, B> resolve(C input) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Collection<ExchangeInterceptor<A, B>> resolveAll(C input) {
		List<ExchangeInterceptor<A, B>> result = new ArrayList<>();
		if(this.parent != null) {
			result.addAll(this.parent.resolveAll(input));
		}
		for(D interceptorRoute : this.routes) {
			ExchangeInterceptor<A, B> match = this.createRouteMatcher(interceptorRoute, input).matches();
			if(match != null) {
				result.add(match);
			}
		}
		return result;
	}
}

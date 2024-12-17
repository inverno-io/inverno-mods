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
package io.inverno.mod.http.base.router;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * Base {@link RouteExtractor} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the route type
 * @param <D> the route manager type
 * @param <E> the router type
 * @param <F> the route extractor type
 */
public abstract class AbstractRouteExtractor<A, B, C extends AbstractRoute<A, B, C, D, E, F>, D extends AbstractRouteManager<A, B, C, D, E, F>, E extends AbstractRouter<A, B, C, D, E, F>, F extends AbstractRouteExtractor<A, B, C, D, E, F>> implements RouteExtractor<A, C> {

	private final E router;
	protected final F parent;

	private Set<C> routes;

	/**
	 * <p>
	 * Creates a route extractor.
	 * </p>
	 *
	 * @param router the router
	 */
	protected AbstractRouteExtractor(E router) {
		this.parent = null;
		this.router = router;
	}

	/**
	 * <p>
	 * Creates a route extractor.
	 * </p>
	 *
	 * @param parent the parent route extractor
	 */
	protected AbstractRouteExtractor(F parent) {
		this.parent = parent;
		this.router = null;
	}

	/**
	 * <p>
	 * Returns the router.
	 * </p>
	 *
	 * @return the router
	 */
	private E getRouter() {
		if(this.router != null) {
			return this.router;
		}
		return this.parent != null ? ((AbstractRouteExtractor<A,B,C,D,E,F>)this.parent).getRouter() : null;
	}

	/**
	 * <p>
	 * Stored the specified route in the list of extracted routes.
	 * </p>
	 *
	 * @param route a route
	 */
	protected final void addRoute(C route) {
		if(this.parent != null) {
			this.parent.addRoute(route);
		}
		else {
			if(this.routes == null) {
				this.routes = new HashSet<>();
			}
			this.routes.add(route);
		}
	}

	/**
	 * <p>
	 * Populates the route before storing it in the list of extracted routes.
	 * </p>
	 *
	 * <p>
	 * This method is invoked when a resource is extracted in order to populate the route created by the router with the criteria extracted by the extractor.
	 * </p>
	 *
	 * @param route a blank route
	 */
	protected abstract void populateRoute(C route);

	@Override
	public final void set(A resource, boolean disabled) {
		if(resource != null) {
			C route = this.getRouter().createRoute(resource, disabled);
			this.populateRoute(route);
			this.addRoute(route);
		}
	}

	@Override
	public final Set<C> getRoutes() {
		return this.parent != null ? this.parent.getRoutes() : Collections.unmodifiableSet(this.routes);
	}
}

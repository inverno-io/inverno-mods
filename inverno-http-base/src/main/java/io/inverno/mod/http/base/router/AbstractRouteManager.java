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

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * Base {@link RouteManager} implementation.
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
public abstract class AbstractRouteManager<A, B, C extends AbstractRoute<A, B, C, D, E, F>, D extends AbstractRouteManager<A, B, C, D, E, F>, E extends AbstractRouter<A, B, C, D, E, F>, F extends AbstractRouteExtractor<A, B, C, D, E, F>> implements RouteManager<A, B, C, D, E> {

	private final E router;

	/**
	 * <p>
	 * Creates a route manager.
	 * </p>
	 *
	 * @param router the router
	 */
	protected AbstractRouteManager(E router) {
		this.router = router;
	}

	/**
	 * <p>
	 * Returns the route matcher.
	 * </p>
	 *
	 * <p>
	 * The route matcher is used to determine whether a route is matching the criteria defined in the route manager.
	 * </p>
	 *
	 * @see #findRoutes()
	 *
	 * @return a route matcher
	 */
	protected abstract Predicate<C> routeMatcher();

	/**
	 * <p>
	 * Returns the route extractor function.
	 * </p>
	 *
	 * <p>
	 * The route extractor is used to extract routes from the route manager when the resource is set. Relying on a {@link Function} allows to easily chain and combine extractor specific to a criteria.
	 * </p>
	 *
	 * @return a route extractor function
	 */
	protected abstract Function<Consumer<F>, Consumer<F>> routeExtractor();

	@Override
	public E set(Supplier<A> resourceFactory, Consumer<C> routeConfigurer) {
		Objects.requireNonNull(resourceFactory);
		F routesExtractor = this.router.createRouteExtractor();
		this.routeExtractor().apply(extractor -> extractor.set(resourceFactory.get(), false)).accept(routesExtractor);
		routesExtractor.getRoutes().forEach(route -> {
			this.router.setRoute(route);
			routeConfigurer.accept(route);
		});
		return this.router;
	}

	@Override
	public final E enable() {
		this.findRoutes().forEach(Route::enable);
		return this.router;
	}

	@Override
	public final E disable() {
		this.findRoutes().forEach(Route::disable);
		return this.router;
	}

	@Override
	public final E remove() {
		this.findRoutes().forEach(Route::remove);
		return this.router;
	}

	@Override
	public final Set<C> findRoutes() {
		return this.router.getRoutes().stream().filter(this.routeMatcher()).collect(Collectors.toSet());
	}
}

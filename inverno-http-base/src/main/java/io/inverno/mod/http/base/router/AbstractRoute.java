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

/**
 * <p>
 * Base {@link Route} implementation
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
public abstract class AbstractRoute<A, B, C extends AbstractRoute<A, B, C, D, E, F>, D extends AbstractRouteManager<A, B, C, D, E, F>, E extends AbstractRouter<A, B, C, D, E, F>, F extends AbstractRouteExtractor<A, B, C, D, E, F>> implements Route<A> {

	private final E router;
	private final A resource;

	private boolean disabled;

	/**
	 * <p>
	 * Creates a route.
	 * </p>
	 *
	 * @param router   the router
	 * @param resource the resource
	 * @param disabled true to create a disabled route, false otherwise
	 */
	protected AbstractRoute(E router, A resource, boolean disabled) {
		this.router = router;
		this.resource = resource;
		this.disabled = disabled;
	}

	@Override
	public final A get() {
		return this.resource;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void enable() {
		this.router.enableRoute((C)this);
		this.disabled = false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void disable() {
		this.router.disableRoute((C)this);
		this.disabled = true;
	}

	@Override
	public final boolean isDisabled() {
		return this.disabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void remove() {
		this.router.removeRoute((C)this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractRoute<?, ?, ?, ?, ?, ?> that = (AbstractRoute<?, ?, ?, ?, ?, ?>) o;
		return Objects.equals(router, that.router);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(router);
	}
}

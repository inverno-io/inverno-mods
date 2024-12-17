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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.router.Route;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.BaseWebRoute;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * {@link BaseWebRoute} base implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 * @param <C> the Web route handler type
 * @param <D> the internal route type
 */
abstract class AbstractWebRoute<A extends ExchangeContext, B extends Exchange<A>, C extends AbstractWebRouteHandler<A, B>, D extends Route<C>> implements BaseWebRoute<A, B> {

	/**
	 * The internal route.
	 */
	protected final D route;

	/**
	 * <p>
	 * Creates a Web route wrapping the specified route.
	 * </p>
	 *
	 * @param route the internal route
	 */
	protected AbstractWebRoute(D route) {
		this.route = route;
	}

	@Override
	public void enable() {
		this.route.enable();
	}

	@Override
	public void disable() {
		this.route.disable();
	}

	@Override
	public boolean isDisabled() {
		return this.route.isDisabled();
	}

	@Override
	public void remove() {
		this.route.remove();
	}

	@Override
	public List<ExchangeInterceptor<A, B>> getInterceptors() {
		return this.route.get().getInterceptors().stream()
			.map(interceptor -> {
				ExchangeInterceptor<A, B> unwrappedInterceptor = interceptor;
				while(unwrappedInterceptor instanceof ExchangeInterceptorWrapper) {
					unwrappedInterceptor = ((ExchangeInterceptorWrapper<A, B>) unwrappedInterceptor).unwrap();
				}
				return unwrappedInterceptor;
			})
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public void setInterceptors(List<ExchangeInterceptor<A, B>> exchangeInterceptors) {
		this.route.get().setInterceptors(exchangeInterceptors);
	}
}

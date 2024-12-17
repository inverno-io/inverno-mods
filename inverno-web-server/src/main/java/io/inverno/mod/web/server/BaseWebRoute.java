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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.List;

/**
 * <p>
 * Base Web route representing {@link WebRoute}, {@link WebSocketRoute} and {@link ErrorWebRoute}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 */
public interface BaseWebRoute<A extends ExchangeContext, B extends Exchange<A>> {

	/**
	 * <p>
	 * Enables the route.
	 * </p>
	 */
	void enable();

	/**
	 * <p>
	 * Disables the route.
	 * </p>
	 */
	void disable();

	/**
	 * <p>
	 * Determines whether this route is disabled.
	 * </p>
	 *
	 * @return true if the route is disabled false otherwise
	 */
	boolean isDisabled();

	/**
	 * <p>
	 * Removes the route.
	 * </p>
	 */
	void remove();

	/**
	 * <p>
	 * Returns the list of interceptors currently applied to the route.
	 * </p>
	 *
	 * <p>
	 * Note that this method returns the raw interceptors that were specified on the route either by using an interceptor manager or explicitly by invoking {@link #setInterceptors(java.util.List)}.
	 * When specified with an interceptor manager and depending on the criteria that were specified at the time, a raw interceptor might actually be wrapped to filter requests that are matching the
	 * route criteria. For instance, an interceptor that intercepts GET exchanges will be defined on the default route (ie. defined with no criteria) but it will only be invoked on GET exchange.
	 * </p>
	 *
	 * @return a list of interceptors
	 */
	List<ExchangeInterceptor<A, B>> getInterceptors();

	/**
	 * <p>
	 * Sets the list of interceptors to apply to the route.
	 * </p>
	 *
	 * <p>
	 * This method will replace the interceptors previously defined on the route. Unlike interceptors defined using an interceptor manager, the specified interceptors are not filtered and intercept
	 * all exchanges handled by the route.
	 * </p>
	 *
	 * @param exchangeInterceptors a list of interceptors
	 */
	void setInterceptors(List<ExchangeInterceptor<A, B>> exchangeInterceptors);
}

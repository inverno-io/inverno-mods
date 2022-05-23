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
package io.inverno.mod.web.spi;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.List;

/**
 * <p>
 * A route which has the ability to be intercepted using one or more {@link ExchangeInterceptor}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
public interface InterceptableRoute<A extends ExchangeContext, B extends Exchange<A>> extends Route<A, B> {
	
	/**
	 * <p>
	 * Returns the list of exchange interceptors attached to the route.
	 * </p>
	 * 
	 * <p>
	 * Note that this method returns the raw interceptors that were specified on the route either by using an {@link InterceptorManager} or explicitly by invoking
	 * {@link #setInterceptors(java.util.List)}. When specified with an interceptor manager and depending on the criteria that were specified at the time, a raw interceptor might be filtered to only
	 * intercept routes that are matching these criteria. For instance, an interceptor that intercepts GET exchanges will be defined on the default route (ie. defined with no criteria)
	 * but it will only be invoked on GET exchange.
	 * </p>
	 *
	 * @return a list of interceptors
	 */
	List<? extends ExchangeInterceptor<A, B>> getInterceptors();
	
	/**
	 * <p>
	 * Sets the list of exchange interceptors to execute prior to the exchange handler when an exchange is processed by the route.
	 * </p>
	 *
	 * <p>
	 * Interceptors are chained and therefore executed in the same order as the specified list.
	 * </p>
	 * 
	 * <p>
	 * This method is explicit and replaces the interceptors previously defined on the route. Unlike interceptors defined using an {@link InterceptorManager}, the specified interceptors won't be
	 * filtered and intercept all exchanges handled by the route.
	 * </p>
	 *
	 * @param interceptors a list of interceptors
	 */
	void setInterceptors(List<? extends ExchangeInterceptor<A, B>> interceptors);
}

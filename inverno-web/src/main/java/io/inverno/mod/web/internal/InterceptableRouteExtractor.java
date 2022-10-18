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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.spi.Route;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * An interceptable route extractor.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 * @param <D> the interceptable route extractor type
 */
public interface InterceptableRouteExtractor<A extends ExchangeContext, B extends Exchange<A>, C extends Route<A, B>, D extends InterceptableRouteExtractor<A, B, C, D>> extends RouteExtractor<A, B, C> {

	/**
	 * <p>
	 * Extracts the route interceptors.
	 * </p>
	 * 
	 * @param interceptors the route interceptors
	 * @param updater an consumer of interceptors used to update interceptors in the extracted route
	 * 
	 * @return the route extractor
	 */
	D interceptors(List<? extends ExchangeInterceptor<A, B>> interceptors, Consumer<List<? extends ExchangeInterceptor<A, B>>> updater);
}

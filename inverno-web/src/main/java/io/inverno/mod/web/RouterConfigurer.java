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
package io.inverno.mod.web;

import java.util.function.Consumer;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * Base router configurer interface.
 * </p>
 * 
 * <p>
 * A router configurer is used to configure a router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Router
 * 
 * @param <A> the type of the exchange context
 * @param <B> the route exchange type
 * @param <C> the router type
 * @param <D> the route manager type
 * @param <E> the route type
 * @param <F> the router exchange type
 */
public interface RouterConfigurer<A extends ExchangeContext, B extends Exchange<A>, C extends Router<A, B, C, D, E, F>, D extends RouteManager<A, B, C, D, E, F>, E extends Route<A, B>, F extends Exchange<A>> extends Consumer<C> {

}

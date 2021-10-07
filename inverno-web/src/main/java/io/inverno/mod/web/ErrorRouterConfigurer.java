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

import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * Base error router configurer interface.
 * </p>
 * 
 * <p>
 * An error router configurer is used to configure an error router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @see Router
 * 
 * @param <A> the error route exchange type
 * @param <B> the error router type
 * @param <C> the error route manager type
 * @param <D> the route type
 */
public interface ErrorRouterConfigurer<A extends ErrorExchange<Throwable>, B extends ErrorRouter<A, B, C, D>, C extends ErrorRouteManager<A, B, C, D>, D extends Route<ExchangeContext, A>> extends Consumer<B> {

}

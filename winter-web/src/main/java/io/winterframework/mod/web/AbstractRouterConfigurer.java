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
package io.winterframework.mod.web;

import java.util.function.Consumer;

import io.winterframework.mod.http.server.Exchange;

/**
 * <p>
 * Base router configurer interface.
 * </p>
 * 
 * <p>
 * A router configurer is used to configure a router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractRouter
 * 
 * @param <A> the route exchange type
 * @param <B> the router type
 * @param <C> the route manager type
 * @param <D> the route type
 * @param <E> the router exchange type
 */
public interface AbstractRouterConfigurer<A extends Exchange, B extends AbstractRouter<A, B, C, D, E>, C extends AbstractRouteManager<A, B, C, D, E>, D extends AbstractRoute<A>, E extends Exchange> extends Consumer<B> {

}

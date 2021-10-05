/*
 * Copyright 2020 Jeremy KUHN
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

import io.inverno.mod.http.server.ErrorExchange;

/**
 * <p>
 * An error web route manager is used to manage the routes of an error web
 * router. It is created by an error web router and allows to define, enable,
 * disable, remove and find error web routes in an error web router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ErrorWebExchange
 * @see ErrorWebRoute
 * @see ErrorWebRouter
 * 
 * @param <A> the type of web exchange context
 */
public interface ErrorWebRouteManager<A extends WebExchange.Context> extends RouteManager<ErrorWebExchange<Throwable, A>, ErrorWebRouter<A>, ErrorWebRouteManager<A>, ErrorWebRoute<A>, ErrorExchange<Throwable>> {

	/**
	 * <p>
	 * Specifies the route error web exchange handler.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the route specified in the error web route
	 * manager to the error web router it comes from.
	 * </p>
	 * 
	 * @param handler the route error web exchange handler
	 * 
	 * @return the router
	 */
	ErrorWebRouter<A> handler(ErrorWebExchangeHandler<? extends Throwable, ? super A> handler);
	
	/**
	 * <p>
	 * Specifies the type of errors accepted by the error web route.
	 * </p>
	 * 
	 * @param error a type of error
	 * 
	 * @return the error web route manager
	 * 
	 * @see ErrorAwareRoute
	 */
	ErrorWebRouteManager<A> error(Class<? extends Throwable> error);
	
	/**
	 * <p>
	 * Specifies the media type of the resource served by the error web route.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return the error web route manager
	 * 
	 * @see AcceptAwareRoute
	 */
	ErrorWebRouteManager<A> produces(String mediaType);
	
	/**
	 * <p>
	 * Specifies the language of the resource served by the error web route.
	 * </p>
	 * 
	 * @param language a language tag
	 * 
	 * @return the error web route manager
	 * 
	 * @see AcceptAwareRoute
	 */
	ErrorWebRouteManager<A> language(String language);
}

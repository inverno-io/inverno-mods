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
package io.winterframework.mod.web;

import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.ErrorExchangeHandler;

/**
 * <p>
 * An error route manager is used to manage the routes of an error router. It is
 * created by an error router and allows to define, enable, disable, remove and
 * find error routes in an error router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ErrorExchange
 * @see ErrorRoute
 * @see ErrorRouter
 */
public interface ErrorRouteManager extends AbstractRouteManager<ErrorExchange<Throwable>, ErrorRouter, ErrorRouteManager, ErrorRoute, ErrorExchange<Throwable>> {

	/**
	 * <p>
	 * Specifies the route error exchange handler.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the route specified in the error route manager
	 * to the error router it comes from.
	 * </p>
	 * 
	 * @param handler the route error exchange handler
	 * 
	 * @return the router
	 */
	ErrorRouter handler(ErrorExchangeHandler<? extends Throwable> handler);
	
	/**
	 * <p>
	 * Specifies the type of errors accepted by the error route.
	 * </p>
	 * 
	 * @param error a type of error
	 * 
	 * @return the error route manager
	 * 
	 * @see ErrorAwareRoute
	 */
	ErrorRouteManager error(Class<? extends Throwable> error);
	
	/**
	 * <p>
	 * Specifies the media type of the resource served by the error route.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return the error route manager
	 * 
	 * @see AcceptAwareRoute
	 */
	ErrorRouteManager produces(String mediaType);
	
	/**
	 * <p>
	 * Specifies the language of the resource served by the error route.
	 * </p>
	 * 
	 * @param language a language tag
	 * 
	 * @return the error route manager
	 * 
	 * @see AcceptAwareRoute
	 */
	ErrorRouteManager language(String language);
}

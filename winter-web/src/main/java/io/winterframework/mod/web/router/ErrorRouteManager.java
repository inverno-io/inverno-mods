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
package io.winterframework.mod.web.router;

import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.ErrorExchangeHandler;
import io.winterframework.mod.web.ResponseBody;

/**
 * @author jkuhn
 *
 */
public interface ErrorRouteManager extends RouteManager<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>, ErrorRouter, ErrorRouteManager, ErrorRoute, Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> {

	ErrorRouter handler(ErrorExchangeHandler<ResponseBody, ? extends Throwable> handler);
	
	ErrorRouteManager error(Class<? extends Throwable> error) throws IllegalArgumentException;
	
	ErrorRouteManager produces(String mediaType);
	
	ErrorRouteManager language(String language);
}

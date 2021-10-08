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
 * An error web router is used to handle failing requests for which an error was
 * thrown during the initial processing.
 * </p>
 * 
 * <p>
 * It determines the error web exchange handler to invoke based on the type of
 * the error as well as the media type and language accepted by the client.
 * </p>
 * 
 * <p>
 * An error web router is itself an error exchange handler that can be used as
 * error handler of a HTTP server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ErrorExchange
 * @see ErrorWebExchange
 * @see ErrorWebExchangeHandler
 * @see ErrorWebRoute
 * @see ErrorWebRouteManager
 */
public interface ErrorWebRouter extends ErrorRouter<ErrorWebExchange<Throwable>, ErrorWebRouter, ErrorWebRouteManager, ErrorWebRoute> {
	
}

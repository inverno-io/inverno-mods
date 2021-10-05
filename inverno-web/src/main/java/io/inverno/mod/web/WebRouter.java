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

import io.inverno.mod.http.server.Exchange;

/**
 * <p>
 * A web router is used to handle HTTP requests.
 * </p>
 * 
 * <p>
 * It determines the web exchange handler to invoke based on the parameters of
 * the request including the absolute path, the method, the content type and the
 * accepted content type and language.
 * </p>
 * 
 * <p>
 * An web router is itself an exchange handler that can be used as root handler
 * of a HTTP server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebExchange
 * @see WebExchangeHandler
 * @see WebRoute
 * @see WebRouteManager
 *
 * @param <A> the type of web exchange context
 */
public interface WebRouter<A extends WebExchange.Context> extends Router<WebExchange<A>, WebRouter<A>, WebRouteManager<A>, WebRoute<A>, Exchange> {

}

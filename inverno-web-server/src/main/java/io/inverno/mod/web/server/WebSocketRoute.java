/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.web.server;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.ws.Web2SocketExchange;

/**
 * <p>
 * A WebSocket route specifies criteria used to determine the WebSocket exchange handler to execute to handle an exchange.
 * </p>
 *
 * <p>
 * It basically supports the following criteria:
 * </p>
 *
 * <ul>
 * <li>the request path which can be parameterized as defined by {@link io.inverno.mod.base.net.URIBuilder}.</li>
 * <li>the language tag accepted by the request</li>
 * <li>the WebSocket subProtocol used by the handler</li>
 * </ul>
 *
 * <p>
 * The request path criteria can be either static or dynamic if a parameterized path is specified as defined by {@link io.inverno.mod.base.net.URIBuilder}. When a parameterized path is defined, the
 * router extracts path parameters from the {@link io.inverno.mod.base.net.URIMatcher} used to match the request. For instance, path {@code /books/{id}} defines path parameter {@code id} and
 * matches paths: {@code /books/1}, {@code /books/2}...
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the exchange context type
 */
public interface WebSocketRoute<A extends ExchangeContext> extends BaseWebRoute<A, WebExchange<A>> {

	/**
	 * <p>
	 * Returns the absolute normalized path matched by a Web exchange in order to be processed by the route.
	 * </p>
	 *
	 * <p>
	 * Path and path pattern are exclusive.
	 * </p>
	 *
	 * @return an absolute normalized path or null to match any exchange
	 */
	String getPath();

	/**
	 * <p>
	 * Returns the path pattern matched by a Web exchange in order to be processed by the route.
	 * </p>
	 *
	 * <p>
	 * Path and path pattern are exclusive.
	 * </p>
	 *
	 * @return a path pattern or null to match any exchange
	 */
	URIPattern getPathPattern();

	/**
	 * <p>
	 * Returns the language tag or language range as defined <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a> matched by a Web exchange in order to be
	 * processed by the route.
	 * </p>
	 *
	 * @return a language tag, a language range or null to match any exchange
	 */
	String getLanguage();

	/**
	 * <p>
	 * Returns the WebSocket subProtocol matched by a Web exchange in order to be processed by the route.
	 * </p>
	 *
	 * @return a subProtocol or null to match any exchange
	 */
	String getSubProtocol();

	/**
	 * <p>
	 * Returns the WebSocket exchange handler used to handle Web exchanges matching the route criteria.
	 * </p>
	 *
	 * @return a WebSocket exchange handler
	 */
	WebSocketExchangeHandler<A, Web2SocketExchange<A>> getHandler();
}

/*
 * Copyright 2020 Jeremy Kuhn
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
import io.inverno.mod.http.server.ReactiveExchangeHandler;

/**
 * <p>
 * An error Web route specifies criteria used to determine the error Web exchange handler to execute to handle a failing exchange.
 * </p>
 *
 * <p>
 * It basically supports the following criteria:
 * </p>
 *
 * <ul>
 * <li>the type of the error thrown during the regular processing of a request</li>
 * <li>the request path which can be parameterized as defined by {@link io.inverno.mod.base.net.URIBuilder}.</li>
 * <li>the content type of the request</li>
 * <li>the content type accepted by the request</li>
 * <li>the language tag accepted by the request</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ErrorWebRouter
 *
 * @param <A> the exchange context type
 */
public interface ErrorWebRoute<A extends ExchangeContext> extends BaseWebRoute<A, ErrorWebExchange<A>> {

	/**
	 * <p>
	 * Returns the error type matched by an error Web exchange in order to be processed by the route.
	 * </p>
	 *
	 * @return an error type or null to match any exchange
	 */
	Class<? extends Throwable> getErrorType();

	/**
	 * <p>
	 * Returns the absolute normalized path matched by an error Web exchange in order to be processed by the route.
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
	 * Returns the path pattern matched by an error Web exchange in order to be processed by the route.
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
	 * Returns the media range defining the content types as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> matched by an error Web exchange in order
	 * to be processed by the route.
	 * </p>
	 *
	 * @return a media range or null
	 */
	String getConsume();

	/**
	 * <p>
	 * Returns the media type or media range as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-3.1.1.5">RFC 7231 Section 3.1.1.5</a> and
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> matched by an error Web exchange to be processed by the route.
	 * </p>
	 *
	 * @return a media type, a media range or null to match any exchange
	 */
	String getProduce();

	/**
	 * <p>
	 * Return the language tag or language range as defined <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a> matched by an error Web exchange in order
	 * to be processed by the route.
	 * </p>
	 *
	 * @return a language tag, a language range or null to match any exchange
	 */
	String getLanguage();

	/**
	 * <p>
	 * Returns the error Web exchange handler used to handle error Web exchanges matching the route criteria.
	 * </p>
	 *
	 * @return an error Web exchange handler
	 */
	ReactiveExchangeHandler<A, ErrorWebExchange<A>> getHandler();
}

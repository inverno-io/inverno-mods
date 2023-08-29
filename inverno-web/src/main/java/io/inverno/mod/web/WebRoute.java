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

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ContentAware;
import io.inverno.mod.web.spi.InterceptableRoute;
import io.inverno.mod.web.spi.MethodAware;
import io.inverno.mod.web.spi.PathAware;

/**
 * <p>
 * A web route specifies criteria used to determine the web exchange handler to execute to handle a request.
 * </p>
 *
 * <p>
 * It basically supports the following criteria:
 * </p>
 *
 * <ul>
 * <li>the path to the resource which can be parameterized as defined by {@link io.inverno.mod.base.net.URIBuilder}.</li>
 * <li>the HTTP method used to access the resource</li>
 * <li>the media range defining the content types accepted by the resource as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.</li>
 * <li>the content type of the resource</li>
 * <li>the language tag of the resource</li>
 * </ul>
 *
 * <p>
 * The path to the resource can be either static or dynamic if a parameterized path is specified as defined by {@link io.inverno.mod.base.net.URIBuilder}. When defined with a parameterized path, a 
 * router can extract path parameters from the {@link io.inverno.mod.base.net.URIMatcher} that matches the request. For instance, path <code>/books/{id}</code> defines path parameter {@code id} and 
 * matches paths: {@code /books/1}, {@code /books/2}...
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see io.inverno.mod.web.WebExchange
 * @see io.inverno.mod.web.WebRouter
 *
 * @param <A> the type of the exchange context
 */
public interface WebRoute<A extends ExchangeContext> extends InterceptableRoute<A, WebExchange<A>>, PathAware, MethodAware, ContentAware, AcceptAware {

	/**
	 * <p>
	 * Returns the static normalized absolute path to the resource served by the route.
	 * </p>
	 *
	 * <p>
	 * This criteria should exactly match the absolute path of the request.
	 * </p>
	 */
	@Override
	String getPath();

	/**
	 * <p>
	 * Returns the URI pattern that matches all the paths to the resource served by the route.
	 * </p>
	 *
	 * <p>
	 * This criteria should match the absolute path of the request.
	 * </p>
	 */
	@Override
	URIPattern getPathPattern();
	
	/**
	 * <p>
	 * Returns the HTTP method accepted by the resource served by the route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the request HTTP method.
	 * </p>
	 */
	@Override
	Method getMethod();
	
	/**
	 * <p>
	 * Returns the media range defining the content types accepted by the resource served by the route as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.
	 * </p>
	 *
	 * <p>
	 * This criteria should match the request {@code content-type} header field.
	 * </p>
	 */
	@Override
	String getConsume();
	
	/**
	 * <p>
	 * Returns the media type of the resource served by the route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the request {@code accept} header field.
	 * </p>
	 * 
	 * @return a media type or null
	 */
	@Override
	String getProduce();
	
	/**
	 * <p>
	 * Returns the language of the resource served by the route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the request {@code accept-language} header field.
	 * </p>
	 * 
	 * @return a language tag or null
	 */
	@Override
	String getLanguage();
}

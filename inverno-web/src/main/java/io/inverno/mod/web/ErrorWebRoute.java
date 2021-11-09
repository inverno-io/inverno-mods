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

import io.inverno.mod.web.spi.ErrorAware;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.Route;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * An error web route specifies criteria used to determine the error web
 * exchange handler to execute to handle a failing request.
 * </p>
 * 
 * <p>
 * It basically supports the following criteria:
 * </p>
 * 
 * <ul>
 * <li>the type of the error thrown during the regular processing of a
 * request</li>
 * <li>the content type of the resource</li>
 * <li>the language tag of the resource</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ErrorWebExchange
 * @see ErrorWebRouter
 */
public interface ErrorWebRoute extends Route<ExchangeContext, ErrorWebExchange<Throwable>>, AcceptAware, ErrorAware {

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
	
	/**
	 * <p>
	 * Returns the type of errors supported by the resource served by the route.
	 * </p>
	 */
	@Override
	Class<? extends Throwable> getError();
}

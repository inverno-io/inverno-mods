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

import java.util.Map;
import java.util.Optional;

import io.winterframework.mod.base.net.URIPattern;
import io.winterframework.mod.http.server.Exchange;

/**
 * <p>
 * An exchange that extends the HTTP server {@link Exchange} with features for
 * the Web.
 * </p>
 * 
 * <p>
 * It supports request body decoding based on the request content type as well
 * as response body encoding based on the response content type.
 * </p>
 * 
 * <p>
 * It also gives access to path parameters when processed in a route defined
 * with a {@link URIPattern}.
 * </p>
 * 
 * <p>
 * It also attributes that can be set and propagated in a chain of exchange
 * handlers.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRoute
 * @see WebRouteManager
 * @see WebRouter
 */
public interface WebExchange extends Exchange {

	@Override
	WebRequest request();
	
	@Override
	WebResponse response();
	
	/**
	 * <p>
	 * Sets the specified attribute in the exchange.
	 * </p>
	 * 
	 * @param name  the name of the attribute
	 * @param value the value of the attribute
	 */
	void setAttribute(String name, Object value);
	
	/**
	 * <p>
	 * Removes the attribute with the specified name.
	 * </p>
	 * 
	 * @param name the name of the attribute to remove
	 */
	void removeAttribute(String name);
	
	/**
	 * <p>
	 * Returns the value of the attribute with the specified name.
	 * </p>
	 * 
	 * @param <T>  the expected type of the value
	 * @param name the attribute name
	 * 
	 * @return an optional returning the attribute or an empty optional if there's
	 *         no attribute with that name
	 */
	<T> Optional<T> getAttribute(String name);
	
	/**
	 * <p>
	 * Returns all attributes.
	 * </p>
	 * 
	 * @return the attributes
	 */
	Map<String, Object> getAttributes();
}

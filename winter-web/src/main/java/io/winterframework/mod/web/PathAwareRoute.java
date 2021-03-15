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

import io.winterframework.mod.base.net.URIBuilder;
import io.winterframework.mod.base.net.URIMatcher;
import io.winterframework.mod.base.net.URIPattern;
import io.winterframework.mod.http.server.Exchange;

/**
 * <p>
 * A route that specifies criteria used to determine whether the resource served
 * by the route can process a request based on its absolute path.
 * </p>
 * 
 * <p>
 * The path to the resource can be either static or dynamic if a parameterized path is
 * specified as defined by {@link URIBuilder}.
 * </p>
 * 
 * <p>
 * When defined with a parameterized path, a router can extract path parameters
 * from the {@link URIMatcher} that matches the request. For instance, path
 * <code>/books/{id}</code> defines path parameter {@code id} and matches paths:
 * {@code /books/1}, {@code /books/2}...
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractRoute
 * @see URIBuilder
 * @see URIPattern
 * @see URIMatcher
 * 
 * @param <A> the type of web exchange handled by the route
 */
public interface PathAwareRoute<A extends Exchange> extends AbstractRoute<A> {

	/**
	 * <p>
	 * Returns the static normalized absolute path to the resource served by the
	 * route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should exactly match the absolute path of the request.
	 * </p>
	 * 
	 * @return an absolute normalized path
	 */
	String getPath();
	
	/**
	 * <p>
	 * Returns the URI pattern that matches all the paths to the resource served by
	 * the route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the absolute path of the request.
	 * </p>
	 * 
	 * @return a URI pattern
	 */
	URIPattern getPathPattern();
}

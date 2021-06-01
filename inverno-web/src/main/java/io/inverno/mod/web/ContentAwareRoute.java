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
 * A route that specifies criteria used to determine whether the resource served
 * by the route can process a request based on its content type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Route
 * 
 * @param <A> the type of web exchange handled by the route
 */
public interface ContentAwareRoute<A extends Exchange> extends Route<A> {

	/**
	 * <p>
	 * Returns the media range defining the content types accepted by the resource
	 * served by the route as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section
	 * 5.3.2</a>.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the request {@code content-type} header field.
	 * </p>
	 * 
	 * @return a media range or null
	 */
	String getConsume();
}

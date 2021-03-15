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

/**
 * <p>
 * A web route specifies criteria used to determine the web exchange handler to
 * execute to handle a request.
 * </p>
 * 
 * <p>
 * It basically supports the following criteria:
 * </p>
 * 
 * <ul>
 * <li>the path to the resource which can be parameterized as defined by
 * {@link URIBuilder}.</li>
 * <li>the HTTP method used to access the resource</li>
 * <li>the media range defining the content types accepted by the resource as
 * defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC
 * 7231 Section 5.3.2</a>.</li>
 * <li>the content type of the resource</li>
 * <li>the language tag of the resource</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebExchange
 * @see WebRouter
 * 
 * @param <A> the type of web exchange handled by the route
 */
public interface WebRoute<A extends WebExchange> extends 
	PathAwareRoute<A>, 
	MethodAwareRoute<A>, 
	ContentAwareRoute<A>, 
	AcceptAwareRoute<A>, 
	AbstractRoute<A> {

}

/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.base.router;

/**
 * <p>
 * A content route.
 * </p>
 *
 * <p>
 * This is used to define route based on the content type of an input. For instance, in order to resolve a handler for an HTTP request with {@code content-type: application/json} header, a content
 * route must be defined with media type {@code application/json} targeting a handler accepting {@code application/json} content.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.ContentRoutingLink
 *
 * @param <A> the resource type
 */
public interface ContentRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns a media range defining the content types accepted by the route as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.
	 * </p>
	 *
	 * @return a media range or null
	 */
	String getContentType();

	/**+
	 * <p>
	 * A content route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @see io.inverno.mod.http.base.router.link.ContentRoutingLink
	 *
	 * @param <A> the resource type
	 * @param <B> the content route type
	 * @param <C> the content route extractor
	 */
	interface Extractor<A, B extends ContentRoute<A>, C extends ContentRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined to accept the specified content.
		 * </p>
		 *
		 * @param mediaRange a media range
		 *
		 * @return a route extractor
		 */
		C contentType(String mediaRange);
	}

	/**
	 * <p>
	 * A content route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the content route type
	 * @param <D> the content route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends ContentRoute<A>, D extends ContentRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the media range matching the content type of an input.
		 * </p>
		 *
		 * @param mediaRange a media range
		 *
		 * @return the route manager
		 */
		D contentType(String mediaRange);
	}
}

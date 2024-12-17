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
 * An accept content route.
 * </p>
 *
 * <p>
 * This is used to define route based on the content accepted by an input. For instance, in order to resolve a handler for an HTTP request with {@code accept: application/json} header, an accept
 * content route must be defined with media type {@code application/json} targeting the handler producing {@code application/json} content.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.InboundAcceptContentRoutingLink
 * @see io.inverno.mod.http.base.router.link.OutboundAcceptContentRoutingLink
 *
 * @param <A> the resource type
 */
public interface AcceptContentRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns a media type or a media range as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-3.1.1.5">RFC 7231 Section 3.1.1.5</a> and
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>
	 * </p>
	 *
	 * @return a media type, a media range or null
	 */
	String getAccept();

	/**+
	 * <p>
	 * An accept content route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the accept content route type
	 * @param <C> the accept content route extractor
	 */
	interface Extractor<A, B extends AcceptContentRoute<A>, C extends AcceptContentRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified accepted media type or media range.
		 * </p>
		 *
		 * @param accept a media type or a media range
		 *
		 * @return a route extractor
		 */
		C accept(String accept);
	}

	/**
	 * <p>
	 * An accept content route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the accept content route type
	 * @param <D> the accept content route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends AcceptContentRoute<A>, D extends AcceptContentRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the media type or media range matching the content accepted by an input.
		 * </p>
		 *
		 * @param accept a media type or a media range
		 *
		 * @return the route manager
		 */
		D accept(String accept);
	}
}

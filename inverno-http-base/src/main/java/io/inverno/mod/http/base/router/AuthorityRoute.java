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

import java.util.regex.Pattern;

/**
 * <p>
 * An authority route.
 * </p>
 *
 * <p>
 * This is used to define route based on the authority specified in an input. For instance, in order to resolve a handler for an HTTP request with {@code host: example.org} header, an authority route
 * must be defined with authority {@code example.org} targeting a handler producing {@code example.org} content.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.AuthorityRoutingLink
 *
 * @param <A> the resource type
 */
public interface AuthorityRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns an authority.
	 * </p>
	 *
	 * @return an authority
	 */
	String getAuthority();

	/**
	 * <p>
	 * Returns a pattern for authority matching.
	 * </p>
	 *
	 * @return an authority pattern
	 */
	Pattern getAuthorityPattern();

	/**
	 * <p>
	 * An authority route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the authority route type
	 * @param <C> the authority route extractor
	 */
	interface Extractor<A, B extends AuthorityRoute<A>, C extends AuthorityRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified authority.
		 * </p>
		 *
		 * @param authority an authority
		 *
		 * @return a route extractor
		 */
		C authority(String authority);

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified authority pattern.
		 * </p>
		 *
		 * @param authorityPattern an authority pattern
		 *
		 * @return a route extractor
		 */
		C authorityPattern(Pattern authorityPattern);
	}

	/**
	 * <p>
	 * An authority route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the authority route type
	 * @param <D> the authority route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends AuthorityRoute<A>, D extends AuthorityRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the authority matching the authority in an input.
		 * </p>
		 *
		 * @param authority an authority
		 *
		 * @return the route manager
		 */
		D authority(String authority);

		/**
		 * <p>
		 * Specifies the authority pattern matching the authority in an input.
		 * </p>
		 *
		 * @param authorityPattern an authority pattern
		 *
		 * @return the route manager
		 */
		D authorityPattern(Pattern authorityPattern);
	}
}

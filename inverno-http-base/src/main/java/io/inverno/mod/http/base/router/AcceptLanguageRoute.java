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
 * An accept language route.
 * </p>
 *
 * <p>
 * This is used to define route based on the language accepted by an input. For instance, in order to resolve a handler for an HTTP request with {@code accept-language: en-EN} header, an accept
 * language route must be defined with language tag {@code en-EN} targeting a handler producing content in {@code en-EN}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink
 *
 * @param <A> the resource type
 */
public interface AcceptLanguageRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns a language tag or a language range as defined <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a>.
	 * </p>
	 *
	 * @return a language tag, a language range or null
	 */
	String getLanguage();

	/**
	 * <p>
	 * An accept language route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the accept language route type
	 * @param <C> the accept language route extractor
	 */
	interface Extractor<A, B extends AcceptLanguageRoute<A>, C extends AcceptLanguageRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified accepted language tag or language range.
		 * </p>
		 *
		 * @param language a language tag or a language range
		 *
		 * @return a route extractor
		 */
		C language(String language);
	}

	/**
	 * <p>
	 * An accept language route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the accept language route type
	 * @param <D> the accept language route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends AcceptLanguageRoute<A>, D extends AcceptLanguageRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the language tag or language range matching the language accepted by an input.
		 * </p>
		 *
		 * @param language a language tag or a language range
		 *
		 * @return the route manager
		 */
		D language(String language);
	}
}

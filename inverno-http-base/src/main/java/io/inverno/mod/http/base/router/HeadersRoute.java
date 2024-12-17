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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * A headers route.
 * </p>
 *
 * <p>
 * This is used to define route based on the headers specified in an input. For instance, in order to resolve a handler for an HTTP request with a {@code customer: abc} header, an headers route must
 * be defined with a header matcher for {@code customer} header targeting the abc customer handler.
 * </p>
 *
 * @see io.inverno.mod.http.base.router.link.HeadersRoutingLink
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface HeadersRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns the headers matchers for each header matched by the route.
	 * </p>
	 *
	 * @return a map of header matchers
	 */
	Map<String, HeaderMatcher> getHeadersMatchers();

	/**
	 * <p>
	 * A headers route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the headers route type
	 * @param <C> the headers route extractor
	 */
	interface Extractor<A, B extends HeadersRoute<A>, C extends HeadersRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified headers matchers.
		 * </p>
		 *
		 * @param headersMatchers a map of header matchers
		 *
		 * @return a route extractor
		 */
		C headersMatchers(Map<String, HeaderMatcher> headersMatchers);
	}

	/**
	 * <p>
	 * A headers route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the headers route type
	 * @param <D> the headers route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends HeadersRoute<A>, D extends HeadersRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the headers matchers matching headers in an input.
		 * </p>
		 *
		 * @param headersMatchers a map of header matchers
		 *
		 * @return the route manager
		 */
		D headersMatchers(Map<String, HeaderMatcher> headersMatchers);
	}

	/**
	 * <p>
	 * A header matcher used to match a header against a list of static values or or a list of patterns.
	 * </p>
	 *
	 * <p>
	 * A header is matched if any of the static values or patterns defined in the matcher are matching the value.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	final class HeaderMatcher {

		private final Set<String> staticValues;

		private final Set<Pattern> patternValues;

		/**
		 * <p>
		 * Creates a header matcher.
		 * </p>
		 *
		 * @param staticValues  a list of static values
		 * @param patternValues a list of patterns
		 */
		public HeaderMatcher(Set<String> staticValues, Set<Pattern> patternValues) {
			this.staticValues = staticValues != null ? Collections.unmodifiableSet(staticValues) : Set.of();
			this.patternValues = patternValues != null ? Collections.unmodifiableSet(patternValues) : Set.of();
		}

		/**
		 * <p>
		 * Returns the static values matched by the matcher.
		 * </p>
		 *
		 * @return a set of values
		 */
		public Set<String> getValues() {
			return this.staticValues;
		}

		/**
		 * <p>
		 * Returns the patterns matched by the matcher.
		 * </p>
		 *
		 * @return a set of patterns
		 */
		public Set<Pattern> getPatterns() {
			return this.patternValues;
		}

		/**
		 * <p>
		 * Matches the specified header value.
		 * </p>
		 *
		 * @param headerValue a header value
		 *
		 * @return true if any of the static values or patterns is matching the header value, false otherwise
		 */
		public boolean matches(String headerValue) {
			// This is OR
			if(staticValues.contains(headerValue)) {
				return true;
			}

			for(Pattern pattern : this.patternValues) {
				if(pattern.matcher(headerValue).matches()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			HeaderMatcher that = (HeaderMatcher) o;
			return Objects.equals(staticValues, that.staticValues) && Objects.equals(
					patternValues != null ? patternValues.stream().map(Pattern::pattern).collect(Collectors.toSet()) : null,
					that.patternValues != null ? that.patternValues.stream().map(Pattern::pattern).collect(Collectors.toSet()) : null
				);
		}

		@Override
		public int hashCode() {
			return Objects.hash(staticValues, patternValues != null ? patternValues.stream().map(Pattern::pattern).collect(Collectors.toSet()) : null);
		}
	}
}

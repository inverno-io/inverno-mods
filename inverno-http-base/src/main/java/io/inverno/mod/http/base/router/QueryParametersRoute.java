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
 * A query parameters route.
 * </p>
 *
 * <p>
 * This is used to define route based on the query parameters specified in an input. For instance, in order to resolve a handler for an HTTP request with {@code customer=abc} query parameter, a query
 * parameters route must be defined with a parameter matcher for {@code customer} parameter targeting the abc customer handler.
 * </p>
 *
 * @see io.inverno.mod.http.base.router.link.QueryParametersRoutingLink
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface QueryParametersRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns the parameter matchers for each parameter matched by the route.
	 * </p>
	 *
	 * @return a map of parameter matchers
	 */
	Map<String, ParameterMatcher> getQueryParameterMatchers();

	/**
	 * <p>
	 * A query parameters route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the query parameters route type
	 * @param <C> the query parameters route extractor
	 */
	interface Extractor<A, B extends QueryParametersRoute<A>, C extends QueryParametersRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified query parameters matchers.
		 * </p>
		 *
		 * @param queryParametersMatchers a map of parameter matchers
		 *
		 * @return a route extractor
		 */
		C queryParametersMatchers(Map<String, ParameterMatcher> queryParametersMatchers);
	}

	/**
	 * <p>
	 * A query parameters route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the query parameters route type
	 * @param <D> the query parameters route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends QueryParametersRoute<A>, D extends QueryParametersRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the query parameters matchers matching query parameter in an input.
		 * </p>
		 *
		 * @param queryParametersMatchers a map of query parameter matchers
		 *
		 * @return the route manager
		 */
		D queryParametersMatchers(Map<String, ParameterMatcher> queryParametersMatchers);
	}

	/**
	 * <p>
	 * A parameter matcher used to match a parameter against a list of static values or or a list of patterns.
	 * </p>
	 *
	 * <p>
	 * A parameter is matched if any of the static values or patterns defined in the matcher are matching the value.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	final class ParameterMatcher {

		private final Set<String> staticValues;

		private final Set<Pattern> patternValues;

		/**
		 * <p>
		 * Creates a parameter matcher.
		 * </p>
		 *
		 * @param staticValues  a list of static values
		 * @param patternValues a list of patterns
		 */
		public ParameterMatcher(Set<String> staticValues, Set<Pattern> patternValues) {
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
		 * Matches the specified parameter value.
		 * </p>
		 *
		 * @param value a parameter value
		 *
		 * @return true if any of the static values or patterns is matching the value, false otherwise
		 */
		public boolean matches(String value) {
			// This is OR
			if(staticValues.contains(value)) {
				return true;
			}

			for(Pattern pattern : this.patternValues) {
				if(pattern.matcher(value).matches()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			QueryParametersRoute.ParameterMatcher that = (QueryParametersRoute.ParameterMatcher) o;
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

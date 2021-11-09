/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.base.net;

import java.util.regex.Pattern;

/**
 * <p>
 * A URI pattern is used to create a URI matcher to perform match operation on
 * URI against against a regular expression built from a URI Builder's
 * components.
 * </p>
 * 
 * <p>
 * A URI pattern is created from a URI builder by invoking the builder's
 * {@link URIBuilder#buildPattern() buildPattern} or
 * {@link URIBuilder#buildPathPattern() buildPathPattern} methods. It is then
 * used to create URI matchers that performs match operations on an input URI
 * string.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see URIBuilder
 * @see URIMatcher
 */
public interface URIPattern {

	/**
	 * <p>
	 * Describes the inclusion state of a URI pattern in another URI pattern.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.3
	 */
	static enum Inclusion {
		/**
		 * One URI pattern is included in another URI pattern.
		 */
		INCLUDED,
		/**
		 * The sets of URIs matched by the URI patterns are disjoint.
		 */
		DISJOINT,
		/**
		 * It wasn't possible to determine with certainty whether a URI pattern is included into another.
		 */
		INDETERMINATE
	}
	
	/**
	 * <p>
	 * Return the underlying JDK pattern.
	 * </p>
	 * 
	 * @return a pattern
	 */
	Pattern getPattern();

	/**
	 * <p>
	 * Return the URI regular expression.
	 * </p>
	 * 
	 * @return a regular expression
	 */
	String getPatternString();
	
	/**
	 * <p>
	 * Returns the raw value of the pattern.
	 * </p>
	 * 
	 * @return the pattern's raw value
	 */
	String getValue();

	/**
	 * <p>
	 * Creates a matchers that will match the specified input URI against this
	 * pattern.
	 * </p>
	 * 
	 * @param uri The URI to match
	 * 
	 * @return a URI matcher
	 */
	URIMatcher matcher(String uri);
	
	/**
	 * <p>
	 * Determines whether the set of URIs matched by the specified URI pattern is included in the set of URIs matched by this URI pattern.
	 * </p>
	 * 
	 * <p>
	 * Considering A: the set of URIs matched by the specified URI pattern, and B: the set of URIs matched by this URI pattern, this method should return:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link URIPattern.Inclusion#INCLUDED} when A is included in B</li>
	 * <li>{@link URIPattern.Inclusion#DISJOINT} when A and B are disjoint</li>
	 * <li>{@link URIPattern.Inclusion#INDETERMINATE} when it wasn't possible to determine inclusion with certainty or if the difference between A and B is not empty</li>
	 * </ul>
	 * 
	 * <p>
	 * Implementations can choose to focus on specific URI components such as path in which case this method must return {@link URIPattern.PatternMatch#INDETERMINATE} when other components are
	 * considered. Parameter names must also be ignored by implementations (eg. /{x} should be considered as equivalent to /{y}).
	 * </p>
	 * 
	 * @param pattern a URI pattern
	 * 
	 * @return a pattern inclusion state specifying whether the specified pattern is included in this pattern
	 */
	URIPattern.Inclusion includes(URIPattern pattern);
	
}

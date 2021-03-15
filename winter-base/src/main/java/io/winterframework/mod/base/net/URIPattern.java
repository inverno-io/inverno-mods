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
package io.winterframework.mod.base.net;

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
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see URIBuilder
 * @see URIMatcher
 */
public interface URIPattern {

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
	 * Creates a matchers that will match the specified input URI against this
	 * pattern.
	 * </p>
	 * 
	 * @param uri The URI to match
	 * 
	 * @return a URI matcher
	 */
	URIMatcher matcher(String uri);
}

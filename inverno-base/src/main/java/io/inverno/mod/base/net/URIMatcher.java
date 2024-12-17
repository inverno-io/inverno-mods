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

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * <p>
 * An engine that performs match operations on a URI by interpreting a URI pattern.
 * </p>
 * 
 * <p>
 * A URI matcher is created from a URI pattern by invoking the pattern's {@link URIPattern#matcher(String)} method. Once created a matcher can be used to determine whether the input URI matches
 * against the URI pattern and extract parameters from the input URI assuming parameters have been specified when building the URI pattern.
 * </p>
 * 
 * <p>
 * For instance the following shows how to match an absolute path against {@code /book/{id}} pattern and extract the {@code id} path parameter:
 * </p>
 * 
 * <pre>{@code
 * URIPattern pathPattern = URIs.uri("/book/{id}", URIs.Option.PARAMETERIZED).buildPathPattern();
 * 
 * URIMatcher matcher = pathPattern.matcher("/book/123");
 * if (matcher.matches()) {
 *     String id = matcher.getParameterValue("id").get();
 * }
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see URIPattern
 * @see URIBuilder
 */
public interface URIMatcher extends Comparable<URIMatcher> {

	/**
	 * <p>
	 * Determines whether the input URI matches against the URI pattern.
	 * </p>
	 * 
	 * <p>
	 * If the match succeeds then parameters if any can be obtained via {@link URIMatcher#getParameterValue(String) getParameterValue} and {@link URIMatcher#getParameters() getParameters} methods.
	 * 
	 * @return true if the input URI matches, false otherwise
	 */
	boolean matches();

	/**
	 * <p>
	 * Return the underlying JDK matcher.
	 * </p>
	 * 
	 * @return a matcher
	 */
	Matcher getMatcher();

	/**
	 * <p>
	 * Return the value of the parameter with the specified name extracted from the input URI.
	 * </p>
	 * 
	 * <p>
	 * The {@link URIMatcher#matches() matches} method must be called first to extract parameters assuming the input URI matches the pattern.
	 * </p>
	 * 
	 * @param name the parameter name
	 * 
	 * @return an optional providing the value
	 */
	Optional<String> getParameterValue(String name);

	/**
	 * <p>
	 * Returns a map containing the parameters extracted from the input URI.
	 * </p>
	 * 
	 * <p>
	 * The {@link URIMatcher#matches() matches} method must be called first to extract parameters assuming the input URI matches the pattern.
	 * </p>
	 * 
	 * @return a map of parameters
	 */
	Map<String, String> getParameters();
}

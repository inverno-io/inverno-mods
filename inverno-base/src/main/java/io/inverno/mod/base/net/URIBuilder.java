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

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A URI builder is used for creating URIs as defined by
 * <a href="https://tools.ietf.org/html/rfc3986">RFC 3986 Section </a>.
 * </p>
 * 
 * <p>
 * The URI components provided to the builder can be parameterized with
 * parameters of the form <code>{{@literal <name>[:<pattern>]}}</code> when the
 * component allows it. It allows to create URI templates used to generate
 * contextual URIs or create URI patterns to match particular URIs.
 * </p>
 * 
 * <p>The following is a complete example of parameterized URI:</p>
 * 
 * <blockquote><pre>
 * {scheme}://{userinfo}@{host}:{port}/a/{segment}?parameter={parameter}#{fragment})
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see URIs
 * @see URIPattern
 */
public interface URIBuilder extends Cloneable {

	/**
	 * <p>
	 * Sets the scheme component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.1">RFC 3986 Section
	 * 3.1</a>.
	 * </p>
	 * 
	 * @param scheme the scheme to set
	 * 
	 * @return the URI builder
	 */
	URIBuilder scheme(String scheme);

	/**
	 * <p>
	 * Sets the user information component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.2.1">RFC 3986 Section
	 * 3.2.1</a>.
	 * </p>
	 * 
	 * @param userInfo the user information to set
	 * 
	 * @return the URI builder
	 */
	URIBuilder userInfo(String userInfo);

	/**
	 * <p>
	 * Sets the host component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.2.2">RFC 3986 Section
	 * 3.2.2</a>.
	 * </p>
	 * 
	 * @param host the host to set
	 * 
	 * @return the URI builder
	 */
	URIBuilder host(String host);

	/**
	 * <p>
	 * Sets the port component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.2.3">RFC 3986 Section
	 * 3.2.3</a>.
	 * </p>
	 * 
	 * @param port the port to set
	 * 
	 * @return the URI builder
	 */
	URIBuilder port(Integer port);

	/**
	 * <p>
	 * Sets the port component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.2.3">RFC 3986 Section
	 * 3.2.3</a>.
	 * </p>
	 * 
	 * @param port the port to set
	 * 
	 * @return the URI builder
	 */
	URIBuilder port(String port);

	/**
	 * <p>
	 * Appends the specified path as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.3">RFC 3986 Section
	 * 3.3</a> ignoring the trailing slash
	 * </p>
	 * 
	 * @param path the path to append
	 * 
	 * @return the URI builder
	 */
	default URIBuilder path(String path) {
		return this.path(path, true);
	}

	/**
	 * <p>
	 * Appends the specified path as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.3">RFC 3986 Section
	 * 3.3</a> ignoring or not the trailing slash.
	 * </p>
	 * 
	 * @param path                the path to append
	 * @param ignoreTrailingSlash true to ignore the trailing slash
	 * 
	 * @return the URI builder
	 */
	URIBuilder path(String path, boolean ignoreTrailingSlash);

	/**
	 * <p>
	 * Appends the specified path segment as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.3">RFC 3986 Section
	 * 3.3</a>
	 * </p>
	 * 
	 * @param segment the path segment to append
	 * 
	 * @return the URI builder
	 */
	URIBuilder segment(String segment);

	/**
	 * <p>
	 * Clears the path component.
	 * </p>
	 * 
	 * @return the URI builder
	 */
	URIBuilder clearPath();

	/**
	 * <p>
	 * Appends a query parameter component with specified name and value as defined
	 * by <a href="https://tools.ietf.org/html/rfc3986#section-3.4">RFC 3986 Section
	 * 3.4</a>.
	 * </p>
	 * 
	 * @param name  the query parameter name
	 * @param value the query parameter value
	 * 
	 * @return the URI builder
	 */
	URIBuilder queryParameter(String name, String value);

	/**
	 * <p>
	 * Clears the query component.
	 * </p>
	 * 
	 * @return the URI builder
	 */
	URIBuilder clearQuery();

	/**
	 * <p>
	 * Sets the fragment component as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-3.5">RFC 3986 Section
	 * 3.5</a>.
	 * </p>
	 * 
	 * @param fragment the fragment to set
	 * 
	 * @return the URI builder
	 */
	URIBuilder fragment(String fragment);

	/**
	 * <p>
	 * Returns the list of parameters specified in all builder's URI components.
	 * </p>
	 * 
	 * @return a list of parameter names
	 */
	List<String> getParameterNames();

	/**
	 * <p>
	 * Returns the list of parameters specified in the builder's path component.
	 * </p>
	 * 
	 * @return a list of parameter names
	 */
	List<String> getPathParameterNames();

	/**
	 * <p>
	 * Returns a map containing the query parameters after replacing the parameters
	 * with the string representation of the specified values.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values an array of values to replace the components parameters
	 * 
	 * @return a map of query parameters grouped by name
	 * @throws URIBuilderException if there was an error building the query
	 *                             parameters
	 */
	Map<String, List<String>> getQueryParameters(Object... values) throws URIBuilderException;

	/**
	 * <p>
	 * Returns a map containing the query parameters after replacing the parameters
	 * with the string representation of the specified values.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values a map of values to replace the components parameters
	 * 
	 * @return a map of query parameters grouped by name
	 * @throws URIBuilderException if there was an error building the query
	 *                             parameters
	 */
	Map<String, List<String>> getQueryParameters(Map<String, ?> values) throws URIBuilderException;

	/**
	 * <p>
	 * Returns a map containing the raw query parameters.
	 * </p>
	 * 
	 * @return a map of query parameters grouped by name
	 * @throws URIBuilderException if there was an error building the query
	 *                             parameters
	 */
	Map<String, List<String>> getRawQueryParameters() throws URIBuilderException;

	/**
	 * <p>
	 * Builds the URI from the builder's URI components after replacing the
	 * parameters with the string representation of the specified values escaping
	 * slash in the path segment components.
	 * </p>
	 * 
	 * @param values an array of values to replace the components parameters
	 * 
	 * @return a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	default URI build(Object... values) throws URIBuilderException {
		return this.build(values, true);
	}

	/**
	 * <p>
	 * Builds the URI from the builder's URI components after replacing the
	 * parameters with the string representation of the specified values escaping or
	 * not slash in the path segment components.
	 * </p>
	 * 
	 * @param values      an array of values to replace the components parameters
	 * @param escapeSlash true to escape slash in the path segment components
	 * 
	 * @return a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	URI build(Object[] values, boolean escapeSlash) throws URIBuilderException;

	/**
	 * <p>
	 * Builds the URI from the builder's URI components after replacing the
	 * parameters with the string representation of the specified values.
	 * </p>
	 * 
	 * @param values a map of values to replace the components parameters
	 * 
	 * @return a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	default URI build(Map<String, ?> values) throws URIBuilderException {
		return this.build(values, true);
	}

	/**
	 * <p>
	 * Builds the URI from the builder's URI components after replacing the
	 * parameters with the string representation of the specified values escaping or
	 * not slash in the path segment components.
	 * </p>
	 * 
	 * @param values      a map of values to replace the components parameters
	 * @param escapeSlash true to escape slash in the path segment components
	 * 
	 * @return a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	URI build(Map<String, ?> values, boolean escapeSlash) throws URIBuilderException;

	/**
	 * <p>
	 * Builds the string representation of the URI from the builder's URI components
	 * after replacing the parameters with the string representation of the
	 * specified values escaping slash in path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values an array of values to replace the components parameters
	 * 
	 * @return a string representation of a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	default String buildString(Object... values) throws URIBuilderException {
		return this.buildString(values, true);
	}

	/**
	 * <p>
	 * Builds the string representation of the URI from the builder's URI components
	 * after replacing the parameters with the string representation of the
	 * specified values escaping or not slash in the path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values      an array of values to replace the components parameters
	 * @param escapeSlash true to escape slash in the path segment components
	 * 
	 * @return a string representation of a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildString(Object[] values, boolean escapeSlash) throws URIBuilderException;

	/**
	 * <p>
	 * Builds the string representation of the URI from the builder's URI components
	 * after replacing the parameters with the string representation of the
	 * specified values escaping slash in path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values a map of values to replace the components parameters
	 * 
	 * @return a string representation of a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	default String buildString(Map<String, ?> values) throws URIBuilderException {
		return this.buildString(values, true);
	}

	/**
	 * <p>
	 * Builds the string representation of the URI from the builder's URI components
	 * after replacing the parameters with the string representation of the
	 * specified values escaping or not slash in the path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values      a map of values to replace the components parameters
	 * @param escapeSlash true to escape slash in the path segment components
	 * 
	 * @return a string representation of a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildString(Map<String, ?> values, boolean escapeSlash) throws URIBuilderException;

	/**
	 * <p>
	 * Builds the raw string representation of the URI from the builder's URI
	 * components.
	 * </p>
	 * 
	 * @return a raw string representation of a URI
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildRawString() throws URIBuilderException;

	/**
	 * <p>
	 * Builds the path component string from the builder's URI path segment
	 * components after replacing the parameters with the string representation of
	 * the specified values escaping slash in path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values an array of values to replace the path components parameters
	 * 
	 * @return a path string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	default String buildPath(Object... values) {
		return this.buildPath(values, true);
	}

	/**
	 * <p>
	 * Builds the path component string from the builder's URI path segment
	 * components after replacing the parameters with the string representation of
	 * the specified values escaping or not slash in the path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values      an array of values to replace the path components
	 *                    parameters
	 * @param escapeSlash true to escape slash in the path segment components
	 * 
	 * @return a path string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildPath(Object[] values, boolean escapeSlash);

	/**
	 * <p>
	 * Builds the path component string from the builder's URI path segment
	 * components after replacing the parameters with the string representation of
	 * the specified values escaping slash in path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values a map of values to replace the components parameters
	 * 
	 * @return a path string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildPath(Map<String, ?> values);

	/**
	 * <p>
	 * Builds the path component string from the builder's URI path segment
	 * components after replacing the parameters with the string representation of
	 * the specified values escaping or not slash in the path segment components.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values      a map of values to replace the components parameters
	 * @param escapeSlash true to escape slashes, false otherwise
	 * 
	 * @return a path string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildPath(Map<String, ?> values, boolean escapeSlash);

	/**
	 * <p>
	 * Builds the path component raw string from the builder's path segment
	 * components.
	 * </p>
	 * 
	 * @return a raw path string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildRawPath() throws URIBuilderException;

	/**
	 * <p>
	 * Builds the query component string from the builder's URI query parameter
	 * components after replacing the parameters with the string representation of
	 * the specified values.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values an array of values to replace the path components parameters
	 * 
	 * @return a query string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildQuery(Object... values) throws URIBuilderException;

	/**
	 * <p>
	 * Builds the query component string from the builder's URI query parameter
	 * components after replacing the parameters with the string representation of
	 * the specified values.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values a map of values to replace the components parameters
	 * 
	 * @return a query string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildQuery(Map<String, ?> values) throws URIBuilderException;

	/**
	 * <p>
	 * Builds the query component raw string from the builder's query parameter
	 * components.
	 * </p>
	 * 
	 * @return a raw query string
	 * @throws URIBuilderException if there was an error building the URI
	 */
	String buildRawQuery() throws URIBuilderException;

	/**
	 * <p>
	 * Builds a URI pattern to exactly match builder's URI.
	 * </p>
	 * 
	 * @return a URI pattern
	 */
	default URIPattern buildPattern() {
		return this.buildPattern(false);
	}

	/**
	 * <p>
	 * Builds a URI pattern to match builder's URI including trailing slash or not.
	 * </p>
	 * 
	 * @param matchTrailingSlash true to match trailing slash
	 * 
	 * @return a URI pattern
	 */
	URIPattern buildPattern(boolean matchTrailingSlash);

	/**
	 * <p>
	 * Builds a URI pattern to exactly match builder's URI path component.
	 * </p>
	 * 
	 * @return a URI pattern
	 */
	default URIPattern buildPathPattern() {
		return this.buildPathPattern(false);
	}

	/**
	 * <p>
	 * Builds a URI pattern to match builder's URI path component including trailing
	 * slash or not.
	 * </p>
	 * 
	 * @param matchTrailingSlash true to match trailing slash
	 * 
	 * @return a URI pattern
	 */
	URIPattern buildPathPattern(boolean matchTrailingSlash);
	
	/**
	 * <p>
	 * Clones the URI builder.
	 * </p>
	 * 
	 * @return A copy of the URI builder
	 */
	URIBuilder clone();
}

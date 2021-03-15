/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.http.server;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.header.Header;

/**
 * <p>
 * Represents the HTTP trailers of a server response in a server exchange as
 * defined by <a href="https://tools.ietf.org/html/rfc7230#section-4.3">RFC 7230
 * Section 4.3</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Response
 */
public interface ResponseTrailers {
	
	/**
	 * <p>
	 * Adds a trailer with the specified name and value.
	 * </p>
	 * 
	 * @param name  the trailer name
	 * @param value the trailer value
	 * 
	 * @return the response trailers
	 */
	ResponseTrailers add(CharSequence name, CharSequence value);
	
	/**
	 * <p>
	 * Adds the specified trailers.
	 * </p>
	 * 
	 * @param trailers the trailers to add
	 * 
	 * @return the response trailers
	 */
	ResponseTrailers add(Header... trailers);
	
	/**
	 * <p>
	 * Sets the value of the trailer with the specified name.
	 * <p>
	 * 
	 * @param name  the trailer name
	 * @param value the trailer value
	 * 
	 * @return the response trailers
	 */
	ResponseTrailers set(CharSequence name, CharSequence value);
	
	/**
	 * <p>
	 * Sets the specified trailers.
	 * </p>
	 * 
	 * @param trailers the trailers to set
	 * 
	 * @return the response trailers
	 */
	ResponseTrailers set(Header... trailers);
	
	/**
	 * <p>
	 * Removes the trailers with the specified names.
	 * </p>
	 * 
	 * @param names the names of the trailers to remove
	 * 
	 * @return the response trailers
	 */
	ResponseTrailers remove(CharSequence... names);
	
	/**
	 * <p>
	 * Determines whether a trailer with the specified name is present.
	 * </p>
	 * 
	 * @param name a trailer name
	 * 
	 * @return true if a trailer is present, false otherwise
	 */
	boolean contains(CharSequence name);
	
	/**
	 * <p>
	 * Determines whether a trailer with the specified name and value is present.
	 * </p>
	 * 
	 * @param name  a trailer name
	 * @param value a trailer value
	 * 
	 * @return true if a trailer is present, false otherwise
	 */
	boolean contains(CharSequence name, CharSequence value);
	
	/**
	 * <p>
	 * Returns the names of the trailers in the response.
	 * </p>
	 * 
	 * @return a list of trailer names
	 */
	Set<String> getNames();
	
	/**
	 * <p>
	 * Returns the value of the trailer with the specified name.
	 * </p>
	 * 
	 * <p>
	 * If there are multiple trailers with the same name, this method returns the
	 * first one.
	 * </p>
	 * 
	 * @param name a trailer name
	 * 
	 * @return an optional returning the value of the trailer or an empty optional if
	 *         there's no trailer with the specified name
	 */
	Optional<String> get(CharSequence name);

	/**
	 * <p>
	 * Returns the values of all trailers with the specified name.
	 * </p>
	 * 
	 * @param name a trailer name
	 * 
	 * @return a list of trailer values or an empty list if there's no trailer with
	 *         the specified name
	 */
	List<String> getAll(CharSequence name);

	/**
	 * <p>
	 * Returns all trailers in the response.
	 * </p>
	 * 
	 * @return a list of trailer entries or an empty list if there's no trailer
	 */
	List<Map.Entry<String, String>> getAll();

	/**
	 * <p>
	 * Decodes and returns the trailer with the specified name.
	 * </p>
	 * 
	 * <p>
	 * If there are multiple trailers with the same name, this method returns the
	 * first one.
	 * </p>
	 * 
	 * @param <T>  the decoded trailer type
	 * @param name a trailer name
	 * 
	 * @return an optional returning the decoded trailer or an empty optional if
	 *         there's no trailer with the specified name
	 */
	<T extends Header> Optional<T> getHeader(CharSequence name);

	/**
	 * <p>
	 * Decodes and returns all trailers with the specified name.
	 * </p>
	 * 
	 * @param <T>  the decoded trailer type
	 * @param name a trailer name
	 * 
	 * @return a list of trailer values or an empty list if there's no trailer with
	 *         the specified name
	 */
	<T extends Header> List<T> getAllHeader(CharSequence name);

	/**
	 * <p>
	 * Decodes and returns all trailers in the response.
	 * </p>
	 * 
	 * @return a list of trailers or an empty list if there's no trailer
	 */
	List<Header> getAllHeader();

	/**
	 * <p>
	 * Returns the trailer with the specified name as a parameter.
	 * </p>
	 * 
	 * <p>
	 * If there are multiple trailers with the same name, this method returns the
	 * first one.
	 * </p>
	 * 
	 * @param name a trailer name
	 * 
	 * @return an optional returning the parameter or an empty optional if there's
	 *         no trailer with the specified name
	 */
	Optional<Parameter> getParameter(CharSequence name);

	/**
	 * <p>
	 * Returns all trailers with the specified name as parameters.
	 * </p>
	 * 
	 * @param name a trailer name
	 * 
	 * @return a list of parameters or an empty list if there's no trailer with the
	 *         specified name
	 */
	List<Parameter> getAllParameter(CharSequence name);

	/**
	 * <p>
	 * Returns all trailers in the response as parameters.
	 * </p>
	 * 
	 * @return a list of parameters or an empty list if there's no trailer
	 */
	List<Parameter> getAllParameter();
}

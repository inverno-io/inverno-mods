/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.base;

import io.inverno.mod.http.base.header.Header;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Represents immutable inbound HTTP headers.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface InboundHeaders {
	
	/**
	 * <p>
	 * Determines whether a header with the specified name is present.
	 * </p>
	 * 
	 * @param name a header name
	 * 
	 * @return true if a header is present, false otherwise
	 */
	boolean contains(CharSequence name);

	/**
	 * <p>
	 * Determines whether a header with the specified name and value is present.
	 * </p>
	 * 
	 * @param name  a header name
	 * @param value a header value
	 * 
	 * @return true if a header is present, false otherwise
	 */
	boolean contains(CharSequence name, CharSequence value);
	
	/**
	 * <p>
	 * Returns the names of the headers specified in the HTTP message.
	 * </p>
	 * 
	 * @return the header names
	 */
	Set<String> getNames();

	/**
	 * <p>
	 * Returns the value of the header with the specified name.
	 * </p>
	 *
	 * <p>
	 * If there are multiple headers with the same name, the first one is returned.
	 * </p>
	 *
	 * @param name a header name
	 *
	 * @return an optional returning the value of the header or an empty optional if there's no header with the specified name
	 */
	Optional<String> get(CharSequence name);
	
	/**
	 * <p>
	 * Returns the values of all headers with the specified name.
	 * </p>
	 *
	 * @param name a header name
	 *
	 * @return a list of header values or an empty list if there's no header with the specified name
	 */
	List<String> getAll(CharSequence name);
	
	/**
	 * <p>
	 * Returns all headers specified in the HTTP message.
	 * </p>
	 * 
	 * @return a list of header entries or an empty list if there's no header
	 */
	List<Map.Entry<String, String>> getAll();
	
	/**
	 * <p>
	 * Returns the header with the specified name as a parameter.
	 * </p>
	 *
	 * <p>
	 * If there are multiple headers with the same name, the first one is returned.
	 * </p>
	 *
	 * @param name a header name
	 *
	 * @return an optional returning the parameter or an empty optional if there's no header with the specified name
	 */
	Optional<Parameter> getParameter(CharSequence name);
	
	/**
	 * <p>
	 * Returns all headers with the specified name as parameters.
	 * </p>
	 *
	 * @param name a header name
	 *
	 * @return a list of parameters or an empty list if there's no header with the specified name
	 */
	List<Parameter> getAllParameter(CharSequence name);
	
	/**
	 * <p>
	 * Returns all headers specified in the HTTP message as parameters.
	 * </p>
	 * 
	 * @return a list of parameters or an empty list if there's no header
	 */
	List<Parameter> getAllParameter();
	
	/**
	 * <p>
	 * Decodes and returns the header with the specified name.
	 * </p>
	 *
	 * <p>
	 * If there are multiple headers with the same name, the first one is returned.
	 * </p>
	 *
	 * @param <T>  the decoded header type
	 * @param name a header name
	 *
	 * @return an optional returning the decoded header or an empty optional if there's no header with the specified name
	 */
	<T extends Header> Optional<T> getHeader(CharSequence name);

	/**
	 * <p>
	 * Decodes and returns all headers with the specified name.
	 * </p>
	 *
	 * @param <T>  the decoded header type
	 * @param name a header name
	 *
	 * @return a list of header values or an empty list if there's no header with the specified name
	 */
	<T extends Header> List<T> getAllHeader(CharSequence name);
	
	/**
	 * <p>
	 * Decodes and returns all headers specified in the HTTP message.
	 * </p>
	 * 
	 * @return a list of headers or an empty list if there's no header
	 */
	List<Header> getAllHeader();
}

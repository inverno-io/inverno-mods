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
import io.winterframework.mod.http.base.Status;
import io.winterframework.mod.http.base.header.Header;
import io.winterframework.mod.http.base.header.Headers;

/**
 * <p>
 * Represents the HTTP headers of a server response in a server exchange as
 * defined by <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230
 * Section 3.2</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Response
 */
public interface ResponseHeaders {

	/**
	 * <p>
	 * Sets the response HTTP status.
	 * </p>
	 * 
	 * @param status the HTTP status
	 * 
	 * @return the response headers
	 */
	ResponseHeaders status(Status status);

	/**
	 * <p>
	 * Sets the response HTTP status code
	 * </p>
	 * 
	 * @param status the HTTP status code
	 * 
	 * @return the response headers
	 */
	ResponseHeaders status(int status);

	/**
	 * <p>
	 * Sets the response content type header field value.
	 * </p>
	 * 
	 * @param contentType the content type
	 * 
	 * @return the response headers
	 */
	ResponseHeaders contentType(String contentType);

	/**
	 * <p>
	 * Sets the response content length.
	 * </p>
	 * 
	 * @param length the content length
	 * 
	 * @return the response headers
	 */
	ResponseHeaders contentLength(long length);

	/**
	 * <p>
	 * Adds a header with the specified name and value.
	 * </p>
	 * 
	 * @param name  the header name
	 * @param value the header value
	 * 
	 * @return the response headers
	 */
	ResponseHeaders add(CharSequence name, CharSequence value);

	/**
	 * <p>
	 * Adds the specified headers.
	 * </p>
	 * 
	 * @param headers the headers to add
	 * 
	 * @return the response headers
	 */
	ResponseHeaders add(Header... headers);

	/**
	 * <p>
	 * Sets the value of the header with the specified name.
	 * <p>
	 * 
	 * @param name  the header name
	 * @param value the header value
	 * 
	 * @return the response headers
	 */
	ResponseHeaders set(CharSequence name, CharSequence value);

	/**
	 * <p>
	 * Sets the specified headers.
	 * </p>
	 * 
	 * @param headers the headers to set
	 * 
	 * @return the response headers
	 */
	ResponseHeaders set(Header... headers);

	/**
	 * <p>
	 * Removes the headers with the specified names.
	 * </p>
	 * 
	 * @param names the names of the headers to remove
	 * 
	 * @return the response headers
	 */
	ResponseHeaders remove(CharSequence... names);

	/**
	 * <p>
	 * Returns the response HTTP status.
	 * </p>
	 * 
	 * @return the response status
	 */
	Status getStatus();

	/**
	 * <p>
	 * Returns the response HTTP status code.
	 * </p>
	 * 
	 * @return the response status code
	 */
	int getStatusCode();

	/**
	 * <p>
	 * Returns the response content type header field value.
	 * </p>
	 * 
	 * @return an optional returning the content type or an empty optional if
	 *         there's no content type header
	 */
	Optional<String> getContentType();

	/**
	 * <p>
	 * Decodes and returns the response content type header.
	 * </p>
	 * 
	 * @return an optional returning the content type header or an empty optional if
	 *         there's no content type header
	 */
	Optional<Headers.ContentType> getContentTypeHeader();

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
	 * Returns the names of the headers in the response.
	 * </p>
	 * 
	 * @return a list of header names
	 */
	Set<String> getNames();

	/**
	 * <p>
	 * Returns the value of the header with the specified name.
	 * </p>
	 * 
	 * <p>
	 * If there are multiple headers with the same name, this method returns the
	 * first one.
	 * </p>
	 * 
	 * @param name a header name
	 * 
	 * @return an optional returning the value of the header or an empty optional if
	 *         there's no header with the specified name
	 */
	Optional<String> get(CharSequence name);

	/**
	 * <p>
	 * Returns the values of all headers with the specified name.
	 * </p>
	 * 
	 * @param name a header name
	 * 
	 * @return a list of header values or an empty list if there's no header with
	 *         the specified name
	 */
	List<String> getAll(CharSequence name);

	/**
	 * <p>
	 * Returns all headers in the response.
	 * </p>
	 * 
	 * @return a list of header entries or an empty list if there's no header
	 */
	List<Map.Entry<String, String>> getAll();

	/**
	 * <p>
	 * Decodes and returns the header with the specified name.
	 * </p>
	 * 
	 * <p>
	 * If there are multiple headers with the same name, this method returns the
	 * first one.
	 * </p>
	 * 
	 * @param <T>  the decoded header type
	 * @param name a header name
	 * 
	 * @return an optional returning the decoded header or an empty optional if
	 *         there's no header with the specified name
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
	 * @return a list of header values or an empty list if there's no header with
	 *         the specified name
	 */
	<T extends Header> List<T> getAllHeader(CharSequence name);

	/**
	 * <p>
	 * Decodes and returns all headers in the response.
	 * </p>
	 * 
	 * @return a list of headers or an empty list if there's no header
	 */
	List<Header> getAllHeader();

	/**
	 * <p>
	 * Returns the header with the specified name as a parameter.
	 * </p>
	 * 
	 * <p>
	 * If there are multiple headers with the same name, this method returns the
	 * first one.
	 * </p>
	 * 
	 * @param name a header name
	 * 
	 * @return an optional returning the parameter or an empty optional if there's
	 *         no header with the specified name
	 */
	Optional<Parameter> getParameter(CharSequence name);

	/**
	 * <p>
	 * Returns all headers with the specified name as parameters.
	 * </p>
	 * 
	 * @param name a header name
	 * 
	 * @return a list of parameters or an empty list if there's no header with the
	 *         specified name
	 */
	List<Parameter> getAllParameter(CharSequence name);

	/**
	 * <p>
	 * Returns all headers in the response as parameters.
	 * </p>
	 * 
	 * @return a list of parameters or an empty list if there's no header
	 */
	List<Parameter> getAllParameter();
}

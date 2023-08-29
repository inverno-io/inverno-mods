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

import io.inverno.mod.http.base.header.Headers;

/**
 * <p>
 * Represents immutable inbound HTTP response headers.
 * </p>
 * 
 * <p>
 * This extends the {@link InboundHeaders} to expose response specific information like response status, content type, content length and set-cookies.
 * </p>
 * 
 * <p>
 * An inbound response is received by a client in a client exchange.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface InboundResponseHeaders extends InboundHeaders {

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
	 * Returns the content type header field value of the request.
	 * </p>
	 * 
	 * @return the content type or null
	 */
	String getContentType();
	
	/**
	 * <p>
	 * Decodes and returns the content type header of the request.
	 * </p>
	 * 
	 * @return the decoded content type header
	 */
	Headers.ContentType getContentTypeHeader();
	
	/**
	 * <p>
	 * Returns the content length of the request.
	 * </p>
	 * 
	 * @return the content length or null
	 */
	Long getContentLength();
	
	/**
	 * <p>
	 * Returns the set-cookies defined in the response.
	 * <p>
	 * 
	 * @return the set-cookies
	 */
	InboundSetCookies cookies();
}

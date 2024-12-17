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
 * Represents immutable inbound HTTP request headers.
 * </p>
 * 
 * <p>
 * This extends the {@link InboundHeaders} to expose request specific information like content type, content length and cookies.
 * </p>
 * 
 * <p>
 * An inbound request is received by a server in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface InboundRequestHeaders extends InboundHeaders {
	
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
	 * @return the decoded content type header or null
	 */
	Headers.ContentType getContentTypeHeader();

	/**
	 * <p>
	 * Returns the accept header field value of the request.
	 * </p>
	 *
	 * @return the accept header value or null
	 */
	String getAccept();

	/**
	 * <p>
	 * Decodes and returns the accept header of the request.
	 * Returns the accept header field value of the request.
	 * </p>
	 *
	 * @return the decoded accept header or null
	 */
	Headers.Accept getAcceptHeader();
	
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
	 * Returns the cookies defined in the request.
	 * </p>
	 * 
	 * @return the cookies
	 */
	InboundCookies cookies();
}

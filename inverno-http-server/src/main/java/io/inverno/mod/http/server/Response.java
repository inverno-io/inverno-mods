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
package io.inverno.mod.http.server;

import java.util.function.Consumer;

/**
 * <p>
 * Represents a server response in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Exchange
 */
public interface Response {

	/**
	 * <p>
	 * Determines whether the response headers have been sent to the client.
	 * </p>
	 *
	 * <p>
	 * Any attempts to specify new headers in the response, once headers haven been sent to the client, will result in an
	 * {@link IllegalStateException} being thrown.
	 * </p>
	 *
	 * @return true if headers have been sent, false otherwise
	 */
	boolean isHeadersWritten();

	/**
	 * <p>
	 * Returns the HTTP headers to send in the response.
	 * </p>
	 * 
	 * @return the headers
	 */
	ResponseHeaders headers();
	
	/**
	 * <p>
	 * Configures the HTTP headers to send in the response.
	 * </p>
	 *
	 * @param headersConfigurer a response headers configurer
	 *
	 * @return the response
	 *
	 * @throws IllegalStateException if response headers have already been sent to the client
	 */
	Response headers(Consumer<ResponseHeaders> headersConfigurer) throws IllegalStateException;
	
	/**
	 * <p>
	 * Returns the HTTP trailers to send in the response.
	 * </p>
	 * 
	 * @return the trailers
	 */
	ResponseTrailers trailers();
	
	/**
	 * <p>
	 * Configures the HTTP trailers to send in the response.
	 * </p>
	 * 
	 * @param trailersConfigurer a response trailers configurer
	 * 
	 * @return the response
	 */
	Response trailers(Consumer<ResponseTrailers> trailersConfigurer);
	
	/**
	 * <p>
	 * Configures the cookies to set in the response.
	 * </p>
	 *
	 * @param cookiesConfigurer a response cookies configurer
	 *
	 * @return the response
	 *
	 * @throws IllegalStateException if response headers have already been sent to the client
	 */
	Response cookies(Consumer<ResponseCookies> cookiesConfigurer) throws IllegalStateException;
	
	/**
	 * <p>
	 * Sends an interim 100 continue response to the client so it can send the rest of the request.
	 * </p>
	 *
	 * <p>
	 * This method should only be used when the request contains header {@code expect: 100-continue}.
	 * </p>
	 *
	 * @return the response
	 */
	Response sendContinue();
	
	/**
	 * <p>
	 * Returns the response body used to produce response payload.
	 * </p>
	 * 
	 * @return a response body
	 */
	ResponseBody body();
}

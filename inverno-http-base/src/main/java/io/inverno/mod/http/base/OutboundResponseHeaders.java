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

import java.util.function.Consumer;

/**
 * <p>
 * Represents mutable outbound HTTP response headers.
 * </p>
 * 
 * <p>
 * This extends the {@link OutboundHeaders} to expose response specific information like response status, content type, content length and set-cookies.
 * </p>
 * 
 * <p>
 * An outbound response is sent by a server in a server exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface OutboundResponseHeaders extends InboundResponseHeaders, OutboundHeaders<OutboundResponseHeaders> {

	/**
	 * <p>
	 * Sets the response HTTP status.
	 * </p>
	 * 
	 * @param status the HTTP status
	 * 
	 * @return the response headers
	 */
	OutboundResponseHeaders status(Status status);

	/**
	 * <p>
	 * Sets the response HTTP status code
	 * </p>
	 * 
	 * @param status the HTTP status code
	 * 
	 * @return the response headers
	 */
	OutboundResponseHeaders status(int status);
	
	/**
	 * <p>
	 * Sets the response content type header field value.
	 * </p>
	 * 
	 * @param contentType the content type
	 * 
	 * @return the response headers
	 */
	OutboundResponseHeaders contentType(String contentType);

	/**
	 * <p>
	 * Sets the response content length.
	 * </p>
	 * 
	 * @param contentLength the content length
	 * 
	 * @return the response headers
	 */
	OutboundResponseHeaders contentLength(long contentLength);
	
	/**
	 * <p>
	 * Sets the response set-cookies.
	 * </p>
	 * 
	 * @param cookiesConfigurer an outbound set-cookies configurer
	 * 
	 * @return the response headers
	 */
	OutboundResponseHeaders cookies(Consumer<OutboundSetCookies> cookiesConfigurer);
}

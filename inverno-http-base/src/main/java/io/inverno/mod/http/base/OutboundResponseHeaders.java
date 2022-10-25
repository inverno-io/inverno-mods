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
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
	
	OutboundResponseHeaders cookies(Consumer<OutboundSetCookies> cookiesConfigurer);
}

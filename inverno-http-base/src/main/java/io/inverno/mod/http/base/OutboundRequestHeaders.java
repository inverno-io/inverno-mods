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
 * Represents mutable outbound HTTP request headers.
 * </p>
 * 
 * <p>
 * This extends the {@link OutboundHeaders} to expose request specific information like content type, content length and cookies.
 * </p>
 * 
 * <p>
 * An outbound request is sent by a client in a client exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface OutboundRequestHeaders extends InboundRequestHeaders, OutboundHeaders<OutboundRequestHeaders> {

	/**
	 * <p>
	 * Sets the request content type header field value.
	 * </p>
	 * 
	 * @param contentType the content type
	 * 
	 * @return the request headers
	 */
	OutboundRequestHeaders contentType(String contentType);

	/**
	 * <p>
	 * Sets the request accept header field value.
	 * </p>
	 *
	 * @param accept the accept header value
	 *
	 * @return the request headers
	 */
	OutboundRequestHeaders accept(String accept);

	/**
	 * <p>
	 * Sets the request content length.
	 * </p>
	 * 
	 * @param contentLength the content length
	 * 
	 * @return the request headers
	 */
	OutboundRequestHeaders contentLength(long contentLength);

	/**
	 * <p>
	 * Sets the request cookies.
	 * </p>
	 * 
	 * @param cookiesConfigurer an outbound cookies configurer
	 * 
	 * @return the request headers
	 */
	OutboundRequestHeaders cookies(Consumer<OutboundCookies> cookiesConfigurer);
}

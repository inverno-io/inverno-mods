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

/**
 * <p>
 * Base HTTP response for representing client or server responses.
 * </p>
 *
 * <p>
 * It exposes content information following HTTP response message format as defined by <a href="https://tools.ietf.org/html/rfc7230">RFC7230</a> and
 * <a href="https://tools.ietf.org/html/rfc7231">RFC7231</a>.
 * </p>
 *
 * <p>
 * Considering a client exchange, where the response is received by the client from the server, implementation shall provide methods to access HTTP response content. Considering a server exchange,
 * where the response is provided and sent from the server to the client, implementation shall provide methods to set HTTP response content.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface BaseResponse {

	/**
	 * <p>
	 * Returns the HTTP headers of the response.
	 * </p>
	 * 
	 * @return the response headers
	 */
	InboundResponseHeaders headers();
	
	/**
	 * <p>
	 * Returns the HTTP response trailer headers.
	 * </p>
	 * 
	 * <p>
	 * Note that in a client exchange trailers are only received after the response body as a result this method shall return {@code null} when invoked before the response data publisher completes.
	 * </p>
	 * 
	 * @return the response trailer headers or null
	 */
	InboundHeaders trailers();
}

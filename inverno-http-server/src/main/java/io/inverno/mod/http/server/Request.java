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

import io.inverno.mod.http.base.BaseRequest;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import java.net.SocketAddress;
import java.util.Optional;

/**
 * <p>
 * Represents a client request in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Exchange
 */
public interface Request extends BaseRequest {

	/**
	 * <p>
	 * Returns the cookies sent in the request.
	 * </p>
	 * 
	 * @return the cookies
	 * 
	 * @deprecated use {@link InboundRequestHeaders#cookies()}
	 */
	@Deprecated
	InboundCookies cookies();
	
	/**
	 * <p>
	 * Returns the name of the scheme used to send the request (eg. http, https...).
	 * </p>
	 * 
	 * @return the name of the scheme
	 */
	String getScheme();
	
	/**
	 * <p>
	 * Returns the socket address of the interface on which the request was received.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getLocalAddress();
	
	/**
	 * <p>
	 * Returns the socket address of the client or last proxy that sent the request.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getRemoteAddress();
	
	/**
	 * <p>
	 * Returns the request body used to consume request payload.
	 * </p>
	 *
	 * @return an optional returning the request body or an empty optional if the request has no payload
	 */
	Optional<? extends RequestBody> body();
}

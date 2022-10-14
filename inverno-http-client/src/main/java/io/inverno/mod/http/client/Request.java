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

package io.inverno.mod.http.client;

import io.inverno.mod.http.base.HttpVersion;
import java.net.SocketAddress;
import java.util.Optional;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface Request extends BaseRequest {
	
	boolean isHeadersWritten();
	
	HttpVersion getProtocol();
	
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
	
	Optional<? extends RequestBody> body();
}

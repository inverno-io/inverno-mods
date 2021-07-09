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

import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a server exchange between a client and a server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Request
 * @see Response
 */
public interface Exchange {

	/**
	 * <p>
	 * Returns the request part of the exchange.
	 * <p>
	 * 
	 * @return the request part
	 */
	Request request();
	
	/**
	 * <p>
	 * Returns the response part of the exchange.
	 * <p>
	 * 
	 * @return the response part
	 */
	Response response();
	
	/**
	 * <p>
	 * Returns the exchange finalizer which completes once the exchange is fully
	 * processed.
	 * </p>
	 * 
	 * <p>
	 * A exchange is considered fully processed when the response has been fully
	 * sent to the client or following an error.
	 * </p>
	 * 
	 * @return a finalizer mono
	 */
	Mono<Void> finalizer();
}

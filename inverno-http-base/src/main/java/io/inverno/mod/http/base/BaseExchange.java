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
 * Base HTTP exchange (request/response) for representing server or client exchanges.
 * </p>
 *
 * <p>
 * An HTTP exchange basically comes down to a {@link #request() request} and a {@link #response() response} being exchanged between a client and a server. In a client exchange, the client sends the
 * request to the server and receives the response from the server. In a server exchange, the server receives the request from the client and sends the response to the client.
 * </p>
 *
 * <p>
 * A {@link #context() context} is also attached to every exchange to provide contextual data and operation during during the processing of the exchange.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see ExchangeContext
 * 
 * @param <A> The exchange context type 
 * @param <B> The request type
 * @param <C> The response type
 */
public interface BaseExchange<A extends ExchangeContext, B, C> {

	/**
	 * <p>
	 * Returns the protocol of the exchange (eg. HTTP/1.1).
	 * </p>
	 * 
	 * @return the protocol
	 */
	HttpVersion getProtocol();
	
	/**
	 * <p>
	 * Returns the context attached to the exchange.
	 * </p>
	 * 
	 * @return the exchange context or null
	 */
	A context();
	
	/**
	 * <p>
	 * Returns the request part of the exchange.
	 * </p>
	 * 
	 * @return the request part
	 */
	B request();
	
	/**
	 * <p>
	 * Returns the response part of the exchange.
	 * </p>
	 * 
	 * @return the response part
	 */
	C response();
}

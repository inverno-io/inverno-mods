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

import io.netty.handler.codec.http2.Http2Error;
import java.util.Optional;
import reactor.core.publisher.Flux;

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
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see ExchangeContext
 * 
 * @param <A> The exchange context type 
 * @param <B> The request type
 * @param <C> The response type
 */
public interface BaseExchange<A extends ExchangeContext, B extends BaseRequest, C> {

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
	
	/**
	 * <p>
	 * Resets the exchange with a {@code CANCEL(0x8)} code.
	 * </p>
	 * 
	 * @see #reset(long)
	 */
	default void reset() {
		this.reset(Http2Error.CANCEL.code());
	}
	
	/**
	 * <p>
	 * Resets the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method is typically invoked to cancel the exchange (i.e. stop processing) by immediately cancelling active subscription to the response data publisher.
	 * </p>
	 * 
	 * <p>
	 * Behaviour depends on the HTTP protocol version, an HTTP/1.x connection might be closed right away if is in an incomplete state when the request and/or the response were partially received
	 * and/or sent whereas with HTTP/2 connection this will just reset the stream bound to the exchange with the specified code as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7540#section-7">RFC 7540 Section 7</a>.
	 * </p>
	 * 
	 * @param code a code
	 */
	void reset(long code);
	
	/**
	 * <p>
	 * Returns the error that caused the cancellation of the exchange.
	 * </p>
	 *
	 * <p>
	 * Exchange disposal resulting or not from an error is usually caught in the request data publisher which can be bound to the response data publisher in which case, errors can be caught by
	 * defining {@link Flux#doOnError(java.util.function.Consumer) } on the response data publisher. When the request data publisher is not consumed, terminated or not bound to the response data
	 * publisher, the response data publisher can be disposed without error, this method gives access to the error when intercepting the exchange disposal by defining 
	 * {@link Flux#doOnCancel(java.lang.Runnable) } on the response data publisher.
	 * </p>
	 *
	 * @return an optional returning the cancel error or an empty optional if the exchange hasn't been disposed or if it completed successfully (i.e. when both request and response data publishers
	 *         completed successfully)
	 */
	Optional<Throwable> getCancelCause();
}

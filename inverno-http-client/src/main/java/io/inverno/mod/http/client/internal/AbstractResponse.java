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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.InboundResponseHeaders;

/**
 * <p>
 * Base {@link HttpConnectionResponse} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the HTTP response headers type
 * @param <B> the HTTP response body type
 * @param <C> the HTTP response trailers type
 * @param <D> the originating response trailers type
 */
public abstract class AbstractResponse<A extends InboundResponseHeaders, B extends AbstractResponseBody, C extends InboundHeaders, D> implements HttpConnectionResponse {

	private final A headers;
	/**
	 * The response body.
	 */
	protected final B body;
	
	private C trailers;

	/**
	 * <p>
	 * Creates a base response.
	 * </p>
	 *
	 * @param headers the response headers
	 * @param body    the response body
	 */
	protected AbstractResponse(A headers, B body) {
		this.headers = headers;
		this.body = body;
	}
	
	/**
	 * <p>
	 * Disposes the response.
	 * </p>
	 * 
	 * <p>
	 * This method disposes the response body.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	public final void dispose(Throwable cause) {
		this.body.dispose(cause);
	}
	
	@Override
	public A headers() {
		return this.headers;
	}
	
	@Override
	public B body() {
		return this.body;
	}

	@Override
	public C trailers() {
		return this.trailers;
	}
	
	/**
	 * <p>
	 * Sets the response trailers.
	 * </p>
	 * 
	 * <p>
	 * This is invoked by the connection when response trailers are received.
	 * </p>
	 * 
	 * @param trailers the originating trailers
	 */
	public final void setTrailers(D trailers) {
		this.trailers = this.createTrailers(trailers);
	}
	
	/**
	 * <p>
	 * Creates the HTTP response trailers from the originating trailers.
	 * </p>
	 * 
	 * @param trailers the originating trailers
	 * 
	 * @return the HTTP response trailers
	 */
	protected abstract C createTrailers(D trailers);
}

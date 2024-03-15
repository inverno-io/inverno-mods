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
import io.inverno.mod.http.client.ResponseBody;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base {@link HttpConnectionResponse} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public abstract class AbstractResponse implements HttpConnectionResponse {

	private final InboundResponseHeaders responseHeaders;
	
	protected InboundHeaders responseTrailers;
	
	private HttpConnectionResponseBody body;

	/**
	 * <p>
	 * Creates a base response.
	 * </p>
	 * 
	 * @param responseHeaders the response headers
	 */
	protected AbstractResponse(InboundResponseHeaders responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	
	@Override
	public InboundResponseHeaders headers() {
		return this.responseHeaders;
	}
	
	@Override
	public ResponseBody body() {
		if(this.body == null) {
			this.body = new HttpConnectionResponseBody();
		}
		return this.body;
	}

	@Override
	public InboundHeaders trailers() {
		return this.responseTrailers;
	}
	
	/**
	 * <p>
	 * Returns the response payload data sink.
	 * </p>
	 * 
	 * @return the payload data sink
	 */
	public Sinks.Many<ByteBuf> data() {
		return ((HttpConnectionResponseBody)this.body()).dataSink;
	}
	
	/**
	 * <p>
	 * Disposes the response.
	 * </p>
	 * 
	 * <p>
	 * This method delegates to {@link #dispose(java.lang.Throwable) } with a null error.
	 * </p>
	 */
	public void dispose() {
		this.dispose(null);
	}
	
	/**
	 * <p>
	 * Disposes the response with the specified error.
	 * </p>
	 * 
	 * <p>
	 * This method cleans up response outstanding resources, it especially drains received data if needed.
	 * </p>
	 * 
	 * <p>
	 * A non-null error indicates that the enclosing exchange did not complete successfully and that the error should be emitted when possible (e.g. in the response data publisher).
	 * </p>
	 * 
	 * @param error an error or null
	 * 
	 * @see HttpConnectionResponseBody#dispose(java.lang.Throwable) 
	 */
	public void dispose(Throwable error) {
		if(this.body != null) {
			this.body.dispose(error);
		}
	}
}

/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.Response;
import java.util.function.Consumer;

/**
 * <p>
 * Base {@link Response} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the response headers type
 * @param <B> the response body type
 * @param <C> the response trailers type
 * @param <D> the response type
 */
public abstract class AbstractResponse<A extends AbstractResponseHeaders<?>, B extends AbstractResponseBody, C extends AbstractResponseTrailers<?>, D extends AbstractResponse<A, B, C, D>> implements Response {
	
	/**
	 * The header service.
	 */
	protected final HeaderService headerService;
	/**
	 * The parameter converter.
	 */
	protected final ObjectConverter<String> parameterConverter;
	/**
	 * Flag indicating whether the request is a {@link Method#HEAD} request.
	 */
	protected final boolean head;
	/**
	 * The response headers.
	 */
	protected final A headers;
	
	/**
	 * The transferer length.
	 */
	protected int transferedLength;

	/**
	 * <p>
	 * Creates a base response.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param head               true to indicate a {@code HEAD} request, false otherwise
	 * @param headers            the response headers
	 */
	public AbstractResponse(HeaderService headerService, ObjectConverter<String> parameterConverter, boolean head, A headers) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.head = head;
		this.headers = headers;
	}
	
	/**
	 * <p>
	 * Sends the response.
	 * </p>
	 *
	 * <p>
	 * This method must execute on the connection event loop and subscribe to the response body data publisher to generate and send the response body. In case of an {@code HEAD} request, an empty
	 * response with headers only shall be sent.
	 * </p>
	 */
	public abstract void send();
	
	/**
	 * <p>
	 * Disposes the response.
	 * </p>
	 * 
	 * <p>
	 * This method shall simply cancel any active subscription.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	public abstract void dispose(Throwable cause);
	
	@Override
	public boolean isHeadersWritten() {
		return this.headers.isWritten();
	}

	@Override
	public int getTransferedLength() {
		return this.transferedLength;
	}
	
	@Override
	public D headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException {
		if(this.headers.isWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		if(headersConfigurer != null) {
			headersConfigurer.accept(this.headers);
		}
		return (D)this;
	}

	@Override
	public A headers() {
		return this.headers;
	}
	
	@Override
	public D trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) {
		if(this.trailers().isWritten()) {
			throw new IllegalStateException("Trailers already written");
		}
		
		if(trailersConfigurer != null) {
			trailersConfigurer.accept(this.trailers());
		}
		return (D)this;
	}
	
	@Override
	public abstract C trailers();

	@Override
	public abstract D sendContinue();

	@Override
	public abstract B body();
}

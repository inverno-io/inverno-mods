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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Response;
import java.util.function.Consumer;
import io.inverno.mod.http.client.InterceptableResponse;

/**
 * <p>
 * Generic {@link InterceptableResponse} implementation.
 * </p>
 * 
 * <p>
 * This implementation also implements {@link Response} which allows it to act as a proxy for the actual response once it has been received from the endpoint. This allows to expose the response
 * actually received to interceptors which is required to be able to intercept the response payload for instance. The {@link #setReceivedResponse(io.inverno.mod.http.client.Response) } shall be
 * invoked to make this instance delegates to the received response. At this point the interceptable response should become immutable.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class GenericInterceptableResponse implements InterceptableResponse, Response {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;

	private GenericInterceptableResponseHeaders responseHeaders;
	private GenericInterceptableResponseTrailers responseTrailers;
	private GenericInterceptableResponseBody responseBody;
	
	private Response receivedResponse;
	
	/**
	 * <p>
	 * Creates a generic interceptable response.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public GenericInterceptableResponse(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	/**
	 * <p>
	 * Validates that the actual response has not been received yet and that we can perform mutable operations.
	 * </p>
	 * 
	 * @throws IllegalArgumentException if the response has been received
	 */
	private void checkNotReceived() throws IllegalArgumentException {
		if(this.receivedResponse != null) {
			throw new IllegalStateException("Response already received");
		}
	}
	
	/**
	 * <p>
	 * Injects the actual response received from the endpoint.
	 * </p>
	 * 
	 * @param receivedResponse the response received from the endpoint
	 */
	public void setReceivedResponse(Response receivedResponse) {
		this.receivedResponse = receivedResponse;
		if(this.responseBody != null) {
			this.responseBody.setReceivedResponseBody(this.receivedResponse.body());
		}
	}
	
	@Override
	public InboundResponseHeaders headers() {
		if(this.receivedResponse != null) {
			return this.receivedResponse.headers();
		}
		if(this.responseHeaders == null) {
			this.responseHeaders = new GenericInterceptableResponseHeaders(headerService, parameterConverter);
		}
		return this.responseHeaders;
	}
	
	@Override
	public InterceptableResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException {
		this.checkNotReceived();
		if(this.responseHeaders == null) {
			this.responseHeaders = new GenericInterceptableResponseHeaders(headerService, parameterConverter);
		}
		headersConfigurer.accept(this.responseHeaders);
		return this;
	}

	@Override
	public GenericInterceptableResponseBody body() {
		if(this.responseBody == null) {
			this.responseBody = new GenericInterceptableResponseBody(this);
			if(this.receivedResponse != null) {
				this.responseBody.setReceivedResponseBody(this.receivedResponse.body());
			}
		}
		return this.responseBody;
	}

	@Override
	public InboundHeaders trailers() {
		if(this.receivedResponse != null) {
			return this.receivedResponse.trailers();
		}
		if(this.responseTrailers == null) {
			this.responseTrailers = new GenericInterceptableResponseTrailers(headerService, parameterConverter);
		}
		return this.responseTrailers;
	}

	@Override
	public InterceptableResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) throws IllegalStateException {
		this.checkNotReceived();
		if(this.responseTrailers == null) {
			this.responseTrailers = new GenericInterceptableResponseTrailers(headerService, parameterConverter);
		}
		trailersConfigurer.accept(this.responseTrailers);
		return this;
	}

	@Override
	public boolean isReceived() {
		return this.receivedResponse != null;
	}
}

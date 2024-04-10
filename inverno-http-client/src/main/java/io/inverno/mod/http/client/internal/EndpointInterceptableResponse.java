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
 * An {@link InterceptableResponse} used to specify a response or instrument the actual response in an {@link ExchangeInterceptor}.
 * </p>
 * 
 * <p>
 * This implementation also implements {@link Response} which allows it to act as a proxy for the actual response once it has been received from the endpoint. This allows to expose the response
 * actually received to interceptors which is required to be able to intercept the response payload for instance. The {@link #setConnectedResponse(io.inverno.mod.http.client.Response) } is invoked to
 * make this instance delegates to the received response. At this point the interceptable response becomes immutable.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointInterceptableResponse implements InterceptableResponse, Response {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;

	private EndpointInterceptableResponseHeaders responseHeaders;
	private EndpointInterceptableResponseTrailers responseTrailers;
	private EndpointInterceptableResponseBody responseBody;
	
	private Response connectedResponse;
	
	/**
	 * <p>
	 * Creates an interceptable response.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public EndpointInterceptableResponse(HeaderService headerService, ObjectConverter<String> parameterConverter) {
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
	private void checkNotConnected() throws IllegalArgumentException {
		if(this.connectedResponse != null) {
			throw new IllegalStateException("Response already received");
		}
	}
	
	/**
	 * <p>
	 * Injects the actual response either received from the endpoint or specified in the interceptor when the request was intercepted (i.e. interceptor returned an empty publisher).
	 * </p>
	 * 
	 * @param connectedResponse the retained response
	 */
	public void setConnectedResponse(Response connectedResponse) {
		if(this.responseBody != null) {
			this.responseBody.setConnectedResponseBody(connectedResponse.body());
		}
		this.connectedResponse = connectedResponse;
	}
	
	@Override
	public InboundResponseHeaders headers() {
		if(this.connectedResponse != null && this.connectedResponse != this) {
			return this.connectedResponse.headers();
		}
		if(this.responseHeaders == null) {
			this.responseHeaders = new EndpointInterceptableResponseHeaders(headerService, parameterConverter);
		}
		return this.responseHeaders;
	}
	
	@Override
	public InterceptableResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException {
		this.checkNotConnected();
		if(headersConfigurer != null) {
			headersConfigurer.accept((OutboundResponseHeaders)this.headers());
		}
		return this;
	}

	@Override
	public EndpointInterceptableResponseBody body() {
		if(this.responseBody == null) {
			this.responseBody = new EndpointInterceptableResponseBody(this);
		}
		return this.responseBody;
	}

	@Override
	public InboundHeaders trailers() {
		if(this.connectedResponse != null && this.connectedResponse != this) {
			return this.connectedResponse.trailers();
		}
		if(this.responseTrailers == null) {
			this.responseTrailers = new EndpointInterceptableResponseTrailers(headerService, parameterConverter);
		}
		return this.responseTrailers;
	}

	@Override
	public InterceptableResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) throws IllegalStateException {
		this.checkNotConnected();
		if(trailersConfigurer != null) {
			trailersConfigurer.accept((OutboundHeaders<?>)this.trailers());
		}
		return this;
	}

	@Override
	public boolean isReceived() {
		return this.connectedResponse != null  && this.connectedResponse != this;
	}
}

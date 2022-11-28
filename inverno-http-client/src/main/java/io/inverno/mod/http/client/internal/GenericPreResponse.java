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
import io.inverno.mod.http.client.PreResponse;
import io.inverno.mod.http.client.PreResponseBody;
import io.inverno.mod.http.client.Response;
import java.util.function.Consumer;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericPreResponse implements PreResponse, Response {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;

	private GenericPreResponseHeaders responseHeaders;
	private GenericPreResponseTrailers responseTrailers;
	private GenericPreResponseBody responseBody;
	
	public GenericPreResponse(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	@Override
	public InboundResponseHeaders headers() {
		if(this.responseHeaders == null) {
			this.responseHeaders = new GenericPreResponseHeaders(headerService, parameterConverter);
		}
		return this.responseHeaders;
	}
	
	@Override
	public PreResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException {
		if(this.responseHeaders == null) {
			this.responseHeaders = new GenericPreResponseHeaders(headerService, parameterConverter);
		}
		headersConfigurer.accept(this.responseHeaders);
		return this;
	}

	@Override
	public GenericPreResponseBody body() {
		if(this.responseBody == null) {
			this.responseBody = new GenericPreResponseBody(this);
		}
		return this.responseBody;
	}

	@Override
	public InboundHeaders trailers() {
		if(this.responseTrailers == null) {
			this.responseTrailers = new GenericPreResponseTrailers(headerService, parameterConverter);
		}
		return this.responseTrailers;
	}

	@Override
	public PreResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) {
		if(this.responseTrailers == null) {
			this.responseTrailers = new GenericPreResponseTrailers(headerService, parameterConverter);
		}
		trailersConfigurer.accept(this.responseTrailers);
		return this;
	}
}

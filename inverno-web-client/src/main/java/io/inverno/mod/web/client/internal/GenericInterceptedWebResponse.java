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
package io.inverno.mod.web.client.internal;

import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.client.InterceptedResponse;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.client.InterceptedWebResponse;
import io.inverno.mod.web.client.InterceptedWebResponseBody;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link InterceptedWebResponse} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericInterceptedWebResponse implements InterceptedWebResponse {

	private final DataConversionService dataConversionService;
	private final InterceptedResponse response;

	private InterceptedWebResponseBody responseBody;

	/**
	 * <p>
	 * Creates a generic intercepted Web response.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param response              the originating intercepted response
	 */
	public GenericInterceptedWebResponse(DataConversionService dataConversionService, InterceptedResponse response) {
		this.response = response;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public InterceptedWebResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException {
		this.response.headers(headersConfigurer);
		return this;
	}

	@Override
	public InterceptedWebResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) throws IllegalStateException {
		this.response.trailers(trailersConfigurer);
		return this;
	}

	@Override
	public InterceptedWebResponseBody body() {
		if(this.responseBody == null) {
			this.responseBody = new GenericInterceptedWebResponseBody(this.dataConversionService, this.response);
		}
		return this.responseBody;
	}

	@Override
	public boolean isReceived() {
		return this.response.isReceived();
	}

	@Override
	public InboundResponseHeaders headers() {
		return this.response.headers();
	}

	@Override
	public InboundHeaders trailers() {
		return this.response.trailers();
	}
}

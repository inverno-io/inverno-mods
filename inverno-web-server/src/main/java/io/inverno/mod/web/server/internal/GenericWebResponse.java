/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.web.server.WebResponse;
import io.inverno.mod.web.server.WebResponseBody;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link WebResponse} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebResponseBody
 */
class GenericWebResponse implements WebResponse {

	private final Response response;
	
	private final WebResponseBody responseBody;
	
	/**
	 * <p>
	 * Creates a generic web response with the specified underlying response and data conversion service.
	 * </p>
	 *
	 * @param response              the underlying response
	 * @param dataConversionService the data conversion service
	 */
	public GenericWebResponse(Response response, DataConversionService dataConversionService) {
		this.response = response;
		this.responseBody = new GenericWebResponseBody(this, response.body(), dataConversionService);
	}

	@Override
	public boolean isHeadersWritten() {
		return this.response.isHeadersWritten();
	}

	@Override
	public int getTransferedLength() {
		return this.response.getTransferedLength();
	}

	@Override
	public InboundResponseHeaders headers() {
		return this.response.headers();
	}

	@Override
	public WebResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException {
		this.response.headers(headersConfigurer);
		return this;
	}

	@Override
	public InboundHeaders trailers() {
		return this.response.trailers();
	}

	@Override
	public WebResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) {
		this.response.trailers(trailersConfigurer);
		return this;
	}

	@Override
	public Response sendContinue() {
		this.response.sendContinue();
		return this;
	}

	@Override
	public WebResponseBody body() {
		return this.responseBody;
	}
}

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
import io.inverno.mod.http.client.Response;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.client.WebResponse;
import io.inverno.mod.web.client.WebResponseBody;

/**
 * <p>
 * Generic {@link WebResponse} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebResponse implements WebResponse {

	private final Response response;
	private final DataConversionService dataConversionService;

	private WebResponseBody webResponseBody;

	/**
	 * <p>
	 * Creates a generic Web response.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param response              the originating response
	 */
	public GenericWebResponse(DataConversionService dataConversionService, Response response) {
		this.response = response;
		this.dataConversionService = dataConversionService;
	}
	
	@Override
	public WebResponseBody body() {
		if(this.webResponseBody == null) {
			this.webResponseBody = new GenericWebResponseBody(this.dataConversionService, this.response);
		}
		return this.webResponseBody;
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

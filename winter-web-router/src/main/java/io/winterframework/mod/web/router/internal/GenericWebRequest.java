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
package io.winterframework.mod.web.router.internal;

import java.net.SocketAddress;
import java.util.Optional;

import io.winterframework.mod.web.router.WebRequest;
import io.winterframework.mod.web.router.WebRequestBody;
import io.winterframework.mod.web.server.Request;
import io.winterframework.mod.web.server.RequestCookies;
import io.winterframework.mod.web.server.RequestHeaders;
import io.winterframework.mod.web.server.RequestParameters;

/**
 * @author jkuhn
 *
 */
public class GenericWebRequest implements WebRequest {

	private Request request;
	
	private Optional<WebRequestBody> webRequestBody;
	
	private BodyConversionService bodyConversionService;
	
	public GenericWebRequest(Request request, BodyConversionService bodyConversionService) {
		this.request = request;
		this.bodyConversionService = bodyConversionService;
	}

	@Override
	public RequestHeaders headers() {
		return this.request.headers();
	}

	@Override
	public RequestParameters parameters() {
		return this.request.parameters();
	}

	@Override
	public RequestCookies cookies() {
		return this.request.cookies();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.request.getRemoteAddress();
	}

	@Override
	public Optional<WebRequestBody> body() {
		if(this.webRequestBody == null) {
			this.webRequestBody = this.request.body().map(requestBody -> new GenericWebRequestBody(this, requestBody, this.bodyConversionService));
		}
		return this.webRequestBody;
	}
}
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

import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.router.WebRequest;
import io.winterframework.mod.web.router.WebRequestBody;
import io.winterframework.mod.web.server.RequestBody;

/**
 * @author jkuhn
 *
 */
public class GenericWebRequestBody implements WebRequestBody {

	private WebRequest request;
	
	private RequestBody requestBody;
	
	private BodyConversionService bodyConversionService;
	
	private Decoder<?> decoder;
	
	public GenericWebRequestBody(WebRequest request, RequestBody requestBody, BodyConversionService bodyConversionService) {
		this.request = request;
		this.requestBody = requestBody;
		this.bodyConversionService = bodyConversionService;
	}

	@Override
	public Raw raw() throws IllegalStateException {
		return this.requestBody.raw();
	}

	@Override
	public Multipart multipart() throws IllegalStateException {
		return this.requestBody.multipart();
	}

	@Override
	public UrlEncoded urlEncoded() throws IllegalStateException {
		return this.requestBody.urlEncoded();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> Decoder<A> decoder(Class<A> type) {
		if(this.decoder == null) {
			this.decoder = this.request.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
				.map(contentType -> this.bodyConversionService.createDecoder(this.requestBody.raw(), contentType.getMediaType(), type))
				.orElseThrow(() -> new InternalServerErrorException("Empty media type"));
		}
		return (Decoder<A>) this.decoder;
	}
}

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

import java.lang.reflect.Type;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.router.RequestDataDecoder;
import io.winterframework.mod.web.router.WebPart;
import io.winterframework.mod.web.router.WebRequest;
import io.winterframework.mod.web.router.WebRequestBody;
import io.winterframework.mod.web.server.RequestBody;
import io.winterframework.mod.web.server.RequestData;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public class GenericWebRequestBody implements WebRequestBody {

	private final WebRequest request;
	
	private final RequestBody requestBody;
	
	private final DataConversionService dataConversionService;
	
	public GenericWebRequestBody(WebRequest request, RequestBody requestBody, DataConversionService dataConversionService) {
		this.request = request;
		this.requestBody = requestBody;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public RequestData<ByteBuf> raw() throws IllegalStateException {
		return this.requestBody.raw();
	}

	@Override
	public Multipart<WebPart> multipart() throws IllegalStateException {
		return new WebMultipart();
	}

	@Override
	public UrlEncoded urlEncoded() throws IllegalStateException {
		return this.requestBody.urlEncoded();
	}

	@Override
	public <A> RequestDataDecoder<A> decoder(Class<A> type) {
		return this.decoder((Type)type);
	}
	
	@Override
	public <A> RequestDataDecoder<A> decoder(Type type) {
		return this.request.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<A>createDecoder(this.requestBody.raw(), contentType.getMediaType(), type))
			.orElseThrow(() -> new InternalServerErrorException("Empty media type"));
	}
	
	private class WebMultipart implements Multipart<WebPart> {

		@Override
		public Publisher<WebPart> stream() {
			return Flux.from(GenericWebRequestBody.this.requestBody.multipart().stream()).map(part -> new GenericWebPart(part, GenericWebRequestBody.this.dataConversionService));
		}
	}
}

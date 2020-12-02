/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.internal.server;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public class GenericRequestBody implements RequestBody {
	
	private Flux<ByteBuf> data;
	private Optional<Headers.ContentType> contentType;
	
	private RequestBody.Raw rawBody;
	private RequestBody.UrlEncoded urlEncodedBody;
	private RequestBody.Multipart multipartBody;
	
	private RequestBodyDecoder<Parameter> urlEncodedBodyDecoder;
	private RequestBodyDecoder<Part> multipartBodyDecoder;

	public GenericRequestBody(Optional<Headers.ContentType> contentType, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder, Flux<ByteBuf> data) {
		this.contentType = contentType;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		this.data = data;
	}
	
	@Override
	public RequestBody.Raw raw() {
		if(this.urlEncodedBody != null || this.multipartBody != null) {
			throw new IllegalStateException("You can't mix regular post request with multipart"); // TODO find proper message
		}
		if(this.rawBody == null) {
			this.rawBody = new GenericRawRequestBody();
		}
		return this.rawBody;
	}

	@Override
	public RequestBody.Multipart multipart() {
		if(this.rawBody != null || this.urlEncodedBody != null) {
			throw new IllegalStateException("You can't mix regular post request with multipart"); // TODO find proper message
		}
		if(this.multipartBody == null) {
			this.multipartBody = new GenericMultipartRequestBody(this.multipartBodyDecoder.decode(this.data, this.contentType.orElse(null)));
		}
		return this.multipartBody;
	}

	@Override
	public RequestBody.UrlEncoded urlEncoded() {
		if(this.rawBody != null || this.multipartBody != null) {
			throw new IllegalStateException("You can't mix regular post request with multipart"); // TODO find proper message
		}
		if(this.urlEncodedBody == null) {
			this.urlEncodedBody = new GenericUrlEncodedRequestBody(this.urlEncodedBodyDecoder.decode(this.data, this.contentType.orElse(null)));
		}
		return this.urlEncodedBody;
	}
	
	private class GenericRawRequestBody implements RequestBody.Raw {

		@Override
		public Flux<ByteBuf> data() {
			return GenericRequestBody.this.data;
		}
	}

	private class GenericUrlEncodedRequestBody implements RequestBody.UrlEncoded {

		private Flux<Parameter> parameters;
		
		public GenericUrlEncodedRequestBody(Flux<Parameter> parameters) {
			this.parameters = parameters;
		}

		@Override
		public Flux<Parameter> parameters() {
			return this.parameters;
		}
	}
	
	private class GenericMultipartRequestBody implements RequestBody.Multipart {

		private Flux<Part> parts;
		
		public GenericMultipartRequestBody(Flux<Part> parts) {
			this.parts = parts;
		}

		@Override
		public Flux<Part> parts() {
			return this.parts;
		}
	}
}

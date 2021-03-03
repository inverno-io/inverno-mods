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
package io.winterframework.mod.http.server.internal;

import java.util.Optional;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.Part;
import io.winterframework.mod.http.server.RequestBody;
import io.winterframework.mod.http.server.RequestData;
import io.winterframework.mod.http.server.internal.multipart.MultipartDecoder;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public class GenericRequestBody implements RequestBody {
	
	private Flux<ByteBuf> data;
	private Optional<Headers.ContentType> contentType;
	
	private RequestData<ByteBuf> rawData;
	private RequestBody.UrlEncoded urlEncodedData;
	private RequestBody.Multipart<Part> multipartData;
	
	private MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	private MultipartDecoder<Part> multipartBodyDecoder;

	public GenericRequestBody(Optional<Headers.ContentType> contentType, MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder, Flux<ByteBuf> data) {
		this.contentType = contentType;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		this.data = data;
	}
	
	@Override
	public RequestData<ByteBuf> raw() {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.rawData == null) {
			this.rawData = new GenericRequestBodyRawData();
		}
		return this.rawData;
	}

	@Override
	public RequestBody.Multipart<Part> multipart() {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.multipartData == null) {
			this.multipartData = new GenericRequestBodyMultipartData(this.multipartBodyDecoder.decode(this.data, this.contentType.orElse(null)));
		}
		return this.multipartData;
	}

	@Override
	public RequestBody.UrlEncoded urlEncoded() {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.urlEncodedData == null) {
			this.urlEncodedData = new GenericRequestBodyUrlEncodedData(this.urlEncodedBodyDecoder.decode(this.data, this.contentType.orElse(null)));
		}
		return this.urlEncodedData;
	}
	
	private class GenericRequestBodyRawData implements RequestData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return GenericRequestBody.this.data;
		}
	}

	private class GenericRequestBodyUrlEncodedData implements RequestBody.UrlEncoded {

		private Publisher<Parameter> parameters;
		
		public GenericRequestBodyUrlEncodedData(Publisher<Parameter> parameters) {
			this.parameters = parameters;
		}

		@Override
		public Publisher<Parameter> stream() {
			return this.parameters;
		}
	}
	
	private class GenericRequestBodyMultipartData implements RequestBody.Multipart<Part> {

		private Publisher<Part> parts;
		
		public GenericRequestBodyMultipartData(Publisher<Part> parts) {
			this.parts = parts;
		}

		@Override
		public Publisher<Part> stream() {
			return this.parts;
		}
	}
}

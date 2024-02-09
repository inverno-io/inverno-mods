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

import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.web.server.InboundDataDecoder;
import io.inverno.mod.web.server.WebPart;
import io.inverno.mod.web.server.WebRequest;
import io.inverno.mod.web.server.WebRequestBody;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * <p>
 * Generic {@link WebRequestBody} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRequestBody implements WebRequestBody {

	private final WebRequest request;
	
	private final RequestBody requestBody;
	
	private final DataConversionService dataConversionService;
	
	/**
	 * <p>
	 * creates a generic web request body with the specified underlying request and request body and data conversion service.
	 * </p>
	 *
	 * @param request               the underlying request
	 * @param requestBody           the unedrlying request body
	 * @param dataConversionService the data conversion service
	 */
	public GenericWebRequestBody(WebRequest request, RequestBody requestBody, DataConversionService dataConversionService) {
		this.request = request;
		this.requestBody = requestBody;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public WebRequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		this.requestBody.transform(transformer);
		return this;
	}

	@Override
	public InboundData<ByteBuf> raw() throws IllegalStateException {
		return this.requestBody.raw();
	}

	@Override
	public InboundData<CharSequence> string() throws IllegalStateException {
		return this.requestBody.string();
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
	public <A> InboundDataDecoder<A> decoder(Class<A> type) {
		return this.decoder((Type)type);
	}
	
	@Override
	public <A> InboundDataDecoder<A> decoder(Type type) {
		return this.request.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> {
				try {
					return this.dataConversionService.<A>createDecoder(this.requestBody.raw(), contentType.getMediaType(), type);
				} 
				catch (NoConverterException e) {
					throw new UnsupportedMediaTypeException("No converter found for media type: " + e.getMediaType(), e);
				}
			})
			.orElseThrow(() -> new BadRequestException("Empty media type"));
	}
	
	/**
	 * <p>
	 * a {@link Multipart} implementation that supports part body decoding.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see WebPart
	 */
	private class WebMultipart implements Multipart<WebPart> {

		@Override
		public Publisher<WebPart> stream() {
			return Flux.from(GenericWebRequestBody.this.requestBody.multipart().stream()).map(part -> new GenericWebPart(part, GenericWebRequestBody.this.dataConversionService));
		}
	}
}

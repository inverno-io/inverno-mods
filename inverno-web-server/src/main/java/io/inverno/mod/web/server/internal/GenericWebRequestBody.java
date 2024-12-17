/*
 * Copyright 2021 Jeremy Kuhn
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
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.web.base.InboundDataDecoder;
import io.inverno.mod.web.base.MissingConverterException;
import io.inverno.mod.web.server.WebPart;
import io.inverno.mod.web.server.WebRequestBody;
import io.inverno.mod.web.server.internal.multipart.GenericWebPart;
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
public class GenericWebRequestBody implements WebRequestBody {

	private final ServerDataConversionService dataConversionService;
	private final Request request;
	private final RequestBody requestBody;

	/**
	 * <p>
	 * Creates a generic Web request body.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param request               the originating request
	 * @param requestBody           the originating request body
	 */
	public GenericWebRequestBody(ServerDataConversionService dataConversionService, Request request, RequestBody requestBody) {
		this.dataConversionService = dataConversionService;
		this.request = request;
		this.requestBody = requestBody;
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
				catch (MissingConverterException e) {
					throw new UnsupportedMediaTypeException("No converter found for media type: " + e.getMediaType(), e);
				}
			})
			.orElseThrow(() -> new BadRequestException("Empty media type"));
	}

	@Override
	public Multipart<WebPart> multipart() throws IllegalStateException {
		return new WebMultipart();
	}

	@Override
	public UrlEncoded urlEncoded() throws IllegalStateException {
		return this.requestBody.urlEncoded();
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
			return Flux.from(GenericWebRequestBody.this.requestBody.multipart().stream()).map(part -> new GenericWebPart(GenericWebRequestBody.this.dataConversionService, part));
		}
	}
}

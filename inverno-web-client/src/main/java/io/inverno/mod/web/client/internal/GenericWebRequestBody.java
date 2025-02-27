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

import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.RequestBody;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.inverno.mod.web.client.WebPartFactory;
import io.inverno.mod.web.client.WebRequestBody;
import io.inverno.mod.web.client.internal.multipart.GenericWebPart;
import io.inverno.mod.web.client.internal.multipart.GenericWebPartFactory;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebRequestBody} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebRequestBody implements WebRequestBody {

	private final Request request;
	private final DataConversionService dataConversionService;

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
	public GenericWebRequestBody(DataConversionService dataConversionService, Request request, RequestBody requestBody) {
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
	public RequestBody before(Mono<Void> before) {
		this.requestBody.before(before);
		return this;
	}

	@Override
	public RequestBody after(Mono<Void> after) {
		this.requestBody.after(after);
		return this;
	}

	@Override
	public void empty() {
		this.requestBody.empty();
	}

	@Override
	public OutboundData<ByteBuf> raw() {
		return this.requestBody.raw();
	}

	@Override
	public <T extends CharSequence> OutboundData<T> string() {
		return this.requestBody.string();
	}

	@Override
	public Resource resource() {
		return this.requestBody.resource();
	}

	@Override
	public UrlEncoded<Parameter.Factory> urlEncoded() {
		return this.requestBody.urlEncoded();
	}

	@Override
	public Multipart<WebPartFactory, Part<?>> multipart() {
		return data -> requestBody.multipart().from((factory, outboundData) -> {
			data.accept(new GenericWebPartFactory(GenericWebRequestBody.this.dataConversionService, factory), new OutboundData<Part<?>>() {
				@Override
				public <T extends Part<?>> void stream(Publisher<T> value) throws IllegalStateException {
					outboundData.stream(Flux.from(value).map(part -> part instanceof GenericWebPart<?> ? ((GenericWebPart<?>) part).unwrap() : part));
				}
			});
		});
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder() {
		return this.request.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<T>createEncoder(this.requestBody.raw(), contentType.getMediaType()))
			.orElseThrow(() -> new IllegalStateException("Empty media type"));
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder(Class<T> type) {
		return this.encoder((Type)type);
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder(Type type) {
		return this.request.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<T>createEncoder(this.requestBody.raw(), contentType.getMediaType(), type))
			.orElseThrow(() -> new IllegalStateException("Empty media type"));
	}
}

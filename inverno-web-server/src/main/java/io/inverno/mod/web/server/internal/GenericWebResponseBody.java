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

import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.web.base.MissingConverterException;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.inverno.mod.web.server.WebResponseBody;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebResponseBody} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericWebResponseBody implements WebResponseBody {

	private final ServerDataConversionService dataConversionService;
	private final ResponseBody responseBody;
	private final Response response;

	/**
	 * <p>
	 * Creates a generic Web response body.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param response              the originating response
	 * @param responseBody          the originating response body
	 */
	public GenericWebResponseBody(ServerDataConversionService dataConversionService, Response response, ResponseBody responseBody) {
		this.dataConversionService = dataConversionService;
		this.responseBody = responseBody;
		this.response = response;
	}

	@Override
	public WebResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		this.responseBody.transform(transformer);
		return this;
	}

	@Override
	public WebResponseBody before(Mono<Void> before) {
		this.responseBody.before(before);
		return this;
	}

	@Override
	public WebResponseBody after(Mono<Void> after) {
		this.responseBody.after(after);
		return this;
	}

	@Override
	public void empty() {
		this.responseBody.empty();
	}

	@Override
	public OutboundData<ByteBuf> raw() {
		return this.responseBody.raw();
	}

	@Override
	public <T extends CharSequence> OutboundData<T> string() {
		return this.responseBody.string();
	}

	@Override
	public Resource resource() {
		return this.responseBody.resource();
	}

	@Override
	public Sse<ByteBuf, Sse.Event<ByteBuf>, Sse.EventFactory<ByteBuf, Sse.Event<ByteBuf>>> sse() {
		return this.responseBody.sse();
	}

	@Override
	public <T extends CharSequence> Sse<T, Sse.Event<T>, Sse.EventFactory<T, Sse.Event<T>>> sseString() {
		return this.responseBody.sseString();
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder() {
		// if we don't have a content type specified in the response, it means that the route was created without any produces clause so we can fallback to a default representation assuming it is accepted in the request otherwise we should fail
		// - define a default converter in the conversion service
		// - check that the produced media type matches the Accept header
		// => We don't have to do anything, if the media is empty, we don't know what to do anyway, so it is up to the user to explicitly set the content type on the response which is enough to make the conversion works otherwise we must fail
		return this.response.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> {
				try {
					return this.dataConversionService.<T>createEncoder(this.response.body().raw(), contentType.getMediaType());
				}
				catch (MissingConverterException e) {
					throw new InternalServerErrorException("No converter found for media type: " + e.getMediaType(), e);
				}
			})
			.orElseThrow(() -> new InternalServerErrorException("Empty media type"));
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder(Class<T> type) {
		return this.encoder((Type)type);
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder(Type type) {
		// if we don't have a content type specified in the response, it means that the route was created without any produces clause so we can fallback to a default representation assuming it is accepted in the request otherwise we should fail
		// - define a default converter in the conversion service
		// - check that the produced media type matches the Accept header
		// => We don't have to do anything, if the media is empty, we don't know what to do anyway, so it is up to the user to explicitly set the content type on the response which is enough to make the conversion works otherwise we must fail
		return this.response.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> {
				try {
					return this.dataConversionService.<T>createEncoder(this.response.body().raw(), contentType.getMediaType(), type);
				}
				catch (MissingConverterException e) {
					throw new InternalServerErrorException("No converter found for media type: " + e.getMediaType(), e);
				}
			})
			.orElseThrow(() -> new InternalServerErrorException("Empty media type"));
	}

	@Override
	public <T> SseEncoder<T> sseEncoder(String mediaType) {
		try {
			return this.dataConversionService.createSseEncoder(this.responseBody.sse(), mediaType);
		}
		catch (MissingConverterException e) {
			throw new InternalServerErrorException("No converter found for media type: " + e.getMediaType(), e);
		}
	}

	@Override
	public <T> SseEncoder<T> sseEncoder(String mediaType, Class<T> type) {
		return this.sseEncoder(mediaType, (Type)type);
	}

	@Override
	public <T> SseEncoder<T> sseEncoder(String mediaType, Type type) {
		try {
			return this.dataConversionService.createSseEncoder(this.responseBody.sse(), mediaType, type);
		}
		catch (MissingConverterException e) {
			throw new InternalServerErrorException("No converter found for media type: " + e.getMediaType(), e);
		}
	}
}

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

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.router.ResponseDataEncoder;
import io.winterframework.mod.web.router.WebResponse;
import io.winterframework.mod.web.router.WebResponseBody;
import io.winterframework.mod.web.server.ResponseBody;
import io.winterframework.mod.web.server.ResponseData;

/**
 * @author jkuhn
 *
 */
public class GenericWebResponseBody implements WebResponseBody {

	private final WebResponse response;
	
	private final ResponseBody responseBody;
	
	private final DataConversionService dataConversionService;
	
	public GenericWebResponseBody(WebResponse response, ResponseBody responseBody, DataConversionService dataConversionService) {
		this.response = response;
		this.dataConversionService = dataConversionService;
		this.responseBody = responseBody;
	}

	@Override
	public void empty() {
		this.responseBody.empty();
	}

	@Override
	public ResponseData<ByteBuf> raw() {
		return this.responseBody.raw();
	}
	
	@Override
	public Resource resource() {
		return this.responseBody.resource();
	}
	
	@Override
	public Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> sse() {
		return this.responseBody.sse();
	}
	
	@Override
	public <T> SseEncoder<T> sseEncoder(String mediaType) {
		return this.dataConversionService.createSseEncoder(this.responseBody.sse(), mediaType);
	}
	
	@Override
	public <T> SseEncoder<T> sseEncoder(String mediaType, Class<T> type) {
		return this.dataConversionService.createSseEncoder(this.responseBody.sse(), mediaType, type);
	}
	
	@Override
	public <T> SseEncoder<T> sseEncoder(String mediaType, Type type) {
		return this.dataConversionService.createSseEncoder(this.responseBody.sse(), mediaType, type);
	}
	
	@Override
	public <T> ResponseDataEncoder<T> encoder() {
		// if we don't have a content type specified in the response, it means that the route was created without any produces clause so we can fallback to a default representation assuming it is accepted in the request otherwise we should fail
		// - define a default converter in the conversion service
		// - check that the produced media type matches the Accept header
		// => We don't have to do anything, if the media is empty, we don't know what to do anyway, so it is up to the user to explicitly set the content type on the response which is enough to make the conversion works otherwise we must fail
		return this.response.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<T>createEncoder(this.response.body().raw(), contentType.getMediaType()))
			.orElseThrow(() -> new InternalServerErrorException("Empty media type"));
	}

	@Override
	public <T> ResponseDataEncoder<T> encoder(Class<T> type) {
		return this.encoder((Type)type);
	}

	@Override
	public <T> ResponseDataEncoder<T> encoder(Type type) {
		// if we don't have a content type specified in the response, it means that the route was created without any produces clause so we can fallback to a default representation assuming it is accepted in the request otherwise we should fail
		// - define a default converter in the conversion service
		// - check that the produced media type matches the Accept header
		// => We don't have to do anything, if the media is empty, we don't know what to do anyway, so it is up to the user to explicitly set the content type on the response which is enough to make the conversion works otherwise we must fail
		return this.response.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<T>createEncoder(this.response.body().raw(), contentType.getMediaType(), type))
			.orElseThrow(() -> new InternalServerErrorException("Empty media type"));
	}
	
}

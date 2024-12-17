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
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.InterceptedResponse;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.inverno.mod.web.client.InterceptedWebResponseBody;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Generic {@link InterceptedWebResponseBody} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericInterceptedWebResponseBody implements InterceptedWebResponseBody {

	private final DataConversionService dataConversionService;
	private final InterceptedResponse response;

	/**
	 * <p>
	 * Creates a generic intercepted Web response body.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param response              the originating intercepted response
	 */
	public GenericInterceptedWebResponseBody(DataConversionService dataConversionService, InterceptedResponse response) {
		this.dataConversionService = dataConversionService;
		this.response = response;
	}

	@Override
	public InterceptedWebResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException {
		this.response.body().transform(transformer);
		return this;
	}

	@Override
	public void empty() {
		this.response.body().empty();
	}

	@Override
	public OutboundData<ByteBuf> raw() {
		return this.response.body().raw();
	}

	@Override
	public <T extends CharSequence> OutboundData<T> string() {
		return this.response.body().string();
	}

	@Override
	public ResourceData resource() {
		return this.response.body().resource();
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder() {
		return this.response.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<T>createEncoder(this.response.body().raw(), contentType.getMediaType()))
			.orElseThrow(() -> new IllegalStateException("Empty media type"));
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder(Class<T> type) {
		return this.encoder((Type)type);
	}

	@Override
	public <T> OutboundDataEncoder<T> encoder(Type type) {
		return this.response.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<T>createEncoder(this.response.body().raw(), contentType.getMediaType(), type))
			.orElseThrow(() -> new IllegalStateException("Empty media type"));
	}
}

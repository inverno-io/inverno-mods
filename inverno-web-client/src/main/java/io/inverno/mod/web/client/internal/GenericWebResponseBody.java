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

import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Response;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.base.InboundDataDecoder;
import io.inverno.mod.web.client.WebResponseBody;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Generic {@link WebResponseBody} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebResponseBody implements WebResponseBody {

	private final Response response;
	private final DataConversionService dataConversionService;

	/**
	 * <p>
	 * Creates a generic Web response body.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param response              the originating response
	 */
	public GenericWebResponseBody(DataConversionService dataConversionService, Response response) {
		this.response = response;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public WebResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException {
		this.response.body().transform(transformer);
		return this;
	}

	@Override
	public InboundData<ByteBuf> raw() {
		return this.response.body().raw();
	}

	@Override
	public InboundData<CharSequence> string() throws IllegalStateException {
		return this.response.body().string();
	}

	@Override
	public <T> InboundDataDecoder<T> decoder(Class<T> type) {
		return this.decoder((Type)type);
	}

	@Override
	public <T> InboundDataDecoder<T> decoder(Type type) {
		return this.response.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> this.dataConversionService.<T>createDecoder(this.response.body().raw(), contentType.getMediaType(), type))
			.orElseThrow(() -> new IllegalStateException("Empty media type"));
	}
}

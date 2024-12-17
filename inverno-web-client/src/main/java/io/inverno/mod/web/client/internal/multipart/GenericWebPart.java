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
package io.inverno.mod.web.client.internal.multipart;

import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.base.OutboundDataEncoder;
import io.inverno.mod.web.client.WebPart;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebPart} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the part body type
 */
public class GenericWebPart<A> implements WebPart<A> {

	private final DataConversionService dataConversionService;
	private final Part<ByteBuf> part;
	private final Type type;

	/**
	 * <p>
	 * Creates a generic Web part.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param part                  the originating part
	 */
	public GenericWebPart(DataConversionService dataConversionService, Part<ByteBuf> part) {
		this(dataConversionService, part, (Type)null);
	}

	/**
	 * <p>
	 * Creates a generic Web part.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service.
	 * @param part the originating part.
	 * @param type the part body type
	 */
	public GenericWebPart(DataConversionService dataConversionService, Part<ByteBuf> part, Class<A> type) {
		this(dataConversionService, part, (Type)type);
	}

	/**
	 * <p>
	 * Creates a generic Web part.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service.
	 * @param part the originating part.
	 * @param type the part body type
	 */
	public GenericWebPart(DataConversionService dataConversionService, Part<ByteBuf> part, Type type) {
		this.dataConversionService = dataConversionService;
		this.part = part;
		this.type = type;
	}

	/**
	 * <p>
	 * Creates a part data encoder.
	 * </p>
	 *
	 * @param <T> the part body type
	 *
	 * @return an outbound data encoder
	 *
	 * @throws IllegalStateException if the content type is defined in the part headers
	 */
	private <T extends A> OutboundDataEncoder<T> createEncoder() throws IllegalStateException {
		return this.part.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> {
				if (this.type == null) {
					return this.dataConversionService.<T>createEncoder(this.part, contentType.getMediaType());
				} else {
					return this.dataConversionService.<T>createEncoder(this.part, contentType.getMediaType(), type);
				}
			})
			.orElseThrow(() -> new IllegalStateException("Empty media type"));
	}

	/**
	 * <p>
	 * Returns the originating part.
	 * </p>
	 *
	 * @return the originating part
	 */
	public Part<ByteBuf> unwrap() {
		return this.part;
	}

	@Override
	public WebPart<A> name(String name) {
		this.part.name(name);
		return this;
	}

	@Override
	public WebPart<A> filename(String filename) {
		this.part.filename(filename);
		return this;
	}

	@Override
	public InboundRequestHeaders headers() {
		return this.part.headers();
	}

	@Override
	public WebPart<A> headers(Consumer<OutboundRequestHeaders> headersConfigurer) {
		this.part.headers(headersConfigurer);
		return this;
	}

	@Override
	public <T extends A> void stream(Publisher<T> value) throws IllegalStateException {
		this.createEncoder().stream(value);
	}

	@Override
	public <T extends A> void many(Flux<T> value) {
		this.createEncoder().many(value);
	}

	@Override
	public <T extends A> void value(T value) throws IllegalStateException {
		this.createEncoder().one(Mono.justOrEmpty(value));
	}

	@Override
	public <T extends A> void one(Mono<T> value) {
		this.createEncoder().one(value);
	}
}

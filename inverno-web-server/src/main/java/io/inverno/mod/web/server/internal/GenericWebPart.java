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

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.web.server.InboundDataDecoder;
import io.inverno.mod.web.server.WebPart;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * <p>
 * Generic {@link WebPart} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebPart implements WebPart {

	private final Part part;
	
	private final DataConversionService dataConversionService;
	
	/**
	 * <p>
	 * Creates a generic web part with the specified underlying part and data conversion service.
	 * </p>
	 *
	 * @param part                  the underlying part
	 * @param dataConversionService the data conversion service
	 */
	public GenericWebPart(Part part, DataConversionService dataConversionService) {
		this.part = part;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public String getName() {
		return this.part.getName();
	}

	@Override
	public Optional<String> getFilename() {
		return this.part.getFilename();
	}

	@Override
	public InboundRequestHeaders headers() {
		return this.part.headers();
	}

	@Override
	public InboundData<ByteBuf> raw() {
		return this.part.raw();
	}

	@Override
	public InboundData<CharSequence> string() {
		return this.part.string();
	}
	
	@Override
	public <A> InboundDataDecoder<A> decoder(Class<A> type) {
		return this.decoder((Type)type);
	}

	@Override
	public <A> InboundDataDecoder<A> decoder(Type type) {
		// Fallback to text/plain media type if no content type is specified in the part because it is not straightforward to specify content-type on an input other than file in a multipart form...
		String mediaType = this.part.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> contentType.getMediaType())
			.orElse(MediaTypes.TEXT_PLAIN);
		try {
			return this.dataConversionService.<A>createDecoder(this.part.raw(), mediaType, type);
		}
		catch (NoConverterException e) {
			throw new InternalServerErrorException("No converter found for media type: " + e.getMediaType(), e);
		}
	}
}

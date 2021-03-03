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
package io.winterframework.mod.web.internal;

import java.lang.reflect.Type;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.Part;
import io.winterframework.mod.http.server.PartHeaders;
import io.winterframework.mod.http.server.RequestData;
import io.winterframework.mod.web.RequestDataDecoder;
import io.winterframework.mod.web.WebPart;

/**
 * @author jkuhn
 *
 */
class GenericWebPart implements WebPart {

	private final Part part;
	
	private final DataConversionService dataConversionService;
	
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
	public PartHeaders headers() {
		return this.part.headers();
	}

	@Override
	public RequestData<ByteBuf> raw() {
		return this.part.raw();
	}

	@Override
	public <A> RequestDataDecoder<A> decoder(Class<A> type) {
		return this.decoder((Type)type);
	}

	@Override
	public <A> RequestDataDecoder<A> decoder(Type type) {
		// Fallback to text/plain media type if no content type is specified in the part because it is not straightforward to specify content-type on an input other than file in a multipart form...
		String mediaType = this.part.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE)
			.map(contentType -> contentType.getMediaType())
			.orElse(MediaTypes.TEXT_PLAIN);
		return this.dataConversionService.<A>createDecoder(this.part.raw(), mediaType, type);
	}
}

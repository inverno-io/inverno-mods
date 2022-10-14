/*
 * Copyright 2022 Jeremy KUHN
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

package io.inverno.mod.http.client.internal.multipart;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ResourcePart extends AbstractPart<Resource> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private Publisher<? extends Resource> resources;

	public ResourcePart(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(headerService, parameterConverter);
		
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}

	@Override
	public <T extends Resource> void stream(Publisher<T> value) throws IllegalStateException {
		this.resources = value;
	}

	public Publisher<ResourcePart.FilePart> getFileParts() {
		if(this.resources == null) {
			return Mono.empty();
		}

		return Flux.from(this.resources)
			.map(resource -> new ResourcePart.FilePart(this.name, this.filename, resource, new GenericPartHeaders(this.headerService, this.parameterConverter, this.headers.getAll())));
	}
	
	public class FilePart extends AbstractDataPart<ByteBuf> {

		public FilePart(String name, String filename, Resource resource, GenericPartHeaders headers) {
			super(ResourcePart.this.headerService, ResourcePart.this.parameterConverter, headers);
			
			this.name = name;
			this.filename = filename;
			
			if(resource.exists().orElse(Boolean.TRUE)) {
				if(this.filename == null) {
					this.filename = resource.getFilename();
				}
				if(this.headers.getContentLength() == null) {
					resource.size().ifPresent(this.headers::contentLength);
				}
				if(this.headers.getCharSequence(Headers.NAME_CONTENT_TYPE) == null) {
					String mediaType = resource.getMediaType();
					this.headers.contentType(mediaType != null ? mediaType : MediaTypes.APPLICATION_OCTET_STREAM);
				}
				this.setData(resource.read().orElseThrow(() -> new IllegalArgumentException("Resource is not readable: " + resource.getURI())));
			}
			else {
				throw new IllegalArgumentException("Resource does not exist: " + resource.getURI());
			}
		}
		
		@Override
		public <T extends ByteBuf> void stream(Publisher<T> value) throws IllegalStateException {
			throw new UnsupportedOperationException();
		}
	}
}
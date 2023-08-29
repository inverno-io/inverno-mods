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

package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.PreResponse;
import io.inverno.mod.http.client.PreResponseBody;
import io.inverno.mod.http.client.ResponseBody;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericPreResponseBody implements PreResponseBody, ResponseBody {

	private final PreResponse response;
	
	private RawOutboundData rawData;
	private StringOutboundData stringData;
	private PreResponseBody.ResourceData resourceData;
	
	private Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer;
	private Publisher<ByteBuf> data;

	public GenericPreResponseBody(PreResponse response) {
		this.response = response;
	}
	
	private void setData(Publisher<ByteBuf> data) {
		this.data = this.transformer != null ? this.transformer.apply(data) : data;
	}
	
	private Publisher<ByteBuf> getData() {
		if(this.data == null) {
			if(this.transformer != null) {
				return this.transformer.apply(Mono.empty());
			}
			return Mono.empty();
		}
		return this.data;
	}
	
	@Override
	public GenericPreResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		if(this.transformer == null) {
			this.transformer = transformer;
		}
		else {
			this.transformer = this.transformer.andThen(transformer);
		}
		
		if(this.data != null) {
			this.data = transformer.apply(this.data);
		}
		return this;
	}
	
	public Function<Publisher<ByteBuf>, Publisher<ByteBuf>> getTransformer() {
		return transformer;
	}

	@Override
	public void empty() {
		this.setData(Mono.empty());
	}

	@Override
	public RawOutboundData raw() {
		if(this.rawData == null) {
			this.rawData = new GenericPreResponseBody.RawOutboundData();
		}
		return this.rawData;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StringOutboundData string() {
		if(this.stringData == null) {
			this.stringData = new GenericPreResponseBody.StringOutboundData();
		}
		return this.stringData;
	}

	@Override
	public PreResponseBody.ResourceData resource() {
		if(this.resourceData == null) {
			this.resourceData = new GenericPreResponseBody.ResourceData();
		}
		return this.resourceData;
	}

	private class RawOutboundData implements OutboundData<ByteBuf>, InboundData<ByteBuf> {

		@SuppressWarnings("unchecked")
		@Override
		public <T extends ByteBuf> void stream(Publisher<T> data) {
			GenericPreResponseBody.this.setData((Publisher<ByteBuf>) data);
		}

		@Override
		public Publisher<ByteBuf> stream() {
			return GenericPreResponseBody.this.getData();
		}
	}
	
	private class StringOutboundData implements OutboundData<CharSequence>, InboundData<CharSequence> {

		@Override
		public <T extends CharSequence> void stream(Publisher<T> value) throws IllegalStateException {
			Publisher<ByteBuf> data;
			if(value instanceof Mono) {
				data = ((Mono<T>)value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
			}
			else {
				data = Flux.from(value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
			}
			GenericPreResponseBody.this.setData(data);
		}
		
		@Override
		public <T extends CharSequence> void value(T value) throws IllegalStateException {
			GenericPreResponseBody.this.setData(value != null ? Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value, Charsets.DEFAULT))) : Mono.empty());
		}

		@Override
		public Publisher<CharSequence> stream() {
			return Flux.from(GenericPreResponseBody.this.getData()).map(chunk -> chunk.toString(Charsets.DEFAULT));
		}
	}
	
	protected class ResourceData implements PreResponseBody.ResourceData {

		/**
		 * <p>
		 * Tries to determine resource content type in which case sets the content type
		 * header.
		 * </p>
		 * 
		 * @param resource the resource
		 */
		protected void populateHeaders(io.inverno.mod.base.resource.Resource resource) {
			GenericPreResponseBody.this.response.headers(h -> {
				if(GenericPreResponseBody.this.response.headers().getContentLength() == null) {
					resource.size().ifPresent(h::contentLength);
				}
				
				if(GenericPreResponseBody.this.response.headers().contains(Headers.NAME_CONTENT_TYPE)) {
					String mediaType = resource.getMediaType();
					if(mediaType != null) {
						h.contentType(mediaType);
					}
				}
				
				if(GenericPreResponseBody.this.response.headers().contains(Headers.NAME_LAST_MODIFIED)) {
					resource.lastModified().ifPresent(lastModified -> {
						h.set(Headers.NAME_LAST_MODIFIED, Headers.FORMATTER_RFC_5322_DATE_TIME.format(lastModified.toInstant()));
					});
				}
			});
		}
		
		@Override
		public void value(io.inverno.mod.base.resource.Resource resource) {
			Objects.requireNonNull(resource);
			// In case of file resources we should always be able to determine existence
			// For other resources with a null exists we can still try, worst case scenario: 
			// internal server error
			if(resource.exists().orElse(true)) {
				this.populateHeaders(resource);
				GenericPreResponseBody.this.setData(resource.read().orElseThrow(() -> new InternalServerErrorException("Resource is not readable: " + resource.getURI())));
			}
			else {
				throw new NotFoundException();
			}
		}
	}
}

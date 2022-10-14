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
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericRequestBodyConfigurator<A extends GenericRequestBody> implements RequestBodyConfigurator {

	protected final InternalRequestHeaders requestHeaders;
	protected final A requestBody;
	protected final ObjectConverter<String> parameterConverter;
	protected final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	protected final MultipartEncoder<Part<?>> multipartBodyEncoder;
	protected final Part.Factory partFactory;
	
	protected GenericRequestBodyConfigurator.RawOutboundData rawData;
	protected GenericRequestBodyConfigurator.StringOutboundData stringData;
	protected GenericRequestBodyConfigurator.ResourceData resourceData;
	protected GenericRequestBodyConfigurator.UrlEncodedData urlEncodedData;
	protected GenericRequestBodyConfigurator.MultipartData multipartData;

	public GenericRequestBodyConfigurator(InternalRequestHeaders requestHeaders, 
			A requestBody, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder,
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory) {
		this.requestHeaders = requestHeaders;
		this.requestBody = requestBody;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
	}

	@Override
	public void empty() {
		this.requestBody.setData(Mono.empty());
	}

	@Override
	public OutboundData<ByteBuf> raw() {
		if(this.rawData == null) {
			this.rawData = new GenericRequestBodyConfigurator.RawOutboundData();
		}
		return this.rawData;
	}

	@Override
	public <T extends CharSequence> OutboundData<T> string() {
		if(this.stringData == null) {
			this.stringData = new GenericRequestBodyConfigurator.StringOutboundData();
		}
		return (OutboundData<T>)this.stringData;
	}

	@Override
	public RequestBodyConfigurator.Resource resource() {
		if(this.resourceData == null) {
			this.resourceData = new GenericRequestBodyConfigurator.ResourceData();
		}
		return this.resourceData;
	}

	@Override
	public RequestBodyConfigurator.UrlEncoded<Parameter.Factory> urlEncoded() {
		if(this.urlEncodedData == null) {
			this.urlEncodedData = new GenericRequestBodyConfigurator.UrlEncodedData();
		}
		return this.urlEncodedData;
	}

	@Override
	public RequestBodyConfigurator.Multipart<Part.Factory, Part<?>> multipart() {
		if(this.multipartData == null) {
			this.multipartData = new GenericRequestBodyConfigurator.MultipartData();
		}
		return this.multipartData;
	}
	
	protected class RawOutboundData implements OutboundData<ByteBuf> {

		@Override
		public <T extends ByteBuf> void stream(Publisher<T> value) throws IllegalStateException {
			GenericRequestBodyConfigurator.this.requestBody.setData((Publisher<ByteBuf>) value);
		}
	}
	
	protected class StringOutboundData implements OutboundData<CharSequence> {

		@Override
		public <T extends CharSequence> void stream(Publisher<T> value) throws IllegalStateException {
			Publisher<ByteBuf> data;
			if(value instanceof Mono) {
				data = ((Mono<T>)value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
			}
			else {
				data = Flux.from(value).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk, Charsets.DEFAULT)));
			}
			GenericRequestBodyConfigurator.this.requestBody.setData(data);
		}

		@Override
		public <T extends CharSequence> void value(T value) throws IllegalStateException {
			GenericRequestBodyConfigurator.this.requestBody.setData(value != null ? Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value, Charsets.DEFAULT))) : Mono.empty());
		}
	}
	
	protected class ResourceData implements RequestBodyConfigurator.Resource {

		protected void populateHeaders(io.inverno.mod.base.resource.Resource resource) {
			if(!GenericRequestBodyConfigurator.this.requestHeaders.contains(Headers.NAME_CONTENT_LENGTH)) {
				resource.size().ifPresent(GenericRequestBodyConfigurator.this.requestHeaders::contentLength);
			}
			if(!GenericRequestBodyConfigurator.this.requestHeaders.contains(Headers.NAME_CONTENT_TYPE)) {
				String mediaType = resource.getMediaType();
				if(mediaType != null) {
					GenericRequestBodyConfigurator.this.requestHeaders.contentType(mediaType);
				}
			}
		}
		
		@Override
		public void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException {
			Objects.requireNonNull(resource);
			if(resource.exists().orElse(true)) {
				this.populateHeaders(resource);
				GenericRequestBodyConfigurator.this.requestBody.setData(resource.read().orElseThrow(() -> new IllegalArgumentException("Resource is not readable: " + resource.getURI())));
			}
			else {
				throw new IllegalArgumentException("Resource does not exist: " + resource.getURI());
			}
		}
	}
	
	protected class UrlEncodedData implements RequestBodyConfigurator.UrlEncoded<Parameter.Factory> {

		@Override
		public void from(BiConsumer<Parameter.Factory, OutboundData<Parameter>> data) {
			data.accept(this::create, this::stream);
		}

		protected <T extends Parameter> void stream(Publisher<T> value) throws IllegalStateException {
			GenericRequestBodyConfigurator.this.requestHeaders.getContentTypeHeader().ifPresentOrElse(
				contentType -> {
					GenericRequestBodyConfigurator.this.requestBody.setData(GenericRequestBodyConfigurator.this.urlEncodedBodyEncoder.encode(Flux.from(value), contentType));
				},
				() -> {
					Headers.ContentType contentType = new ContentTypeCodec.ContentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED, Charsets.DEFAULT, null, null);
					GenericRequestBodyConfigurator.this.requestHeaders.set(contentType);
					GenericRequestBodyConfigurator.this.requestBody.setData(GenericRequestBodyConfigurator.this.urlEncodedBodyEncoder.encode(Flux.from(value), contentType));
				}
			);
		}
		
		protected <T> Parameter create(String name, T value) {
			return new GenericParameter(name, value, GenericRequestBodyConfigurator.this.parameterConverter);
		}
	}
	
	protected class MultipartData implements RequestBodyConfigurator.Multipart<Part.Factory, Part<?>> {

		@Override
		public void from(BiConsumer<Part.Factory, OutboundData<Part<?>>> data) {
			data.accept(GenericRequestBodyConfigurator.this.partFactory, this::stream);
		}
	
		protected <T extends Part<?>> void stream(Publisher<T> value) throws IllegalStateException {
			GenericRequestBodyConfigurator.this.requestHeaders.getContentTypeHeader().ifPresentOrElse(
				contentType -> {
					GenericRequestBodyConfigurator.this.requestBody.setData(GenericRequestBodyConfigurator.this.multipartBodyEncoder.encode(Flux.from(value), contentType));
				},
				() -> {
					Headers.ContentType contentType = new ContentTypeCodec.ContentType(MediaTypes.MULTIPART_FORM_DATA, Charsets.DEFAULT, ContentTypeCodec.generateMultipartBoundary(), null);
					GenericRequestBodyConfigurator.this.requestHeaders.set(contentType);
					GenericRequestBodyConfigurator.this.requestBody.setData(GenericRequestBodyConfigurator.this.multipartBodyEncoder.encode(Flux.from(value), contentType));
				}
			);
		}
	}
}

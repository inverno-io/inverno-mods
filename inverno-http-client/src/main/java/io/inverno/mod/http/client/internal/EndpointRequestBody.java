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
import io.inverno.mod.http.client.RequestBody;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * The {@link RequestBody} implementation exposed in the {@link EndpointRequest} to set the request body.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointRequestBody implements RequestBody {

	private final EndpointRequestHeaders requestHeaders;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	private final MultipartEncoder<Part<?>> multipartBodyEncoder;
	private final Part.Factory partFactory;
	
	private EndpointRequestBody.RawOutboundData rawData;
	private EndpointRequestBody.StringOutboundData stringData;
	private EndpointRequestBody.ResourceData resourceData;
	private EndpointRequestBody.UrlEncodedData urlEncodedData;
	private EndpointRequestBody.MultipartData multipartData;
	
	private Publisher<ByteBuf> data;
	private io.inverno.mod.base.resource.Resource resource;
	private Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer;
	
	/**
	 * <p>
	 * Creates an endpoint request body.
	 * </p>
	 * 
	 * @param requestHeaders        the request headers
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 */
	public EndpointRequestBody(EndpointRequestHeaders requestHeaders, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder,
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory) {
		this.requestHeaders = requestHeaders;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
	}
	
	/**
	 * <p>
	 * Sets the payload raw data publisher of the request body.
	 * </p>
	 * 
	 * @param data a raw data publisher
	 * 
	 * @throws IllegalStateException if the request payload was already sent to the endpoint
	 */
	private void setData(Publisher<ByteBuf> data) throws IllegalStateException {
		Publisher<ByteBuf> transformedData = this.transformer != null ? this.transformer.apply(data) : data;
		this.resource = null;
		this.data = transformedData;
	}

	/**
	 * <p>
	 * Return the raw data publisher that has been set in the request body or null if none was specified.
	 * </p>
	 * 
	 * @return the raw data publisher or null
	 */
	public Publisher<ByteBuf> getData() {
		return this.data;
	}
	
	/**
	 * <p>
	 * Sets the resource that has been set as request body.
	 * </p>
	 * 
	 * @param resource the resource
	 */
	private void setResource(io.inverno.mod.base.resource.Resource resource) {
		this.resource = resource;
	}

	/**
	 * <p>
	 * Returns the resource that has been set in the request body or null
	 * </p>
	 * 
	 * <p>
	 * Returning the resource here enables transport specific implementation to use optimized file access (zero copy) when the transport allows it.
	 * </p>
	 * 
	 * @return the resource or null.
	 */
	public io.inverno.mod.base.resource.Resource getResource() {
		return resource;
	}
	
	@Override
	public RequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
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
	
	@Override
	public void empty() {
		this.setData(Mono.empty());
	}

	@Override
	public OutboundData<ByteBuf> raw() {
		if(this.rawData == null) {
			this.rawData = new EndpointRequestBody.RawOutboundData();
		}
		return this.rawData;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends CharSequence> OutboundData<T> string() {
		if(this.stringData == null) {
			this.stringData = new StringOutboundData();
		}
		return (OutboundData<T>)this.stringData;
	}

	@Override
	public RequestBody.Resource resource() {
		if(this.resourceData == null) {
			this.resourceData = new EndpointRequestBody.ResourceData();
		}
		return this.resourceData;
	}

	@Override
	public RequestBody.UrlEncoded<Parameter.Factory> urlEncoded() {
		if(this.urlEncodedData == null) {
			this.urlEncodedData = new EndpointRequestBody.UrlEncodedData();
		}
		return this.urlEncodedData;
	}

	@Override
	public RequestBody.Multipart<Part.Factory, Part<?>> multipart() {
		if(this.multipartData == null) {
			this.multipartData = new EndpointRequestBody.MultipartData();
		}
		return this.multipartData;
	}
	
	/**
	 * <p>
	 * Raw {@link OutboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class RawOutboundData implements OutboundData<ByteBuf> {

		@Override
		@SuppressWarnings("unchecked")
		public <T extends ByteBuf> void stream(Publisher<T> value) throws IllegalStateException {
			EndpointRequestBody.this.setData((Publisher<ByteBuf>) value);
		}
	}
	
	/**
	 * <p>
	 * String {@link OutboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class StringOutboundData implements OutboundData<CharSequence> {

		@Override
		public <T extends CharSequence> void stream(Publisher<T> value) throws IllegalStateException {
			Publisher<ByteBuf> data;
			if(value instanceof Mono) {
				data = ((Mono<T>)value).map(chunk -> Unpooled.copiedBuffer(chunk, Charsets.DEFAULT));
			}
			else {
				data = Flux.from(value).map(chunk -> Unpooled.copiedBuffer(chunk, Charsets.DEFAULT));
			}
			EndpointRequestBody.this.setData(data);
		}

		@Override
		public <T extends CharSequence> void value(T value) throws IllegalStateException {
			EndpointRequestBody.this.setData(value != null ? Mono.just(Unpooled.copiedBuffer(value, Charsets.DEFAULT)) : Mono.empty());
		}
	}
	
	/**
	 * <p>
	 * Generic {@link RequestBody.Resource} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class ResourceData implements RequestBody.Resource {
		
		protected void populateHeaders(io.inverno.mod.base.resource.Resource resource) {
			if(!EndpointRequestBody.this.requestHeaders.contains(Headers.NAME_CONTENT_LENGTH)) {
				resource.size().ifPresent(EndpointRequestBody.this.requestHeaders::contentLength);
			}
			if(!EndpointRequestBody.this.requestHeaders.contains(Headers.NAME_CONTENT_TYPE)) {
				String mediaType = resource.getMediaType();
				if(mediaType != null) {
					EndpointRequestBody.this.requestHeaders.contentType(mediaType);
				}
			}
		}
		
		@Override
		public void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException {
			Objects.requireNonNull(resource);
			if(resource.exists().orElse(true)) {
				this.populateHeaders(resource);
				EndpointRequestBody.this.setData(resource.read());
				EndpointRequestBody.this.setResource(resource);
			}
			else {
				throw new IllegalArgumentException("Resource does not exist: " + resource.getURI());
			}
		}
	}
	
	/**
	 * <p>
	 * Generic {@link RequestBody.UrlEncoded} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class UrlEncodedData implements Parameter.Factory, RequestBody.UrlEncoded<Parameter.Factory> {

		@Override
		public void from(BiConsumer<Parameter.Factory, OutboundData<Parameter>> data) {
			data.accept(this, this::stream);
		}

		/**
		 * <p>
		 * Sets the stream of parameters to send to the endpoint.
		 * </p>
		 * 
		 * @param <T>   the parameter value type
		 * @param value the stream of parameters
		 * 
		 * @throws IllegalStateException if the request payload was already sent to the endpoint
		 */
		protected <T extends Parameter> void stream(Publisher<T> value) throws IllegalStateException {
			Headers.ContentType contentTypeHeader = EndpointRequestBody.this.requestHeaders.getContentTypeHeader();
			if(contentTypeHeader == null) {
				contentTypeHeader = new ContentTypeCodec.ContentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED, Charsets.DEFAULT, null, null);
				EndpointRequestBody.this.requestHeaders.set(contentTypeHeader);
			}
			EndpointRequestBody.this.setData(EndpointRequestBody.this.urlEncodedBodyEncoder.encode(Flux.from(value), contentTypeHeader));
		}
		
		@Override
		public <T> Parameter create(String name, T value) {
			return new GenericParameter(name, value, EndpointRequestBody.this.parameterConverter);
		}

		@Override
		public <T> Parameter create(String name, T value, Type type) {
			return new GenericParameter(name, value, EndpointRequestBody.this.parameterConverter, type);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link RequestBody.Multipart} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class MultipartData implements RequestBody.Multipart<Part.Factory, Part<?>> {

		@Override
		public void from(BiConsumer<Part.Factory, OutboundData<Part<?>>> data) {
			data.accept(EndpointRequestBody.this.partFactory, this::stream);
		}
	
		/**
		 * <p>
		 * Sets the stream of parts to send to the endpoint.
		 * </p>
		 * 
		 * @param <T>   the part's data type
		 * @param value the stream of parameters
		 * 
		 * @throws IllegalStateException if the request payload was already sent to the endpoint
		 */
		protected <T extends Part<?>> void stream(Publisher<T> value) throws IllegalStateException {
			Headers.ContentType contentTypeHeader = EndpointRequestBody.this.requestHeaders.getContentTypeHeader();
			if(contentTypeHeader == null) {
				contentTypeHeader = new ContentTypeCodec.ContentType(MediaTypes.MULTIPART_FORM_DATA, Charsets.DEFAULT, ContentTypeCodec.generateMultipartBoundary(), null);
				EndpointRequestBody.this.requestHeaders.set(contentTypeHeader);
			}
			else if(contentTypeHeader.getBoundary() == null) {
				contentTypeHeader = new ContentTypeCodec.ContentType(MediaTypes.MULTIPART_FORM_DATA, Charsets.orDefault(contentTypeHeader.getCharset()), ContentTypeCodec.generateMultipartBoundary(), contentTypeHeader.getParameters());
				EndpointRequestBody.this.requestHeaders.set(contentTypeHeader);
			}
			EndpointRequestBody.this.setData(EndpointRequestBody.this.multipartBodyEncoder.encode(Flux.from(value), contentTypeHeader));
		}
	}
}

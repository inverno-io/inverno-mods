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
 * <p>
 * Generic {@link RequestBodyConfigurator} implementation.
 * </p>
 *
 * <p>
 * This implementation sets the request payload data publisher in a {@link GenericResponseBody} which is eventually subscribed when sending the request to the endpoint.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see GenericResponseBody
 * 
 * @param <A> the generic request body type
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

	/**
	 * <p>
	 * Creates generic request body configurator.
	 * </p>
	 * 
	 * @param requestHeaders        the request headers
	 * @param requestBody           the request body
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 */
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
	
	/**
	 * <p>
	 * Raw {@link OutboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class RawOutboundData implements OutboundData<ByteBuf> {

		@Override
		public <T extends ByteBuf> void stream(Publisher<T> value) throws IllegalStateException {
			GenericRequestBodyConfigurator.this.requestBody.setData((Publisher<ByteBuf>) value);
		}
	}
	
	/**
	 * <p>
	 * String {@link OutboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
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
	
	/**
	 * <p>
	 * Generic {@link RequestBodyConfigurator.Resource} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
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
	
	/**
	 * <p>
	 * Generic {@link RequestBodyConfigurator.UrlEncoded} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class UrlEncodedData implements RequestBodyConfigurator.UrlEncoded<Parameter.Factory> {

		@Override
		public void from(BiConsumer<Parameter.Factory, OutboundData<Parameter>> data) {
			data.accept(this::create, this::stream);
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
			Headers.ContentType contentTypeHeader = GenericRequestBodyConfigurator.this.requestHeaders.getContentTypeHeader();
			if(contentTypeHeader == null) {
				contentTypeHeader = new ContentTypeCodec.ContentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED, Charsets.DEFAULT, null, null);
				GenericRequestBodyConfigurator.this.requestHeaders.set(contentTypeHeader);
			}
			GenericRequestBodyConfigurator.this.requestBody.setData(GenericRequestBodyConfigurator.this.urlEncodedBodyEncoder.encode(Flux.from(value), contentTypeHeader));
		}
		
		/**
		 * <p>
		 * Creates a parameter with specified name and value.
		 * </p>
		 * 
		 * @param <T>   the parameter value type
		 * @param name  the parameter name 
		 * @param value the parameter value
		 * 
		 * @return a new parameter
		 */
		protected <T> Parameter create(String name, T value) {
			return new GenericParameter(name, value, GenericRequestBodyConfigurator.this.parameterConverter);
		}
	}
	
	/**
	 * <p>
	 * Generic {@link RequestBodyConfigurator.Multipart} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class MultipartData implements RequestBodyConfigurator.Multipart<Part.Factory, Part<?>> {

		@Override
		public void from(BiConsumer<Part.Factory, OutboundData<Part<?>>> data) {
			data.accept(GenericRequestBodyConfigurator.this.partFactory, this::stream);
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
			Headers.ContentType contentTypeHeader = GenericRequestBodyConfigurator.this.requestHeaders.getContentTypeHeader();
			if(contentTypeHeader == null) {
				contentTypeHeader = new ContentTypeCodec.ContentType(MediaTypes.MULTIPART_FORM_DATA, Charsets.DEFAULT, ContentTypeCodec.generateMultipartBoundary(), null);
				GenericRequestBodyConfigurator.this.requestHeaders.set(contentTypeHeader);
			}
			GenericRequestBodyConfigurator.this.requestBody.setData(GenericRequestBodyConfigurator.this.multipartBodyEncoder.encode(Flux.from(value), contentTypeHeader));
		}
	}
}

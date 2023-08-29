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
import io.inverno.mod.http.client.ResponseBody;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.inverno.mod.http.client.InterceptableResponse;
import io.inverno.mod.http.client.InterceptableResponseBody;

/**
 * <p>
 * Generic {@link InterceptableResponse} implementation.
 * </p>
 * 
 * <p>
 * This implementation also implements {@link ResponseBody} which allows it to act as a proxy for the actual response body once the response has been received from the endpoint. This allows to expose
 * the body actually received to interceptors which is required to be able to transform the response payload publisher. The {@link #setReceivedResponseBody(io.inverno.mod.http.client.ResponseBody)}
 * shall be invoked to make this instance delegates to the received response body. At this point the interceptable response body should become immutable, only transforming the payload data publisher
 * should be allowed.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class GenericInterceptableResponseBody implements InterceptableResponseBody, ResponseBody {

	private final InterceptableResponse response;
	
	private RawOutboundData rawData;
	private StringOutboundData stringData;
	private InterceptableResponseBody.ResourceData resourceData;
	
	private Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer;
	private Publisher<ByteBuf> data;
	
	private ResponseBody receivedBody;

	/**
	 * <p>
	 * Creates a generic interceptable response body.
	 * </p>
	 * 
	 * @param response the enclosig interceptable response
	 */
	public GenericInterceptableResponseBody(InterceptableResponse response) {
		this.response = response;
	}
	
	/**
	 * <p>
	 * Injects the actual response body received from the endpoint.
	 * </p>
	 * 
	 * @param receivedBody the response body received from the endpoint
	 */
	public void setReceivedResponseBody(ResponseBody receivedBody) {
		this.receivedBody = receivedBody;
	}
	
	/**
	 * <p>
	 * Sets the response data publisher.
	 * </p>
	 * 
	 * <p>
	 * This is only allowed while executing interceptors before the actual response is received from the endpoint. The interceptable response will be exposed in the resulting exchange only when the
	 * interceptable exchange has been canceled in an interceptor, otherwise the actual response received from the endpoint will simply erase the interceptable response.
	 * </p>
	 * 
	 * @param data the payload data publisher
	 * 
	 * @throws IllegalStateException if a body has already been received from the endpoint
	 */
	private void setData(Publisher<ByteBuf> data) throws IllegalStateException {
		if(this.receivedBody != null) {
			throw new IllegalStateException("Response already received");
		}
		this.data = this.transformer != null ? this.transformer.apply(data) : data;
	}
	
	/**
	 * <p>
	 * Returns the payload data publisher set in the interceptable response body by an interceptor.
	 * </p>
	 * 
	 * @return the payload data publisher
	 */
	private Publisher<ByteBuf> getData() {
		if(this.data == null) {
			if(this.transformer != null) {
				return this.transformer.apply(Mono.empty());
			}
			return Mono.empty();
		}
		return this.data;
	}
	
	/**
	 * <p>
	 * Returns the payload data publisher transformer.
	 * </p>
	 * 
	 * <p>
	 * Assuming the exchange hasn't been canceled in an interceptor and a response is received from the endpoint, this transformer will be applied to the actual payload data publisher.
	 * </p>
	 * 
	 * @return the payload data publisher transformer or null
	 */
	public Function<Publisher<ByteBuf>, Publisher<ByteBuf>> getTransformer() {
		return transformer;
	}
	
	@Override
	public GenericInterceptableResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		if(this.receivedBody != null) {
			this.receivedBody.transform(transformer);
			return this;
		}
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
	public RawOutboundData raw() {
		if(this.rawData == null) {
			this.rawData = new GenericInterceptableResponseBody.RawOutboundData();
		}
		return this.rawData;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StringOutboundData string() {
		if(this.stringData == null) {
			this.stringData = new GenericInterceptableResponseBody.StringOutboundData();
		}
		return this.stringData;
	}

	@Override
	public InterceptableResponseBody.ResourceData resource() {
		if(this.resourceData == null) {
			this.resourceData = new GenericInterceptableResponseBody.ResourceData();
		}
		return this.resourceData;
	}

	/**
	 * <p>
	 * Raw {@link OutboundData}/{@link InboundData} implementation.
	 * </p>
	 * 
	 * <p>
	 * This implements both outbound and inbound data as this will be used while intercepting the interceptable exchange and it might be used in the resulting exhange if an interceptor canceled the
	 * exchange.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class RawOutboundData implements OutboundData<ByteBuf>, InboundData<ByteBuf> {

		@SuppressWarnings("unchecked")
		@Override
		public <T extends ByteBuf> void stream(Publisher<T> data) throws IllegalStateException {
			GenericInterceptableResponseBody.this.setData((Publisher<ByteBuf>) data);
		}

		@Override
		public Publisher<ByteBuf> stream() {
			if(GenericInterceptableResponseBody.this.receivedBody != null) {
				return GenericInterceptableResponseBody.this.receivedBody.raw().stream();
			}
			return GenericInterceptableResponseBody.this.getData();
		}
	}
	
	/**
	 * <p>
	 * String {@link OutboundData}/{@link InboundData} implementation.
	 * </p>
	 * 
	 * <p>
	 * This implements both outbound and inbound data as this will be used while intercepting the interceptable exchange and it might be used in the resulting exhange if an interceptor canceled the
	 * exchange.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
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
			GenericInterceptableResponseBody.this.setData(data);
		}
		
		@Override
		public <T extends CharSequence> void value(T value) throws IllegalStateException {
			GenericInterceptableResponseBody.this.setData(value != null ? Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(value, Charsets.DEFAULT))) : Mono.empty());
		}

		@Override
		public Publisher<CharSequence> stream() {
			if(GenericInterceptableResponseBody.this.receivedBody != null) {
				return GenericInterceptableResponseBody.this.receivedBody.string().stream();
			}
			return Flux.from(GenericInterceptableResponseBody.this.getData()).map(chunk -> chunk.toString(Charsets.DEFAULT));
		}
	}
	
	/**
	 * <p>
	 * String {@link InterceptableResponseBody.ResourceData} implementation.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class ResourceData implements InterceptableResponseBody.ResourceData {

		/**
		 * <p>
		 * Tries to determine resource content type in which case sets the content type
		 * header.
		 * </p>
		 * 
		 * @param resource the resource
		 */
		protected void populateHeaders(io.inverno.mod.base.resource.Resource resource) {
			GenericInterceptableResponseBody.this.response.headers(h -> {
				if(GenericInterceptableResponseBody.this.response.headers().getContentLength() == null) {
					resource.size().ifPresent(h::contentLength);
				}
				
				if(GenericInterceptableResponseBody.this.response.headers().contains(Headers.NAME_CONTENT_TYPE)) {
					String mediaType = resource.getMediaType();
					if(mediaType != null) {
						h.contentType(mediaType);
					}
				}
				
				if(GenericInterceptableResponseBody.this.response.headers().contains(Headers.NAME_LAST_MODIFIED)) {
					resource.lastModified().ifPresent(lastModified -> {
						h.set(Headers.NAME_LAST_MODIFIED, Headers.FORMATTER_RFC_5322_DATE_TIME.format(lastModified.toInstant()));
					});
				}
			});
		}
		
		@Override
		public void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException {
			Objects.requireNonNull(resource);
			// In case of file resources we should always be able to determine existence
			// For other resources with a null exists we can still try, worst case scenario: 
			// internal server error
			if(resource.exists().orElse(true)) {
				this.populateHeaders(resource);
				GenericInterceptableResponseBody.this.setData(resource.read().orElseThrow(() -> new InternalServerErrorException("Resource is not readable: " + resource.getURI())));
			}
			else {
				throw new NotFoundException();
			}
		}
	}
}

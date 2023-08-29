/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Generic {@link RequestBody} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericRequestBody implements RequestBody {
	
	private final Optional<Headers.ContentType> contentType;
	private final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	private final MultipartDecoder<Part> multipartBodyDecoder;

	Sinks.Many<ByteBuf> dataSink;
	private boolean subscribed;
	private boolean disposed;
	
	private Flux<ByteBuf> data;
	private InboundData<ByteBuf> rawData;
	private InboundData<CharSequence> stringData;
	private RequestBody.UrlEncoded urlEncodedData;
	private RequestBody.Multipart<Part> multipartData;

	/**
	 * <p>
	 * Creates a request body with the specified content type, url encoded body decoder, multipart body decoder and payload data publisher.
	 * </p>
	 *
	 * @param contentType           the request content type
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	public GenericRequestBody(Optional<Headers.ContentType> contentType, MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder) {
		this.contentType = contentType;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		
		// TODO deal with backpressure using a custom queue: if the queue reach a given threshold we should suspend the read on the channel: this.context.channel().config().setAutoRead(false)
		// and resume when this flux is actually consumed (doOnRequest? this might impact performance)
		this.dataSink = Sinks.many().unicast().onBackpressureBuffer();
		this.data = Flux.defer(() -> {
			if(this.disposed) {
				return Mono.error(new IllegalStateException("Request was disposed"));
			}
			return this.dataSink.asFlux()
				.doOnSubscribe(ign -> this.subscribed = true)
				.doOnDiscard(ByteBuf.class, ByteBuf::release);
		});
	}

	void dispose() {
		this.dataSink.tryEmitComplete();
		if(!this.subscribed) {
			// Try to drain and release buffered data 
			// when the datasink was already subscribed data are released in doOnDiscard
			this.dataSink.asFlux().subscribe(
				chunk -> chunk.release(), 
				ex -> {
					// TODO Should be ignored but can be logged as debug or trace log
				}
			);
		}
		this.disposed = true;
	}

	@Override
	public RequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		if(this.subscribed) {
			throw new IllegalStateException("Request data already consumed");
		}
		this.data = Flux.from(transformer.apply(this.data));
		return this;
	}

	@Override
	public InboundData<ByteBuf> raw() {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.rawData == null) {
			this.rawData = new RawInboundData();
		}
		return this.rawData;
	}
	
	@Override
	public InboundData<CharSequence> string() throws IllegalStateException {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.stringData == null) {
			this.stringData = new StringInboundData();
		}
		return this.stringData;
	}
	
	@Override
	public RequestBody.UrlEncoded urlEncoded() {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.urlEncodedData == null) {
			this.urlEncodedData = new UrlEncodedInboundData(this.urlEncodedBodyDecoder.decode(this.data, this.contentType.orElse(null)));
		}
		return this.urlEncodedData;
	}
	
	@Override
	public RequestBody.Multipart<Part> multipart() {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.multipartData == null) {
			this.multipartData = new MultipartInboundData(this.multipartBodyDecoder.decode(this.data, this.contentType.orElse(null)));
		}
		return this.multipartData;
	}
	
	/**
	 * <p>
	 * Generic raw {@link InboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class RawInboundData implements InboundData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return GenericRequestBody.this.data;
		}
	}
	
	/**
	 * <p>
	 * Generic string {@link InboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class StringInboundData implements InboundData<CharSequence> {

		@Override
		public Publisher<CharSequence> stream() {
			return GenericRequestBody.this.data.map(buf -> {
				try {
					return buf.toString(Charsets.DEFAULT);
				}
				finally {
					buf.release();
				}
			});
		}
	}

	/**
	 * <p>
	 * Generic {@link RequestBody.UrlEncoded} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class UrlEncodedInboundData implements RequestBody.UrlEncoded {

		private final Publisher<Parameter> parameters;
		
		private Mono<Map<String, Parameter>> parametersMap;
		
		/**
		 * <p>
		 * Creates an application/x-www-form-urlencoded data consumer with the specified
		 * source of parameters.
		 * </p>
		 * 
		 * @param parameters the parameter publisher
		 */
		public UrlEncodedInboundData(Publisher<Parameter> parameters) {
			this.parameters = Flux.from(parameters).cache();
		}

		@Override
		public Publisher<Parameter> stream() {
			return this.parameters;
		}

		@Override
		public Mono<Map<String, Parameter>> collectMap() {
			if(this.parametersMap == null) {
				this.parametersMap = Flux.from(this.parameters).collectMap(Parameter::getName).cache();
			}
			return this.parametersMap;
		}
	}
	
	/**
	 * <p>
	 * Generic {@link RequestBody.Multipart} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class MultipartInboundData implements RequestBody.Multipart<Part> {

		private final Publisher<Part> parts;
		
		/**
		 * <p>
		 * Creates a multipart/form-data consumer with the specified source of parts.
		 * </p>
		 * 
		 * @param parameters the parameter publisher
		 */
		public MultipartInboundData(Publisher<Part> parts) {
			this.parts = parts;
		}

		@Override
		public Publisher<Part> stream() {
			return this.parts;
		}
	}
}

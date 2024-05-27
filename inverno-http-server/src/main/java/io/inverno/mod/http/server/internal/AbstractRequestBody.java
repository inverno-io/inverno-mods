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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.HttpServerException;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base {@link RequestBody} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 * 
 * @param <A> the request headers type
 */
public abstract class AbstractRequestBody<A extends AbstractRequestHeaders> implements RequestBody {

	private static final HttpServerException REQUEST_DISPOSED_ERROR = new StacklessHttpServerException("Request was disposed");
	
	private final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	private final MultipartDecoder<Part> multipartBodyDecoder;
	private final A headers;
	
	private final Sinks.Many<ByteBuf> dataSink;
	private Flux<ByteBuf> data;
	private boolean subscribed;

	/**
	 * The raw inbound data.
	 */
	protected InboundData<ByteBuf> rawData;
	/**
	 * The string inbound data.
	 */
	protected InboundData<CharSequence> stringData;
	/**
	 * The application/x-www-form-urlencoded data.
	 */
	protected RequestBody.UrlEncoded urlEncodedData;
	/**
	 * The multipart/form-data body data.
	 */
	protected RequestBody.Multipart<Part> multipartData;
	
	private Throwable cancelCause;

	/**
	 * <p>
	 * Creates a base request body.
	 * </p>
	 *
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param headers               the request headers
	 */
	public AbstractRequestBody(MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder, A headers) {
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		this.headers = headers;
		
		this.dataSink = Sinks.many().unicast().onBackpressureBuffer();
		this.data = Flux.defer(() -> {
			if(this.cancelCause != null) {
				return Mono.error(this.cancelCause);
			}
			return this.dataSink.asFlux()
				.doOnSubscribe(ign -> this.subscribed = true)
				.doOnDiscard(ByteBuf.class, ByteBuf::release);
		});
	}

	/**
	 * <p>
	 * Returns the request body data sink.
	 * </p>
	 * 
	 * <p>
	 * This is used by an {@link HttpConnection} to emit request data.
	 * </p>
	 * 
	 * @return the request body data sink
	 */
	public final Sinks.Many<ByteBuf> getDataSink() {
		return this.dataSink;
	}
	
	/**
	 * <p>
	 * Disposes the request body.
	 * </p>
	 * 
	 * <p>
	 * This methods tries to terminate the data sink with or without an error. If the data publisher was not subscribed, it is subscribed in order to release data.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	public final void dispose(Throwable cause) {
		if(this.cancelCause == null) {
			if(cause != null) {
				this.cancelCause = cause;
				this.dataSink.tryEmitError(cause);
			}
			else {
				this.cancelCause = REQUEST_DISPOSED_ERROR;
				this.dataSink.tryEmitComplete();
			}
			if(!this.subscribed) {
				try {
					this.data.subscribe(
						ByteBuf::release,
						ex -> {
							// TODO Should be ignored but can be logged as debug or trace log
						}
					);
				}
				catch(Throwable throwable) {
					// this could mean data have already been subscribed OR the publisher terminated with an error 
					// in any case data should have been released
				}
			}
		}
	}

	@Override
	public final RequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException {
		if(this.subscribed) {
			throw new IllegalStateException("Request data already consumed");
		}
		this.data = Flux.from(transformer.apply(this.data));
		return this;
	}

	@Override
	public InboundData<ByteBuf> raw() throws IllegalStateException {
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
	public UrlEncoded urlEncoded() throws IllegalStateException {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.urlEncodedData == null) {
			this.urlEncodedData = new UrlEncodedInboundData(this.urlEncodedBodyDecoder.decode(this.data, this.headers.getContentTypeHeader()));
		}
		return this.urlEncodedData;
	}
	
	@Override
	public Multipart<? extends Part> multipart() throws IllegalStateException {
		// We don't need to check whether another data method has been invoke since the data Flux is a unicast Flux, an IllegalStateSxception will be thrown if multiple subscriptions are made
		if(this.multipartData == null) {
			this.multipartData = new MultipartInboundData(this.multipartBodyDecoder.decode(this.data, this.headers.getContentTypeHeader()));
		}
		return this.multipartData;
	}
	
	/**
	 * <p>
	 * Generic raw {@link InboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	protected class RawInboundData implements InboundData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return AbstractRequestBody.this.data;
		}
	}
	
	/**
	 * <p>
	 * Generic string {@link InboundData} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	protected class StringInboundData implements InboundData<CharSequence> {

		@Override
		public Publisher<CharSequence> stream() {
			return AbstractRequestBody.this.data.map(buf -> {
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
	 * @since 1.10
	 */
	protected class UrlEncodedInboundData implements RequestBody.UrlEncoded {

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
	 * @since 1.10
	 */
	protected class MultipartInboundData implements RequestBody.Multipart<Part> {

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

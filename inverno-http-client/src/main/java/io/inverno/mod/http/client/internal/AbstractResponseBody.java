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
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.ResponseBody;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 *  <p>
 * Base {@link ResponseBody} implementation representing the body received from the connected remote endpoint.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.11
 */
public abstract class AbstractResponseBody implements ResponseBody {

	private static final HttpClientException RESPONSE_DISPOSED_ERROR = new StacklessHttpClientException("Response was disposed");
	
	private final Sinks.Many<ByteBuf> dataSink;
	private Flux<ByteBuf> data;
	private boolean subscribed;
	
	private AbstractResponseBody.RawInboundData rawData;
	private AbstractResponseBody.StringInboundData stringData;
	
	private Throwable cancelCause;

	/**
	 * <p>
	 * Creates an HTTP response body.
	 * </p>
	 */
	public AbstractResponseBody() {
		this.dataSink = Sinks.many().unicast().onBackpressureBuffer();
		this.data = Flux.defer(() -> {
			this.subscribed = true;
			if(this.cancelCause != null) {
				return Mono.error(this.cancelCause);
			}
			return this.dataSink.asFlux()
				.doOnDiscard(ByteBuf.class, ByteBuf::release);
		});
	}
	
	/**
	 * <p>
	 * Returns the response body data sink.
	 * </p>
	 *
	 * @return the response body data sink
	 */
	public final Sinks.Many<ByteBuf> getDataSink() {
		return this.dataSink;
	}

	/**
	 * <p>
	 * Disposes the response body.
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
			if(!this.subscribed) {
				try {
					this.dataSink.asFlux().subscribe(
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
			if(cause != null) {
				this.dataSink.tryEmitError(cause);
				this.cancelCause = cause;
			}
			else {
				this.dataSink.tryEmitComplete();
				this.cancelCause = RESPONSE_DISPOSED_ERROR;
			}
		}
	}
	
	@Override
	public ResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException {
		if(this.subscribed) {
			throw new IllegalStateException("Response data already consumed");
		}
		this.data = Flux.from(transformer.apply(this.data));
		return this;
	}

	@Override
	public InboundData<ByteBuf> raw() {
		if(this.rawData == null) {
			this.rawData = new AbstractResponseBody.RawInboundData();
		}
		return this.rawData;
	}

	@Override
	public InboundData<CharSequence> string() throws IllegalStateException {
		if(this.stringData == null) {
			this.stringData = new AbstractResponseBody.StringInboundData();
		}
		return this.stringData;
	}
	
	/**
	 * <p>
	 * Generic raw {@link InboundData} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.11
	 */
	private class RawInboundData implements InboundData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return AbstractResponseBody.this.data;
		}
	}
	
	/**
	 * <p>
	 * Generic string {@link InboundData} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.11
	 */
	private class StringInboundData implements InboundData<CharSequence> {

		@Override
		public Publisher<CharSequence> stream() {
			return Flux.from(AbstractResponseBody.this.data)
				.map(buf -> {
					try {
						return buf.toString(Charsets.DEFAULT);
					}
					finally {
						buf.release();
					}
				});
		}
	}
}

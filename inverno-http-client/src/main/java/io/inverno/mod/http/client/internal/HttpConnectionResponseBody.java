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
import io.inverno.mod.http.client.ResponseBody;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * The HTTP connection response body representing the response payload received after sending the request to the endpoint.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class HttpConnectionResponseBody implements ResponseBody {

	final Sinks.Many<ByteBuf> dataSink;
	private boolean subscribed;
	private boolean disposed;
	
	private Publisher<ByteBuf> data;
	
	private HttpConnectionResponseBody.RawInboundData rawData;
	private HttpConnectionResponseBody.StringInboundData stringData;

	/**
	 * <p>
	 * Creates an HTTP connection response body.
	 * </p>
	 */
	public HttpConnectionResponseBody() {
		this.dataSink = Sinks.many().unicast().onBackpressureBuffer();
		this.data = Flux.defer(() -> {
			if(this.disposed) {
				return Mono.error(new IllegalStateException("Response was disposed"));
			}
			return this.dataSink.asFlux()
				.doOnSubscribe(ign -> this.subscribed = true)
				.doOnDiscard(ByteBuf.class, ByteBuf::release);
		});
	}

	/**
	 * <p>
	 * Disposes the response body.
	 * </p>
	 * 
	 * <p>
	 * This method delegates to {@link #dispose(java.lang.Throwable) } with a null error.
	 * </p>
	 */
	void dispose() {
		this.dispose(null);
	}
	
	/**
	 * <p>
	 * Disposes the response body with the specified error.
	 * </p>
	 * 
	 * <p>
	 * This method drains received data if the response body data publisher hasn't been subscribed.
	 * </p>
	 * 
	 * <p>
	 * A non-null error indicates that the enclosing exchange did not complete successfully and that the error should be emitted by the response data publisher.
	 * </p>
	 * 
	 * @param error an error or null
	 */
	void dispose(Throwable error) {
		if(!this.disposed) {
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
			else if(error != null) {
				this.dataSink.tryEmitError(error);
			}
			else {
				this.dataSink.tryEmitComplete();
			}
			this.disposed = true;
		}
	}
	
	@Override
	public ResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalArgumentException {
		if(this.subscribed) {
			throw new IllegalStateException("Response data already consumed");
		}
		this.data = transformer.apply(this.data);
		return this;
	}
	
	@Override
	public InboundData<ByteBuf> raw() throws IllegalStateException {
		if(this.rawData == null) {
			this.rawData = new RawInboundData();
		}
		return this.rawData;
	}

	@Override
	public InboundData<CharSequence> string() throws IllegalStateException {
		if(this.stringData == null) {
			this.stringData = new StringInboundData();
		}
		return this.stringData;
	}
	
	/**
	 * <p>
	 * Generic raw {@link InboundData} implementation.
	 * </p>
	 *
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class RawInboundData implements InboundData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return HttpConnectionResponseBody.this.data;
		}
	}
	
	/**
	 * <p>
	 * Generic string {@link InboundData} implementation.
	 * </p>
	 *
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class StringInboundData implements InboundData<CharSequence> {

		@Override
		public Publisher<CharSequence> stream() {
			return Flux.from(HttpConnectionResponseBody.this.data).map(buf -> {
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

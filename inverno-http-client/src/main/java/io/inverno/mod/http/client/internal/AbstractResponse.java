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

import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.client.Response;
import io.inverno.mod.http.client.ResponseBody;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public abstract class AbstractResponse implements Response {

	private final InboundResponseHeaders responseHeaders;
	
	protected InboundHeaders responseTrailers;
	
	private boolean subscribed;
	private boolean disposed;
	
	private GenericResponseBody body;
	private Sinks.Many<ByteBuf> data;

	protected AbstractResponse(InboundResponseHeaders responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	
	@Override
	public InboundResponseHeaders headers() {
		return this.responseHeaders;
	}
	
	@Override
	public ResponseBody body() {
		if(this.body == null) {
			// create a many sink
			this.data = this.data();
			Flux<ByteBuf> responseBodyData = Flux.defer(() -> {
				if(this.disposed) {
					return Mono.error(new IllegalStateException("Response was disposed"));
				}
				return this.data.asFlux()
					.doOnSubscribe(ign -> this.subscribed = true)
					.doOnDiscard(ByteBuf.class, ByteBuf::release);
			});
			this.body = new GenericResponseBody(responseBodyData);
		}
		return this.body;
	}

	@Override
	public InboundHeaders trailers() {
		return this.responseTrailers;
	}
	
	public Sinks.Many<ByteBuf> data() {
		if(this.data == null) {
			this.data = Sinks.many().unicast().onBackpressureBuffer();
		}
		return this.data;
	}

	public boolean isDisposed() {
		return disposed;
	}
	
	public void dispose() {
		if(!this.subscribed) {
			// Try to drain and release buffered data 
			// when the datasink was already subscribed data are released in doOnDiscard (see #body())
			this.data.asFlux().subscribe(
				chunk -> chunk.release(), 
				ex -> {
					// TODO Should be ignored but can be logged as debug or trace log
				}
			);
		}
		this.disposed = true;
	}
}

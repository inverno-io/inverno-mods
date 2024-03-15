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

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * The HTTP connection request body resulting from sending the request to the endpoint.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public abstract class HttpConnectionRequestBody {
	
	private final Publisher<ByteBuf> data;
	private final boolean single;
	
	private boolean subscribed;

	/**
	 * <p>
	 * Creates an HTTP connection request body.
	 * </p>
	 * 
	 * @param data the raw data publisher
	 */
	public HttpConnectionRequestBody(Publisher<ByteBuf> data) {
		this.data = data;
		this.single = data instanceof Mono;
	}
	
	/**
	 * <p>
	 * Subscribes to the request payload data.
	 * </p>
	 * 
	 * <p>
	 * This is invoked when starting an exchange to send the data to the endpoint.
	 * </p>
	 * 
	 * @param s a subscriber
	 */
	public void dataSubscribe(Subscriber<? super ByteBuf> s) {
		// No need to synchronize this code since we are in an EventLoop
		if(this.subscribed) {
			throw new IllegalStateException("Request data already subscribed");
		}
		if(this.data == null) {
			// should be same as empty()
			s.onComplete();
		}
		else {
			Flux.from(this.data).doOnDiscard(ByteBuf.class, ByteBuf::release).subscribe(s);
		}
		this.subscribed = true;
	}
	
	/**
	 * <p>
	 * Determines whether the request payload data publisher is single (i.e. a {@code Mono}).
	 * </p>
	 * 
	 * @return true if the payload data publisher is single, false otherwise
	 */
	public boolean isSingle() {
		return this.single;
	}
}

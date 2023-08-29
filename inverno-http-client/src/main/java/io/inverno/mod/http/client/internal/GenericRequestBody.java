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

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import io.inverno.mod.http.client.InterceptableRequestBody;

/**
 * <p>
 * Generic request body used internally in the {@link GenericRequestBodyConfigurator} to set the payload data to send to the endpoint.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see GenericRequestBodyConfigurator
 * @see Http1xRequestBody
 */
public class GenericRequestBody implements InterceptableRequestBody {
	
	public static final GenericRequestBody EMPTY;
	
	static {
		EMPTY = new GenericRequestBody();
		EMPTY.setData(Mono.empty());
	}

	private MonoSink<Publisher<ByteBuf>> dataEmitter;
	private Publisher<ByteBuf> data;
	
	private boolean subscribed;
	private boolean dataSet;
	private boolean single;
	
	private Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer;
	
	/**
	 * <p>
	 * Sets the payload draw ata publisher of the request body.
	 * </p>
	 * 
	 * @param data a raw data publisher
	 * 
	 * @throws IllegalStateException if the request payload was already sent to the endpoint
	 */
	public final void setData(Publisher<ByteBuf> data) throws IllegalStateException {
		if(this.subscribed && this.dataSet) {
			throw new IllegalStateException("Response data already posted");
		}

		Publisher<ByteBuf> transformedData = this.transformer != null ? this.transformer.apply(data) : data;

		if(transformedData instanceof Mono) {
			this.single = true;
		}
		if(this.dataEmitter != null) {
			this.dataEmitter.success(transformedData);
		}
		else {
			this.data = transformedData;
		}
		this.dataSet = true;
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
			this.data = Flux.switchOnNext(Mono.<Publisher<ByteBuf>>create(emitter -> this.dataEmitter = emitter));
		}
		Flux.from(this.data).doOnDiscard(ByteBuf.class, ByteBuf::release).subscribe(s);
		this.subscribed = true;
	}
	
	@Override
	public InterceptableRequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		if(this.subscribed && this.dataSet) {
			throw new IllegalStateException("Request data already sent");
		}

		if(this.transformer == null) {
			this.transformer = transformer;
		}
		else {
			this.transformer = this.transformer.andThen(transformer);
		}

		if(this.dataSet) {
			this.data = transformer.apply(this.data);
		}
		return this;
	}

	/**
	 * <p>
	 * Determines whether the request payload data publisher is single (i.e. a {@code Mono}).
	 * </p>
	 * 
	 * @return true if the payload data publisher is single, false otherwise
	 */
	public boolean isSingle() {
		return single;
	}
}

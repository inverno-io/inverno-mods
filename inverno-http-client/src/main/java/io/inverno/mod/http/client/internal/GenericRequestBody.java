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

import io.inverno.mod.http.client.RequestBody;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericRequestBody implements RequestBody {
	
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
	
	public final void setData(Publisher<ByteBuf> data) {
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
	public RequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		if(this.subscribed && this.dataSet) {
			throw new IllegalStateException("Response data already posted");
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

	public boolean isSingle() {
		return single;
	}
}

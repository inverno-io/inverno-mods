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

package io.inverno.mod.http.base.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SignalType;

/**
 * <p>
 * A sequencer of outbound data.
 * </p>
 * 
 * <p>
 * It is basically used to buffer outbound data before sending them in order to avoid small data chunk and optimize network performances.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class OutboundDataSequencer {

	/**
	 * The default outbound buffer capacity.
	 */
	public static final int DEFAULT_BUFFER_CAPACITY = 8192;
	
	/**
	 * The outbound buffer capacity.
	 */
	private int bufferCapacity = DEFAULT_BUFFER_CAPACITY;

	/**
	 * <p>
	 * Sets the outbound buffer capacity.
	 * </p>
	 * 
	 * @param bufferCapacity a buffer capacity
	 */
	public void setBufferCapacity(int bufferCapacity) {
		this.bufferCapacity = bufferCapacity;
	}
	
	/**
	 * <p>
	 * Sequences the specified flux of data by buffering them up to the buffer capacity before emitting them in the resulting flux.
	 * </p>
	 * 
	 * @param data the data to sequence
	 * 
	 * @return a flux of buffered data
	 */
	public Flux<ByteBuf> sequence(Flux<ByteBuf> data) {
		return Flux.create(dataSink -> {
			data.subscribe(new DataSubscriber(dataSink));
		});
	}
	
	/**
	 * <p>
	 * The subscriber used to bufferize outbound data.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class DataSubscriber extends BaseSubscriber<ByteBuf> {

		private final FluxSink<ByteBuf> dataSink;
		
		private ByteBuf currentBuffer;
		
		public DataSubscriber(FluxSink<ByteBuf> dataSink) {
			this.dataSink = dataSink;
		}
		
		@Override
		protected void hookOnNext(ByteBuf buffer) {
			if(this.currentBuffer == null) {
				this.currentBuffer = buffer;
			}
			else {
				this.currentBuffer = Unpooled.wrappedBuffer(this.currentBuffer, buffer);
			}
			
			if(this.currentBuffer.readableBytes() > OutboundDataSequencer.this.bufferCapacity) {
				this.dataSink.next(this.currentBuffer.readRetainedSlice(OutboundDataSequencer.this.bufferCapacity));
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			this.dataSink.error(throwable);
		}

		@Override
		protected void hookOnComplete() {
			if(this.currentBuffer != null && this.currentBuffer.readableBytes() > 0) {
				this.dataSink.next(this.currentBuffer);
			}
			this.dataSink.complete();
		}

		@Override
		protected void hookFinally(SignalType type) {
		}
	}
}

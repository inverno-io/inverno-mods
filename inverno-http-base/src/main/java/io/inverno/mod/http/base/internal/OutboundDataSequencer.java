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
import java.util.Iterator;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A sequencer of outbound data.
 * </p>
 * 
 * <p>
 * It is basically used to buffer outbound data before sending them in order to avoid small data chunk and optimize network performances.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class OutboundDataSequencer implements Function<Flux<ByteBuf>, Flux<ByteBuf>> {

	/**
	 * The default outbound buffer capacity.
	 */
	public static final int DEFAULT_BUFFER_CAPACITY = 8192;

	/**
	 * The outbound buffer capacity.
	 */
	private final int bufferCapacity;

	private int accumulatedSize;

	private ByteBuf retainedBuffer;

	/**
	 * <p>
	 * Creates an outbound data sequencer with {@link #DEFAULT_BUFFER_CAPACITY}.
	 * </p>
	 */
	public OutboundDataSequencer() {
		this(DEFAULT_BUFFER_CAPACITY);
	}
	
	/**
	 * <p>
	 * Creates an outbound data sequencer.
	 * </p>
	 * 
	 * @param bufferCapacity the buffer capacity
	 */
	public OutboundDataSequencer(int bufferCapacity) {
		this.bufferCapacity = bufferCapacity;
	}
	
	@Override
	public Flux<ByteBuf> apply(Flux<ByteBuf> data) {
		return data.bufferUntil(buffer -> {
					this.accumulatedSize += buffer.readableBytes();
					return this.accumulatedSize >= this.bufferCapacity;
				}
			)
			.map(accBuffers -> {
				ByteBuf[] buffers;
				int index;
				if(this.retainedBuffer == null) {
					buffers = new ByteBuf[accBuffers.size()];
					index = 0;
				}
				else {
					buffers = new ByteBuf[accBuffers.size() + 1];
					buffers[0] = this.retainedBuffer;
					index = 1;
				}

				for(ByteBuf b : accBuffers) {
					buffers[index++] = b;
				}

				ByteBuf buffer = Unpooled.wrappedBuffer(buffers);
				if(this.accumulatedSize > this.bufferCapacity) {
					this.retainedBuffer = buffer;
					buffer = buffer.readRetainedSlice(this.bufferCapacity);
					this.accumulatedSize = this.retainedBuffer.readableBytes();

				}
				else {
					this.retainedBuffer = null;
					this.accumulatedSize = 0;
				}
				return buffer;
			})
			.concatWith(Flux.defer(() -> Mono.justOrEmpty(this.retainedBuffer)
				.flatMapIterable(buffer -> () -> new Iterator<>() {
					@Override
					public boolean hasNext() {
						return buffer.isReadable();
					}

					@Override
					public ByteBuf next() {
						return buffer.readRetainedSlice(Math.min(buffer.readableBytes(), bufferCapacity));
					}
				}))
			);
	}
}

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
package io.winterframework.mod.base.resource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Base implementation for {@link AsyncResource}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AsyncResource
 */
public abstract class AbstractAsyncResource extends AbstractResource implements AsyncResource {

	/**
	 * The default read buffer capacity.
	 */
	public static final int DEFAULT_READ_BUFFER_CAPACITY = 8192;
	
	private int readBufferCapacity = DEFAULT_READ_BUFFER_CAPACITY;
	
	private static ExecutorService defaultExecutor;
	
	private ExecutorService executor;
	
	/**
	 * <p>
	 * Creates an asnc resource.
	 * </p>
	 */
	public AbstractAsyncResource() {
	}

	/**
	 * <p>
	 * Creates an async resource with the specified media type service.
	 * </p>
	 * 
	 * @param mediaTypeService a media type service
	 */
	protected AbstractAsyncResource(MediaTypeService mediaTypeService) {
		super(mediaTypeService);
	}

	/**
	 * <p>
	 * Sets the buffer read capacity.
	 * </p>
	 * 
	 * @param readBufferCapacity the buffer read capacity to set
	 */
	public void setReadBufferCapacity(int readBufferCapacity) {
		this.readBufferCapacity = readBufferCapacity;
	}
	
	private static ExecutorService getDefaultExecutor() {
		if(defaultExecutor == null) {
			// TODO using such thread pool might be dangerous since there are no thread limit...
			defaultExecutor = Executors.newCachedThreadPool();
			
			/*defaultExecutor = new ThreadPoolExecutor(0, 20,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>());*/
		}
		return defaultExecutor;
	}
	
	@Override
	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}
	
	@Override
	public ExecutorService getExecutor() {
		return this.executor != null ? this.executor : getDefaultExecutor();
	}
	
	/**
	 * <p>
	 * Thrown when the end of the file is reached when reading a resource.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	protected static class EndOfFileException extends RuntimeException {

		private static final long serialVersionUID = -959922787939538588L;
	}
	
	@Override
	public Optional<Publisher<ByteBuf>> read() {
		return this.openReadableByteChannel().map(channel -> {
			return Flux.generate(
				() -> Long.valueOf(0),	
				(position, sink) -> {
					sink.next(position);
					return position + this.readBufferCapacity;
				}
			)
			.map(position -> {
				ByteBuffer data = ByteBuffer.allocate(this.readBufferCapacity);
				try {
					if(channel.read(data) == -1) {
						throw new EndOfFileException();
					}
				}
				catch (IOException e) {
					throw Exceptions.propagate(e);
				}
				data.flip();
				return Unpooled.wrappedBuffer(data);
			})
			.onErrorResume(EndOfFileException.class, ex -> Mono.empty())
			.doOnTerminate(() -> {
				try {
					channel.close();
				}
				catch (IOException e) {
				}
			})
			.subscribeOn(Schedulers.fromExecutor(this.getExecutor()));
		});
	}
	
	@Override
	public Optional<Publisher<Integer>> write(Publisher<ByteBuf> data, boolean append, boolean createParents) {
		return this.openWritableByteChannel(append, createParents)
			.map(channel -> Flux.from(data)
				.concatMap(chunk -> Mono.<Integer>create(sink -> {
						try {
							sink.success(channel.write(chunk.nioBuffer()));
						} 
						catch (IOException e) {
							sink.error(e);
						}
						finally {
							chunk.release();
						}
					})
					.subscribeOn(Schedulers.fromExecutor(this.getExecutor()))
				)
				.doOnTerminate(() -> {
					try {
						channel.close();
					}
					catch (IOException e) {
					}
				})
			);
	}
}

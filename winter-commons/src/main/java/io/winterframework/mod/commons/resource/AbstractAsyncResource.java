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
package io.winterframework.mod.commons.resource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractAsyncResource extends AbstractResource implements AsyncResource {

	public static final int DEFAULT_READ_BUFFER_CAPACITY = 8192;
	
	protected int readBufferCapacity = DEFAULT_READ_BUFFER_CAPACITY;
	
	private static ExecutorService defaultExecutor;
	
	private ExecutorService executor;
	
	public AbstractAsyncResource() {
	}

	protected AbstractAsyncResource(MediaTypeService mediaTypeService) {
		super(mediaTypeService);
	}

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
	
	protected static class EndOfFileException extends RuntimeException {

		private static final long serialVersionUID = -959922787939538588L;
		
	}
	
	@Override
	public Optional<Flux<ByteBuf>> read() throws IOException {
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
	public Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append, boolean createParents) throws IOException {
		return this.openWritableByteChannel(append, createParents)
			.map(channel -> data
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
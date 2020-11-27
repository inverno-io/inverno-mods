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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

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
public class PathResource extends AbstractAsyncResource {
	
	public static final int DEFAULT_READ_BUFFER_CAPACITY = 8192;
	
	private int readBufferCapacity = DEFAULT_READ_BUFFER_CAPACITY;

	private Path path;
	
	public PathResource(Path path) {
		this(path, null);
	}
	
	protected PathResource(Path path, MediaTypeService mediaTypeService) {
		super(mediaTypeService);
		this.path = Objects.requireNonNull(path.normalize());
	}
	
	public void setReadBufferCapacity(int readBufferCapacity) {
		this.readBufferCapacity = readBufferCapacity;
	}
	
	@Override
	public String getFilename() throws IOException {
		return this.path.getFileName().toString();
	}
	
	@Override
	public String getMediaType() throws IOException {
		return this.getMediaTypeService().getForPath(this.path);
	}
	
	@Override
	public URI getURI() {
		return this.path.toUri();
	}
	
	@Override
	public Boolean exists() throws IOException {
		return Files.exists(this.path);
	}

	@Override
	public Long size() throws IOException {
		return Files.size(this.path);
	}
	
	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() throws IOException {
		try {
			if(Files.isReadable(this.path)) {
				return Optional.of(FileChannel.open(this.path, StandardOpenOption.READ));
			}
			return Optional.empty();
		}
		catch(FileSystemException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws IOException {
		try {
			if(createParents) {
				Files.createDirectories(this.path.getParent());
			}
			if(append) {
				return Optional.of(FileChannel.open(this.path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND));		
			}
			else {
				return Optional.of(FileChannel.open(this.path, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
			}
		}
		catch (FileSystemException e) {
			return Optional.empty();
		}
	}
	
	@Override
	public FileTime lastModified() throws IOException {
		try {
			return Files.getLastModifiedTime(this.path);
		}
		catch (FileSystemException e) {
			return null;
		}
	}
	
	private static class EndOfFileException extends RuntimeException {

		private static final long serialVersionUID = -959922787939538588L;
		
	}
	
	// 10148354
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
				return Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(data));
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
		
		
		/*if(Files.isReadable(this.path)) {
			return Optional.of(Flux.create(emitter -> {
				AsynchronousFileChannel channel;
				try {
					channel = AsynchronousFileChannel.open(this.path, Set.of(StandardOpenOption.READ), this.getExecutor());
					
					AtomicLong pendingRequest = new AtomicLong(0);
					AtomicLong position = new AtomicLong(0);
					ByteBuffer readBuffer = ByteBuffer.allocate(this.readBufferCapacity);
					CompletionHandler<Integer, Void> readHandler = new CompletionHandler<Integer, Void>() {
						
						private void emitNext() {
							readBuffer.flip();
							ByteBuf data = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(readBuffer));
							if(data.isReadable()) {
								emitter.next(data);
							}
							readBuffer.clear();
						}
						
						@Override
						public void failed(Throwable exc, Void attachment) {
							emitter.error(exc);
						}
						
						@Override
						public void completed(Integer result, Void attachment) {
							if(result == -1) {
								this.emitNext();
								emitter.complete();
							}
							else {
								if(!readBuffer.hasRemaining()) {
									this.emitNext();
								}
								long newPosition = position.addAndGet(result);
								if(pendingRequest.decrementAndGet() > 0) {
									channel.read(readBuffer, newPosition, null, this);
								}
							}
						}
					};
					
					emitter.onRequest(n -> {
						if(pendingRequest.getAndAdd(n) == 0) {
							channel.read(readBuffer, position.get(), null, readHandler);
						}
					});
					
					emitter.onDispose(() -> {
						try {
							channel.close();
						}
						catch (IOException e) {
							
						}
					});
				} 
				catch (IOException e) {
					emitter.error(e);
				}
			}));
		}
		return Optional.empty();*/
	}
	
	@Override
	public Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append, boolean createParents) throws IOException {
		return this.openWritableByteChannel(append, createParents)
			.map(channel -> data.concatMap(
				chunk -> Mono.<Integer>create(sink -> {
						try {
							sink.success(channel.write(chunk.nioBuffer()));
						} 
						catch (IOException e) {
							sink.error(e);
						}
					}).subscribeOn(Schedulers.fromExecutor(this.getExecutor()))
				)
			);
	}

	@Override
	public boolean delete() throws IOException {
		try {
			return Files.deleteIfExists(this.path);
		}
		catch (FileSystemException e) {
			return false;
		}
	}
	
	@Override
	public void close() throws IOException {
		
	}
	
	@Override
	public Resource resolve(URI uri) throws IOException {
		PathResource resolvedResource = new PathResource(Paths.get(this.getURI().resolve(uri.normalize())), this.getMediaTypeService());
		resolvedResource.setExecutor(this.getExecutor());
		return resolvedResource;
	}
}

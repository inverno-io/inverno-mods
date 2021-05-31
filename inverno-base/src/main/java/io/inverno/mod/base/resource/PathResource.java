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
package io.inverno.mod.base.resource;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a path and
 * looks up data on the file system.
 * </p>
 * 
 * <p>
 * A typical usage is:
 * </p>
 * 
 * <blockquote><pre>
 * PathResource resource = new PathResource(Paths.get("/path/to/resource"));
 * ...
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AsyncResource
 */
public class PathResource extends AbstractAsyncResource {
	
	private Path path;
	
	/**
	 * <p>
	 * Creates a path resource with the specified path.
	 * </p>
	 * 
	 * @param path the resource path
	 */
	public PathResource(Path path) {
		this(path, null);
	}
	
	/**
	 * <p>
	 * Creates a path resource with the specified path and media type service.
	 * </p>
	 * 
	 * @param path             the resource path
	 * @param mediaTypeService the media type service
	 */
	public PathResource(Path path, MediaTypeService mediaTypeService) {
		super(mediaTypeService);
		this.path = Objects.requireNonNull(path.normalize());
	}
	
	@Override
	public String getFilename() {
		return this.path.getFileName().toString();
	}
	
	@Override
	public String getMediaType() {
		return this.getMediaTypeService().getForPath(this.path);
	}
	
	@Override
	public URI getURI() {
		return this.path.toUri();
	}
	
	@Override
	public Optional<Boolean> exists() {
		return Optional.of(Files.exists(this.path));
	}
	
	@Override
	public Optional<Boolean> isFile() {
		return Optional.of(Files.isRegularFile(this.path));
	}

	@Override
	public Optional<Long> size() {
		try {
			return Optional.of(Files.size(this.path));
		} 
		catch (IOException e) {
			// TODO log debug
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() {
		try {
			if(Files.isReadable(this.path)) {
				return Optional.of(FileChannel.open(this.path, StandardOpenOption.READ));
			}
			return Optional.empty();
		}
		catch(IOException e) {
			// TODO log debug
			return Optional.empty();
		}
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) {
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
		catch (IOException e) {
			// TODO log debug
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<FileTime> lastModified() {
		try {
			return Optional.of(Files.getLastModifiedTime(this.path));
		}
		catch (IOException e) {
			// TODO log debug
			return Optional.empty();
		}
	}
	
	// Following implementation is using AsyncrhonousFileChannel
	// This seem less performant than the reactive implementation
	/*@Override
	public Optional<Flux<ByteBuf>> read() throws IOException {
		if(Files.isReadable(this.path)) {
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
		return Optional.empty();
	}*/

	@Override
	public boolean delete() {
		try {
			return Files.deleteIfExists(this.path);
		}
		catch (IOException e) {
			// TODO log debug
			return false;
		}
	}
	
	@Override
	public void close() {
		
	}
	
	@Override
	public Resource resolve(Path path) {
		PathResource resolvedResource = new PathResource(this.path.resolve(path), this.getMediaTypeService());
		resolvedResource.setExecutor(this.getExecutor());
		return resolvedResource;
	}
}

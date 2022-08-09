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

import io.netty.buffer.ByteBuf;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * <p>
 * A resource represents an abstraction of an actual resource like a file, an
 * entry in a zip/jar file, a on the classpath...
 * </p>
 * 
 * <p>
 * Resource data can be read using a {@link ReadableByteChannel} assuming the
 * resource is readable:
 * </p>
 * 
 * <pre>{@code
 * try (Resource resource = new FileResource("/path/to/file")) {
 *     String content = resource.openReadableByteChannel()
 *         .map(channel -> {
 *             try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
 *                 ByteBuffer buffer = ByteBuffer.allocate(256);
 *                 while (channel.read(buffer) > 0) {
 *                     out.write(buffer.array(), 0, buffer.position());
 *                     buffer.clear();
 *                 }
 *                 return new String(out.toByteArray(), Charsets.UTF_8);
 *             }
 *             finally {
 *                 channel.close();
 *             }
 *         })
 *         .orElseThrow(() -> new IllegalStateException("Resource is not readable"));
 * }
 * }</pre>
 * 
 * <p>
 * Resource data can also be read in a reactive way:
 * </p>
 * 
 * <pre>{@code
 * try(Resource resource = new FileResource("/path/to/resource")) {
 *     String content = resource.read()
 *         .map(data -> {
 *             return data
 *                 .map(chunk -> {
 *                     try {
 *                         return chunk.toString(Charsets.UTF_8);
 *                     }
 *                     finally {
 *                         chunk.release();
 *                     }
 *                 })
 *                 .collect(Collectors.joining())
 *                 .block();
 *             })
 *             .orElseThrow(() -> new IllegalStateException("Resource is not readable"));
 * }
 * }</pre>
 * 
 * <p>
 * Data can be written to a resource using a {@link WritableByteChannel}
 * assuming the resource is writable:
 * </p>
 * 
 * <pre>{@code
 * try (Resource resource = new FileResource("/path/to/file")) {
 *     resource.openReadableByteChannel()
 *         .ifPresentOrElse(
 *             channel -> {
 *                 try {
 *                     ByteBuffer buffer = ByteBuffer.wrap("Hello world".getBytes(Charsets.UTF_8));
 *                     channel.write(buffer);
 *                 }
 *                 finally {
 *                     channel.close();
 *                 }
 *             },
 *             () -> {
 *                 throw new IllegalStateException("Resource is not writable");
 *             }
 *         );
 * }
 * }</pre>
 * 
 * <p>
 * Data can also be written to a resource in a reactive way:
 * </p>
 * 
 * <pre>{@code
 * try (Resource resource = new FileResource("/path/to/resource")) {
 *     resource.write(Flux.just(Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("Hello world".getBytes(Charsets.UTF_8)))))
 *         .ifPresentOrElse(result -> {
 *             int nbBytes = result.collect(Collectors.summingInt(i -> i)).block();
 *             System.out.println(nbBytes + " bytes written");
 *         }, () -> {
 *             throw new IllegalStateException("Resource is not writable");
 *         });
 * }
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface Resource extends AutoCloseable {

	/**
	 * <p>
	 * Returns the resource file name.
	 * </p>
	 * 
	 * @return the resource file name
	 * @throws ResourceException if there was an error resolving the resource file
	 *                           name
	 */
	String getFilename() throws ResourceException;
	
	/**
	 * <p>
	 * Returns the resource media type.
	 * </p>
	 * 
	 * @return the resource media type or null if it couldn't be determined
	 * @throws ResourceException if there was an error resolving the resource media
	 *                           type
	 */
	String getMediaType() throws ResourceException;
	
	/**
	 * <p>Returns the resource URI.</p>
	 * 
	 * @return the resource URI
	 */
	URI getURI();
	
	/**
	 * <p>
	 * Determines whether this resource represents a file.
	 * </p>
	 * 
	 * <p>
	 * A file resource is a resource that can be accessed through a
	 * {@link FileChannel}.
	 * </p>
	 * 
	 * @return an optional returning true if the resource is a file, false otherwise
	 *         or an empty optional if it couldn't be determined
	 * @throws ResourceException if there was an error determining whether the
	 *                           resource is a file
	 */
	Optional<Boolean> isFile() throws ResourceException;
	
	/**
	 * <p>
	 * Determines whether the resource exists.
	 * </p>
	 * 
	 * @return an optional returning true if the resource exists, false otherwise
	 *         or an empty optional if existence couldn't be determined
	 * @throws ResourceException if there was an error determining resource
	 *                           existence
	 */
	Optional<Boolean> exists() throws ResourceException;

	/**
	 * <p>
	 * Returns the resource last modified time stamp.
	 * </p>
	 * 
	 * @return an optional returning the resource last modified time stamp or an
	 *         empty optional if it couldn't be determined
	 * @throws ResourceException if there was an error resolving resource last
	 *                           modified time stamp
	 */
	Optional<FileTime> lastModified() throws ResourceException;
	
	/**
	 * <p>
	 * Returns the resource content size.
	 * </p>
	 * 
	 * @return an optional returning the resource content size or an empty optional
	 *         if it couldn't be determined
	 * @throws ResourceException if there was an error resolving resource content
	 *                           size
	 */
	Optional<Long> size() throws ResourceException;
	
	/**
	 * <p>
	 * Opens a readable byte channel to the resource.
	 * </p>
	 * 
	 * @return an optional returning a readable byte channel or an empty optional if
	 *         the resource is not readable
	 * @throws ResourceException if there was an error opening the readable byte
	 *                           channel
	 */
	Optional<ReadableByteChannel> openReadableByteChannel() throws ResourceException;
	
	/**
	 * <p>
	 * Opens a writable byte channel to the resource.
	 * </p>
	 * 
	 * @return an optional returning a writable byte channel or an empty optional if
	 *         the resource is not writable
	 * @throws ResourceException if there was an error opening the writable byte
	 *                           channel
	 */
	default Optional<WritableByteChannel> openWritableByteChannel() throws ResourceException {
		return this.openWritableByteChannel(false);
	}
	
	/**
	 * <p>
	 * Opens a writable byte channel to the resource that will append or not content
	 * to an existing resource.
	 * </p>
	 * 
	 * @param append true to append content to an existing resource
	 * 
	 * @return an optional returning a writable byte channel or an empty optional if
	 *         the resource is not writable
	 * @throws ResourceException if there was an error opening the writable byte
	 *                           channel
	 */
	default Optional<WritableByteChannel> openWritableByteChannel(boolean append) throws ResourceException {
		return this.openWritableByteChannel(append, true);
	}
	
	/**
	 * <p>
	 * Opens a writable byte channel to the resource that will append or not content
	 * to an existing resource and create or not missing parent directories.
	 * </p>
	 * 
	 * @param append        true to append content to an existing resource
	 * @param createParents true to create missing parent directories
	 * 
	 * @return an optional returning a writable byte channel or an empty optional if
	 *         the resource is not writable
	 * @throws ResourceException if there was an error opening the writable byte
	 *                           channel
	 */
	Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws ResourceException;
	
	/**
	 * <p>
	 * Reads the resource in a reactive way.
	 * </p>
	 * 
	 * @return an optional returning a stream of ByteBuf or an empty optional if the
	 *         resource is not readable
	 * @throws ResourceException if there was an error reading the resource
	 */
	Optional<Publisher<ByteBuf>> read() throws ResourceException;
	
	/**
	 * <p>
	 * Writes content to the resource in a reactive way.
	 * </p>
	 * 
	 * @param data the stream of data to write
	 * 
	 * @return an optional returning a stream of integer emitting number of bytes
	 *         written or an empty optional if the resource is not writable
	 * @throws ResourceException if there was an error writing to the resource
	 */
	default Optional<Publisher<Integer>> write(Publisher<ByteBuf> data) throws ResourceException {
		return this.write(data, false);
	}
	
	/**
	 * <p>
	 * Writes content to the resource in a reactive way appending or not content to
	 * an existing resource.
	 * </p>
	 * 
	 * @param data   the stream of data to write
	 * @param append true to append content to an existing resource
	 * 
	 * @return an optional returning a stream of integer emitting number of bytes
	 *         written or an empty optional if the resource is not writable
	 * @throws ResourceException if there was an error writing to the resource
	 */
	default Optional<Publisher<Integer>> write(Publisher<ByteBuf> data, boolean append) throws ResourceException {
		return this.write(data, append, true);
	}
	
	/**
	 * <p>
	 * Writes content to the resource in a reactive way appending or not content to
	 * an existing resource and create or not missing parent directories.
	 * </p>
	 * 
	 * @param data          the stream of data to write
	 * @param append        true to append content to an existing resource
	 * @param createParents true to create missing parent directories
	 * 
	 * @return an optional returning a stream of integer emitting number of bytes
	 *         written or an empty optional if the resource is not writable
	 * @throws ResourceException if there was an error writing to the resource
	 */
	Optional<Publisher<Integer>> write(Publisher<ByteBuf> data, boolean append, boolean createParents) throws ResourceException;
	
	/**
	 * <p>
	 * Deletes the resource.
	 * </p>
	 * 
	 * @return true if the resource had been deleted, false otherwise
	 * @throws ResourceException if there was an error deleting to the resource
	 */
	boolean delete() throws ResourceException;
	
	/**
	 * <p>
	 * Resolves the specified URI against the resource URI as defined by
	 * {@link Path#resolve(Path)}.
	 * </p>
	 * 
	 * @param path the path to resolve
	 * 
	 * @return a new resource resulting from the resolution of the specified path
	 *         against the resource
	 * @throws ResourceException if there was an error resolving the resource
	 */
	Resource resolve(Path path) throws ResourceException;
	
	/**
	 * <p>
	 * Resolves the specified path against the resource URI as defined by
	 * {@link Path#resolve(String)}.
	 * </p>
	 * 
	 * @param path the path to resolve
	 * 
	 * @return a new resource resulting from the resolution of the specified path
	 *         against the resource
	 * @throws ResourceException if there was an error resolving the resource
	 */
	default Resource resolve(String path) throws ResourceException {
		return this.resolve(Path.of(path));
	}
	
	@Override
	void close();
}

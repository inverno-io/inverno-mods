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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public interface Resource extends AutoCloseable {

	String getFilename() throws ResourceException;
	
	String getMediaType() throws ResourceException;
	
	URI getURI();
	
	boolean isFile() throws ResourceException;
	
	Optional<Boolean> exists() throws ResourceException;

	Optional<FileTime> lastModified() throws ResourceException;
	
	Optional<Long> size() throws ResourceException;
	
	Optional<ReadableByteChannel> openReadableByteChannel() throws ResourceException;
	
	default Optional<WritableByteChannel> openWritableByteChannel() throws ResourceException {
		return this.openWritableByteChannel(false);
	}
	
	default Optional<WritableByteChannel> openWritableByteChannel(boolean append) throws ResourceException {
		return this.openWritableByteChannel(append, true);
	}
	
	Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws ResourceException;
	
	Optional<Flux<ByteBuf>> read() throws ResourceException;
	
	default Optional<Flux<Integer>> write(Flux<ByteBuf> data) throws ResourceException {
		return this.write(data, false);
	}
	
	default Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append) throws ResourceException {
		return this.write(data, append, true);
	}
	
	Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append, boolean createParents) throws ResourceException;
	
	boolean delete() throws ResourceException;
	
	Resource resolve(URI uri) throws ResourceException;
	
	default Resource resolve(String path) throws ResourceException {
		try {
			return this.resolve(new URI(path));
		} 
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	void close();
}

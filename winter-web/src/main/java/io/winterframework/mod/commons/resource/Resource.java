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

import java.io.Closeable;
import java.io.IOException;
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
public interface Resource extends Closeable {

	String getFilename() throws IOException;
	
	String getMediaType() throws IOException;
	
	URI getURI();
	
	Boolean exists() throws IOException;

	FileTime lastModified() throws IOException;
	
	Long size() throws IOException;
	
	Optional<ReadableByteChannel> openReadableByteChannel() throws IOException;
	
	default Optional<WritableByteChannel> openWritableByteChannel() throws IOException {
		return this.openWritableByteChannel(false);
	}
	
	default Optional<WritableByteChannel> openWritableByteChannel(boolean append) throws IOException {
		return this.openWritableByteChannel(append, true);
	}
	
	Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws IOException;
	
	Optional<Flux<ByteBuf>> read() throws IOException;
	
	default Optional<Flux<Integer>> write(Flux<ByteBuf> data) throws IOException {
		return this.write(data, false);
	}
	
	default Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append) throws IOException {
		return this.write(data, append, true);
	}
	
	Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append, boolean createParents) throws IOException;
	
	boolean delete() throws IOException;
	
	Resource resolve(URI uri) throws IOException;
	
	default Resource resolve(String path) throws URISyntaxException, IOException {
		return this.resolve(new URI(path));
	}
}

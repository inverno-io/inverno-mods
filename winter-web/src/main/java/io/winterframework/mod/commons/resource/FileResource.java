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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public class FileResource extends AbstractAsyncResource {

	public static final String SCHEME_FILE = "file";
	
	private PathResource pathResource;
	
	public FileResource(URI uri) throws IllegalArgumentException {
		this(uri, null);
	}
	
	public FileResource(File file) {
		this(file, null);
	}
	
	public FileResource(String pathname) {
		this(pathname, null);
	}
	
	protected FileResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.pathResource = new PathResource(Paths.get(this.checkUri(uri)), mediaTypeService);
	}
	
	protected FileResource(String pathname, MediaTypeService mediaTypeService) {
		this(new File(pathname), mediaTypeService);
	}
	
	protected FileResource(File file, MediaTypeService mediaTypeService) {
		super(mediaTypeService);
		this.pathResource = new PathResource(Objects.requireNonNull(file.getAbsoluteFile()).toPath(), mediaTypeService);
	}
	
	@Override
	public void setExecutor(ExecutorService executor) {
		this.pathResource.setExecutor(executor);
	}
	
	private URI checkUri(URI uri) throws IllegalArgumentException {
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_FILE)) {
			throw new IllegalArgumentException("Not a " + SCHEME_FILE + " uri");
		}
		return uri.normalize();
	}
	@Override
	public String getFilename() throws IOException {
		return this.pathResource.getFilename();
	}
	
	@Override
	public String getMediaType() throws IOException {
		return this.pathResource.getMediaType();
	}
	
	@Override
	public URI getURI() {
		return this.pathResource.getURI();
	}
	
	@Override
	public Boolean exists() throws IOException {
		return this.pathResource.exists();
	}

	@Override
	public FileTime lastModified() throws IOException {
		return this.pathResource.lastModified();
	}
	
	@Override
	public Long size() throws IOException {
		return this.pathResource.size();
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() throws IOException {
		return this.pathResource.openReadableByteChannel();
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws IOException {
		return this.pathResource.openWritableByteChannel(append, createParents);
	}

	@Override
	public Optional<Flux<ByteBuf>> read() throws IOException {
		return this.pathResource.read();
	}
	
	@Override
	public Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append, boolean createParents) throws IOException {
		return this.pathResource.write(data, append, createParents);
	}
	
	@Override
	public boolean delete() throws IOException {
		return this.pathResource.delete();
	}
	
	@Override
	public void close() throws IOException {
		this.pathResource.close();
	}
	
	@Override
	public Resource resolve(URI uri) throws IOException {
		FileResource resolvedResource = new FileResource(this.getURI().resolve(uri.normalize()), this.getMediaTypeService());
		resolvedResource.setExecutor(this.getExecutor());
		return resolvedResource;
	}
}

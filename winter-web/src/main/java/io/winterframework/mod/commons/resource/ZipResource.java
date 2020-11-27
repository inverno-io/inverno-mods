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
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public class ZipResource extends AbstractAsyncResource {

	public static final String SCHEME_ZIP = "zip";
	
	public static final String SCHEME_JAR = "jar";
	
	private URI uri;
	private URI zipUri;
	private URI zipFsUri;
	private String resourcePath;
	
	private FileSystem fileSystem;
	private Optional<PathResource> pathResource;
	
	private boolean closed;
	
	public ZipResource(URI uri) throws IOException {
		this(uri, (MediaTypeService)null);
	}
	
	protected ZipResource(URI uri, String scheme) throws IOException {
		this(uri, scheme, null);
	}
	
	protected ZipResource(URI uri, MediaTypeService mediaTypeService) throws IOException {
		this(uri, SCHEME_ZIP, mediaTypeService);
	}
	
	protected ZipResource(URI uri, String scheme, MediaTypeService mediaTypeService) throws IOException {
		super(mediaTypeService);
		if(!Objects.requireNonNull(uri).getScheme().equals(scheme)) {
			throw new IllegalArgumentException("Not a " + scheme + " uri");
		}
		this.uri = uri.normalize();
		String spec = this.uri.getSchemeSpecificPart();
		int resourcePathIndex = spec.indexOf("!/");
        if (resourcePathIndex == -1) {
        	throw new IllegalArgumentException("Missing resource path info: ...!/path/to/resource");
        }
        String zipSpec = spec.substring(0, resourcePathIndex);
        try {
			this.zipUri = new URI(zipSpec);
			this.zipFsUri = new URI(SCHEME_JAR, zipSpec, null);
			this.resourcePath = spec.substring(resourcePathIndex + 1);
		} 
        catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid " + scheme + " resource URI", e);
		}
	}
	
	@Override
	public void setExecutor(ExecutorService executor) {
		super.setExecutor(executor);
		if(this.pathResource != null) {
			this.pathResource.ifPresent(resource -> resource.setExecutor(this.getExecutor()));
		}
	}
	
	private Optional<PathResource> resolve() throws IOException {
		if(this.closed) {
			throw new ClosedResourceException();
		}
		if(this.pathResource == null) {
			try {
				this.fileSystem = FileSystems.newFileSystem(this.zipFsUri, Map.of("create", "true"));
				PathResource resolvedResource = new PathResource(this.fileSystem.getPath(this.resourcePath), this.getMediaTypeService());
				resolvedResource.setExecutor(this.getExecutor());
				this.pathResource = Optional.of(resolvedResource);
			} 
			catch (FileSystemException e) {
				return Optional.empty();
			}
		}
		return this.pathResource;
	}
	
	@Override
	public String getFilename() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().getFilename();
		}
		return null;
	}
	
	@Override
	public String getMediaType() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().getMediaType();
		}
		return null;
	}
	
	@Override
	public URI getURI() {
		return this.uri;
	}
	
	@Override
	public Boolean exists() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().exists();
		}
		return false;
	}

	@Override
	public FileTime lastModified() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().lastModified();
		}
		return null;
	}
	
	@Override
	public Long size() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().size();
		}
		return null;
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().openReadableByteChannel();
		}
		return Optional.empty();
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws IOException {
		try {
			if(createParents) {
				Files.createDirectories(Paths.get(this.zipUri).getParent());
			}
			Optional<PathResource> pr = this.resolve();
			if(pr.isPresent()) {
				return pr.get().openWritableByteChannel(append, createParents);
			}
			return Optional.empty();
		} 
		catch (FileSystemException e) {
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<Flux<ByteBuf>> read() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().read();
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<Flux<Integer>> write(Flux<ByteBuf> data, boolean append, boolean createParents) throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().write(data);
		}
		return Optional.empty();
	}

	@Override
	public boolean delete() throws IOException {
		Optional<PathResource> pr = this.resolve();
		if(pr.isPresent()) {
			return pr.get().delete();
		}
		return false;
	}
	
	@Override
	public void close() throws IOException {
		try {
			Optional<PathResource> pr = this.resolve();
			if(pr.isPresent()) {
				pr.get().close();
			}
		}
		finally {
			try {
				this.fileSystem.close();
			}
			finally {
				this.closed = true;
			}
		}
	}
	
	@Override
	public Resource resolve(URI uri) throws IOException {
		ZipResource resolvedResource = new ZipResource(this.uri.resolve(uri.normalize()), this.getMediaTypeService());
		resolvedResource.setExecutor(this.getExecutor());
		return resolvedResource;
	}
}

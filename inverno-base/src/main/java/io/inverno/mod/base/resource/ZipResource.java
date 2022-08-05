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
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URI of the
 * form <code>zip:file:/path/to/zip!/path/to/resource</code> and looks up data in a
 * zip file on the file system system.
 * </p>
 * 
 * <p>
 * A typical usage is:
 * </p>
 * 
 * <pre>{@code
 * ZipResource resource = new ZipResource(URI.create("zip:file:/path/to/zip!/path/to/resource"));
 * ...
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AsyncResource
 * @see JarResource
 */
public class ZipResource extends AbstractAsyncResource {

	/**
	 * The zip resource scheme
	 */
	public static final String SCHEME_ZIP = "zip";
	
	/**
	 * The jar resource scheme
	 */
	public static final String SCHEME_JAR = "jar";
	
	private URI uri;
	
	/**
	 * The URI of the ZIP file.
	 */
	protected URI zipUri;
	
	/**
	 * The URI of the ZIP file system.
	 */
	protected URI zipFsUri;
	
	/**
	 * The path to the resource in the ZIP file.
	 */
	protected Path resourcePath;
	
	private FileSystem fileSystem;
	private Optional<PathResource> pathResource;
	
	private boolean closed;
	
	/**
	 * <p>
	 * Creates a zip resource with the specified URI.
	 * </p>
	 * 
	 * @param uri the resource URI
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  zip resource
	 */
	public ZipResource(URI uri) throws IllegalArgumentException {
		this(uri, (MediaTypeService)null);
	}

	/**
	 * <p>
	 * Creates a zip resource with the specified URI and media type service.
	 * </p>
	 * 
	 * @param uri              the resource URI
	 * @param mediaTypeService a media type service
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  zip resource
	 */
	public ZipResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		this(uri, SCHEME_ZIP, mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a zip-like resource with the specified URI and scheme.
	 * </p>
	 * 
	 * @param uri    the resource URI
	 * @param scheme the visible resource scheme (ie. zip, jar...)
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  resource of the specified scheme
	 */
	protected ZipResource(URI uri, String scheme) throws IllegalArgumentException {
		this(uri, scheme, null);
	}
	
	/**
	 * <p>
	 * Creates a zip-like resource with the specified URI, scheme and media type
	 * service.
	 * </p>
	 * 
	 * @param uri              the resource URI
	 * @param scheme           the visible resource scheme (ie. zip, jar...)
	 * @param mediaTypeService a media type service
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  resource of the specified scheme
	 */
	protected ZipResource(URI uri, String scheme, MediaTypeService mediaTypeService) throws IllegalArgumentException {
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
			this.resourcePath = Paths.get(spec.substring(resourcePathIndex + 1)).normalize();
		} 
        catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid " + scheme + " resource URI", e);
		}
	}
	
	/**
	 * <p>
	 * Checks that the specified URI is a zip resource URI.
	 * </p>
	 * 
	 * @param uri the uri to check
	 * 
	 * @return the uri if it is a zip resource URI
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  zip resource
	 */
	public static URI checkUri(URI uri) throws IllegalArgumentException {
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_ZIP)) {
			throw new IllegalArgumentException("Not a " + SCHEME_ZIP + " uri");
		}
		return uri.normalize();
	}
	
	@Override
	public void setExecutor(ExecutorService executor) {
		super.setExecutor(executor);
		if(this.pathResource != null) {
			this.pathResource.ifPresent(resource -> resource.setExecutor(this.getExecutor()));
		}
	}
	
	private Optional<PathResource> resolve() {
		if(this.closed) {
			throw new ClosedResourceException();
		}
		if(this.pathResource == null) {
			try {
//				this.fileSystem = FileSystems.newFileSystem(this.zipFsUri, Map.of("create", "true"));
				this.fileSystem = ReferenceCountedFileSystems.getFileSystem(this.zipFsUri, Map.of("create", "true"));
				PathResource resolvedResource = new PathResource(this.fileSystem.getPath(this.resourcePath.toString()), this.getMediaTypeService());
				resolvedResource.setExecutor(this.getExecutor());
				this.pathResource = Optional.of(resolvedResource);
			} 
			catch (IOException e) {
				// TODO log debug
				return Optional.empty();
			}
		}
		return this.pathResource;
	}
	
	@Override
	public String getFilename() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().getFilename();
		}
		return null;
	}
	
	@Override
	public String getMediaType() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().getMediaType();
		}
		return null;
	}
	
	@Override
	public URI getURI() {
		return this.uri;
	}
	
	@Override
	public Optional<Boolean> exists() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().exists();
		}
		return Optional.of(false);
	}
	
	@Override
	public Optional<Boolean> isFile() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().isFile();
		}
		return Optional.of(false);
	}

	@Override
	public Optional<FileTime> lastModified() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().lastModified();
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<Long> size() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().size();
		}
		return Optional.empty();
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().openReadableByteChannel();
		}
		return Optional.empty();
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) {
		try {
			if(createParents) {
				Files.createDirectories(Paths.get(this.zipUri).getParent());
			}
			Optional<PathResource> r = this.resolve();
			if(r.isPresent()) {
				return r.get().openWritableByteChannel(append, createParents);
			}
			return Optional.empty();
		} 
		catch (IOException e) {
			// TODO log debug
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<Publisher<ByteBuf>> read() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().read();
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<Publisher<Integer>> write(Publisher<ByteBuf> data, boolean append, boolean createParents) {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().write(data);
		}
		return Optional.empty();
	}

	@Override
	public boolean delete() {
		Optional<PathResource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().delete();
		}
		return false;
	}
	
	@Override
	public void close() {
		try {
			if(this.pathResource != null && this.pathResource.isPresent()) {
				this.pathResource.get().close();
			}
		}
		finally {
			try {
				if(this.fileSystem != null) {
					this.fileSystem.close();
				}
			} 
			catch (IOException e) {
				throw new ResourceException(e);
			}
			finally {
				this.closed = true;
			}
		}
	}
	
	@Override
	public Resource resolve(Path path) {
		try {
			URI resolvedURI = new URI(ZipResource.SCHEME_ZIP, this.zipFsUri.getSchemeSpecificPart()+ "!" + this.resourcePath.resolve(path).normalize().toString(), null);
			ZipResource resolvedResource = new ZipResource(resolvedURI, this.getMediaTypeService());
			resolvedResource.setExecutor(this.getExecutor());
			return resolvedResource;
		} 
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid path", e);
		}
	}
}

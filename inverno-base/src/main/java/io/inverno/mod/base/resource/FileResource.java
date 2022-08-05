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
import java.io.File;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.reactivestreams.Publisher;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URI of the
 * form <code>file:/path/to/resource</code> and looks up data on the file
 * system.
 * </p>
 * 
 * <p>
 * A typical usage is:
 * </p>
 * 
 * <pre>{@code
 * FileResource resource = new FileResource(URI.create("file:/path/to/resource"));
 * ...
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AsyncResource
 */
public class FileResource extends AbstractAsyncResource {

	/**
	 * The file resource scheme
	 */
	public static final String SCHEME_FILE = "file";
	
	private PathResource pathResource;
	
	/**
	 * <p>
	 * Creates a file resource with the specified URI.
	 * </p>
	 * 
	 * @param uri the resource URI
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  file resource
	 */
	public FileResource(URI uri) throws IllegalArgumentException {
		this(uri, null);
	}
	
	/**
	 * <p>
	 * Creates a file resource from the specified file.
	 * </p>
	 * 
	 * @param file a file
	 */
	public FileResource(File file) {
		this(file, null);
	}
	
	/**
	 * <p>
	 * Creates a file resource from the specified path name.
	 * </p>
	 * 
	 * @param pathname a path to a file
	 */
	public FileResource(String pathname) {
		this(pathname, null);
	}
	
	/**
	 * <p>
	 * Creates a file resource from the specified path.
	 * </p>
	 * 
	 * @param path a path to a file
	 */
	public FileResource(Path path) {
		this(path, null);
	}
	
	/**
	 * <p>
	 * Creates a file resource with the specified URI and media type service.
	 * </p>
	 * 
	 * @param uri              the resource URI
	 * @param mediaTypeService a media type service
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  file resource
	 */
	public FileResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.pathResource = new PathResource(Paths.get(FileResource.checkUri(uri)), mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a file resource from the specified file with the specified media type
	 * service.
	 * </p>
	 * 
	 * @param file a file
	 * @param mediaTypeService a media type service
	 */
	public FileResource(File file, MediaTypeService mediaTypeService) {
		super(mediaTypeService);
		this.pathResource = new PathResource(Objects.requireNonNull(file.getAbsoluteFile()).toPath(), mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a file resource from the specified path name with the specified media type
	 * service.
	 * </p>
	 * 
	 * @param pathname         a path to a file
	 * @param mediaTypeService a media type service
	 */
	public FileResource(String pathname, MediaTypeService mediaTypeService) {
		this(new File(pathname), mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a file resource from the specified path with the specified media type
	 * service.
	 * </p>
	 * 
	 * @param path             a path to a file
	 * @param mediaTypeService a media type service
	 */
	public FileResource(Path path, MediaTypeService mediaTypeService) {
		super(mediaTypeService);
		this.pathResource = new PathResource(path, mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a file resource from the specified path resource with the specified media type service.
	 * </p>
	 * 
	 * @param pathResource     a path resource
	 * @param mediaTypeService a media type service
	 */
	private FileResource(PathResource pathResource, MediaTypeService mediaTypeService) {
		super(mediaTypeService);
		this.pathResource = pathResource;
	}
	
	/**
	 * <p>
	 * Checks that the specified URI is a file resource URI.
	 * </p>
	 * 
	 * @param uri the uri to check
	 * 
	 * @return the uri if it is a file resource URI
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  file resource
	 */
	public static URI checkUri(URI uri) throws IllegalArgumentException {
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_FILE)) {
			throw new IllegalArgumentException("Not a " + SCHEME_FILE + " uri");
		}
		return uri.normalize();
	}
	
	@Override
	public void setExecutor(ExecutorService executor) {
		this.pathResource.setExecutor(executor);
	}
	
	@Override
	public String getFilename() {
		return this.pathResource.getFilename();
	}
	
	@Override
	public String getMediaType() {
		return this.pathResource.getMediaType();
	}
	
	@Override
	public URI getURI() {
		return this.pathResource.getURI();
	}
	
	@Override
	public Optional<Boolean> exists() {
		return this.pathResource.exists();
	}
	
	@Override
	public Optional<Boolean> isFile() {
		return this.pathResource.isFile();
	}

	@Override
	public Optional<FileTime> lastModified() {
		return this.pathResource.lastModified();
	}
	
	@Override
	public Optional<Long> size() {
		return this.pathResource.size();
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() {
		return this.pathResource.openReadableByteChannel();
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) {
		return this.pathResource.openWritableByteChannel(append, createParents);
	}

	@Override
	public Optional<Publisher<ByteBuf>> read() {
		return this.pathResource.read();
	}
	
	@Override
	public Optional<Publisher<Integer>> write(Publisher<ByteBuf> data, boolean append, boolean createParents) {
		return this.pathResource.write(data, append, createParents);
	}
	
	@Override
	public boolean delete() {
		return this.pathResource.delete();
	}
	
	@Override
	public void close() {
		this.pathResource.close();
	}
	
	@Override
	public Resource resolve(Path path) throws IllegalArgumentException {
		FileResource resolvedResource = new FileResource((PathResource)this.pathResource.resolve(path), this.getMediaTypeService());
		resolvedResource.setExecutor(this.getExecutor());
		return resolvedResource;
	}
}

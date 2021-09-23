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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URI of the
 * form <code>classpath:/path/to/resource</code> and looks up data on the
 * classpath.
 * </p>
 * 
 * <p>
 * A typical usage is:
 * </p>
 * 
 * <blockquote><pre>
 * ClasspathResource resource = new ClasspathResource(URI.create("classpath:/path/to/resource"));
 * ...
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AsyncResource
 */
public class ClasspathResource extends AbstractAsyncResource {

	/**
	 * The classpath resource scheme
	 */
	public static final String SCHEME_CLASSPATH = "classpath";
	
	private Optional<Resource> resource;
	
	private URI uri;
	private String resourceName;
	
	private Class<?> clazz;
	private ClassLoader classLoader;
	
	/**
	 * <p>
	 * Creates a classpath resource with the specified URI.
	 * </p>
	 * 
	 * @param uri the resource URI
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  classpath resource
	 */
	public ClasspathResource(URI uri) throws IllegalArgumentException {
		this(uri, (MediaTypeService)null);
	}
	
	/**
	 * <p>
	 * Creates a classpath resource with the specified URI that looks up data from
	 * the specified class.
	 * </p>
	 * 
	 * @param uri   the resource URI
	 * @param clazz a class
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  classpath resource
	 * 
	 * @see Class#getResource(String)
	 */
	public ClasspathResource(URI uri, Class<?> clazz) throws IllegalArgumentException {
		this(uri, clazz, null);
	}

	/**
	 * <p>
	 * Creates a classpath resource with the specified URI that looks up data from
	 * the specified class loader.
	 * </p>
	 * 
	 * @param uri         the resource URI
	 * @param classLoader a class loader
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  classpath resource
	 * 
	 * @see ClassLoader#getResource(String)
	 */
	public ClasspathResource(URI uri, ClassLoader classLoader) throws IllegalArgumentException {
		this(uri, classLoader, null);
	}
	
	/**
	 * <p>
	 * Creates a classpath resource with the specified URI and media type service.
	 * </p>
	 * 
	 * @param uri              the resource URI
	 * @param mediaTypeService a media type service
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  classpath resource
	 */
	public ClasspathResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		this(uri, (ClassLoader)null, mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a classpath resource with the specified URI and media type service
	 * that looks up data from the specified class.
	 * </p>
	 * 
	 * @param uri              the resource URI
	 * @param clazz            a class
	 * @param mediaTypeService a media type service
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  classpath resource
	 */
	public ClasspathResource(URI uri, Class<?> clazz, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.uri = ClasspathResource.checkUri(uri);
		this.clazz = Objects.requireNonNull(clazz);
		
		this.resourceName = this.uri.getRawPath();
		if(this.resourceName == null || this.resourceName.isEmpty()) {
			throw new IllegalArgumentException("No resource name specified in uri: " + SCHEME_CLASSPATH + ":/[RESOURCE_NAME]");
		}
		if(!this.resourceName.startsWith("/")) {
			this.resourceName = "/" + this.resourceName;
		}
	}
	
	/**
	 * <p>
	 * Creates a classpath resource with the specified URI and media type service
	 * that looks up data from the specified class loader.
	 * </p>
	 * 
	 * @param uri              the resource URI
	 * @param classLoader      a class loader
	 * @param mediaTypeService a media type service
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  classpath resource
	 */
	public ClasspathResource(URI uri, ClassLoader classLoader, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.uri = ClasspathResource.checkUri(uri);
		
		this.resourceName = this.uri.getRawPath();
		if(this.resourceName == null || this.resourceName.isEmpty()) {
			throw new IllegalArgumentException("No resource name specified in uri: " + SCHEME_CLASSPATH + ":/[RESOURCE_NAME]");
		}
		if(this.resourceName.startsWith("/")) {
			this.resourceName = this.resourceName.substring(1);
		}
		
		if(classLoader == null) {
			try {
				this.classLoader = Thread.currentThread().getContextClassLoader();
			}
			catch (Throwable ex) {
				this.classLoader = ClasspathResource.class.getClassLoader();
				if (this.classLoader == null) {
					this.classLoader = ClassLoader.getSystemClassLoader();
				}
			}
		}
		else {
			this.classLoader = classLoader;
		}
	}
	
	/**
	 * <p>
	 * Checks that the specified URI is a classpath resource URI.
	 * </p>
	 * 
	 * @param uri the uri to check
	 * 
	 * @return the uri if it is a classpath resource URI
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  classpath resource
	 */
	public static URI checkUri(URI uri) throws IllegalArgumentException {
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_CLASSPATH) || uri.getAuthority() != null) {
			throw new IllegalArgumentException("Not a " + SCHEME_CLASSPATH + " uri");
		}
		if(uri.isOpaque()) {
			throw new IllegalArgumentException(SCHEME_CLASSPATH + "uri can't be opaque");
		}
		return uri.normalize();
	}
	
	@Override
	public void setExecutor(ExecutorService executor) {
		super.setExecutor(executor);
		if(this.resource != null) {
			this.resource.ifPresent(resource -> {
				if(resource instanceof AsyncResource) {
					((AsyncResource) resource).setExecutor(this.getExecutor());
				}
			});
		}
	}
	
	private Optional<Resource> resolve() {
		if(this.resource == null) {
			URL url;
			if(this.clazz != null) {
				url = this.clazz.getResource(this.resourceName);
			}
			else {
				url = this.classLoader.getResource(this.resourceName);
			}
			if(url != null) {
				URI uri;
				try {
					uri = url.toURI();
				} 
				catch (URISyntaxException e) {
					throw new ResourceException("Error resolving classpath resource: " + this.uri, e);
				}
				String scheme = uri.getScheme();
				Resource resolvedResource;
				switch(scheme) {
					case FileResource.SCHEME_FILE:
						resolvedResource = new FileResource(uri, this.getMediaTypeService());
						break;
					case JarResource.SCHEME_JAR:
						resolvedResource = new JarResource(uri, this.getMediaTypeService());
						break;
					case ZipResource.SCHEME_ZIP:
						resolvedResource = new ZipResource(uri, this.getMediaTypeService());
						break;
					case NativeResource.SCHEME_RESOURCE:
						resolvedResource = new NativeResource(uri, this.getMediaTypeService());
						break;
					default:
						throw new ResourceException("Unsupported resource scheme: " + scheme);
				}
				if(resolvedResource instanceof AsyncResource) {
					((AsyncResource) resolvedResource).setExecutor(this.getExecutor());
				}
				this.resource = Optional.of(resolvedResource);
			}
			else {
				this.resource = Optional.empty();
			}
		}
		return this.resource;
	}
	
	@Override
	public String getFilename() {
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().getFilename();
		}
		return null;
	}

	@Override
	public String getMediaType() {
		Optional<Resource> r = this.resolve();
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
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().exists();
		}
		return Optional.of(false);
	}
	
	@Override
	public Optional<Boolean> isFile() {
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().isFile();
		}
		return Optional.of(false);
	}
	
	@Override
	public Optional<Long> size() {
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().size();
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<FileTime> lastModified() {
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().lastModified();
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() {
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().openReadableByteChannel();
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) {
		return Optional.empty();
	}
	
	@Override
	public Optional<Publisher<ByteBuf>> read() {
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			return r.get().read();
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<Publisher<Integer>> write(Publisher<ByteBuf> data, boolean append, boolean createParents) {
		return Optional.empty();
	}
	
	@Override
	public boolean delete() {
		throw new UnsupportedOperationException("Can't delete a classpath resource");
	}

	@Override
	public void close() {
		Optional<Resource> r = this.resolve();
		if(r.isPresent()) {
			try {
				r.get().close();
			} 
			catch (Exception e) {
				throw new ResourceException(e);
			}
		}
	}
	
	@Override
	public Resource resolve(Path path) throws IllegalArgumentException {
		try {
			URI resolvedUri = new URI(ClasspathResource.SCHEME_CLASSPATH, Paths.get(this.uri.getRawPath()).resolve(path).toString(), null);
			ClasspathResource resolvedResource;
			if(this.clazz != null) {
				resolvedResource = new ClasspathResource(resolvedUri, this.clazz, this.getMediaTypeService());
			}
			else {
				resolvedResource = new ClasspathResource(resolvedUri, this.classLoader, this.getMediaTypeService());
			}
			resolvedResource.setExecutor(this.getExecutor());
			return resolvedResource;
		} 
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid path", e);
		}
	}
}

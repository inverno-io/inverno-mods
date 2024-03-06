/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.base.ApplicationRuntime;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URI of the form {@code resource:path/to/resource} and looks up data in a native image.
 * </p>
 *
 * <p>
 * A typical usage is:
 * </p>
 *
 * <pre>{@code
 * NativeResource resource = new NativeResource(URI.create("resource:path/to/resource"));
 * ...
 * }</pre>
 *
 * When running a native image, this is actually equivalent to:
 *
 * <pre>{@code
 * ClasspathResource resource = new ClasspathResource(URI.create("resource:/path/to/resource"));
 * ...
 * }</pre>
 * 
 * <p>
 * Note that native resources can only be created when running a native image (see {@link RuntimeEnvironment#IMAGE_NATIVE}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see AbstractAsyncResource
 * @see ClasspathResource
 */
public class NativeResource extends AbstractAsyncResource {

	/**
	 * The module resource scheme
	 */
	public static final String SCHEME_RESOURCE = "resource";
	
	private final URI uri;
	private String resourceName;
	
	private Class<?> clazz;
	private ClassLoader classLoader;
	
	private Optional<URL> resourceURL;
	
	private Optional<Boolean> exists;
	
	/**
	 * <p>
	 * Creates a native resource with the specified URI.
	 * </p>
	 *
	 * @param uri the resource URI
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a native resource
	 * @throws IllegalStateException if runtime environment is not native
	 */
	public NativeResource(URI uri) throws IllegalArgumentException, IllegalStateException {
		this(uri, (MediaTypeService)null);
	}
	
	/**
	 * <p>
	 * Creates a native resource with the specified URI that looks up data from the module layer that contains the module of which the specified class is a member.
	 * </p>
	 *
	 * @param uri the resource URI
	 * @param clazz a class
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a native resource
	 * @throws IllegalStateException if runtime environment is not native
	 *
	 * @see Class#getModule()
	 */
	public NativeResource(URI uri, Class<?> clazz) throws IllegalArgumentException, IllegalStateException {
		this(uri, clazz, null);
	}
	
	/**
	 * <p>
	 * Creates a native resource with the specified URI that looks up data from the specified class loader.
	 * </p>
	 *
	 * @param uri the resource URI
	 * @param classLoader a class loader
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a native resource
	 * @throws IllegalStateException if runtime environment is not native
	 *
	 * @see ClassLoader#getResource(String)
	 */
	public NativeResource(URI uri, ClassLoader classLoader) throws IllegalArgumentException, IllegalStateException {
		this(uri, classLoader, null);
	}
	
	/**
	 * <p>
	 * Creates a native resource with the specified URI and media type service.
	 * </p>
	 *
	 * @param uri the resource URI
	 * @param mediaTypeService the media type service
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a native resource
	 * @throws IllegalStateException if runtime environment is not native
	 */
	public NativeResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException, IllegalStateException {
		this(uri, (ClassLoader)null, mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a native resource with the specified URI and media type service that looks up data from the specified class.
	 * </p>
	 *
	 * @param uri the resource URI
	 * @param clazz a class
	 * @param mediaTypeService a media type service
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a native resource
	 * @throws IllegalStateException if runtime environment is not native
	 */
	public NativeResource(URI uri, Class<?> clazz, MediaTypeService mediaTypeService) throws IllegalArgumentException, IllegalStateException {
		super(mediaTypeService);
		this.uri = NativeResource.checkUri(uri);
		this.clazz = Objects.requireNonNull(clazz);
		
		this.resourceName = this.uri.isOpaque() ? this.uri.getRawSchemeSpecificPart() : this.uri.getRawPath();
		if(this.resourceName == null || this.resourceName.isEmpty()) {
			throw new IllegalArgumentException("No resource name specified in uri: " + SCHEME_RESOURCE + ":[RESOURCE_NAME]");
		}
		if(!this.resourceName.startsWith("/")) {
			this.resourceName = "/" + this.resourceName;
		}
	}
	
	/**
	 * <p>
	 * Creates a native resource with the specified URI and media type service that looks up data from the specified class loader.
	 * </p>
	 *
	 * @param uri the resource URI
	 * @param classLoader a class loader
	 * @param mediaTypeService a media type service
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a native resource
	 * @throws IllegalStateException if runtime environment is not native
	 */
	public NativeResource(URI uri, ClassLoader classLoader, MediaTypeService mediaTypeService) throws IllegalArgumentException, IllegalStateException {
		super(mediaTypeService);
		this.uri = NativeResource.checkUri(uri);
		
		this.resourceName = this.uri.isOpaque() ? this.uri.getRawSchemeSpecificPart() : this.uri.getRawPath();
		if(this.resourceName == null || this.resourceName.isEmpty()) {
			throw new IllegalArgumentException("No resource name specified in uri: " + SCHEME_RESOURCE + ":[RESOURCE_NAME]");
		}
		if(this.resourceName.startsWith("/")) {
			this.resourceName = this.resourceName.substring(1);
		}
		
		if(classLoader == null) {
			try {
				this.classLoader = Thread.currentThread().getContextClassLoader();
			}
			catch (Throwable ex) {
				this.classLoader = NativeResource.class.getClassLoader();
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
	 * Checks that the specified URI is a module resource URI.
	 * </p>
	 *
	 * @param uri the uri to check
	 *
	 * @return the uri if it is a module resource URI
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 * @throws IllegalStateException if runtime environment is not native
	 */
	public static URI checkUri(URI uri) throws IllegalArgumentException, IllegalStateException {
		if(ApplicationRuntime.getApplicationRuntime() != ApplicationRuntime.IMAGE_NATIVE) {
			throw new IllegalStateException("A native resource can only be created when running a native image");
		}
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_RESOURCE)) {
			throw new IllegalArgumentException("Not a " + SCHEME_RESOURCE + " uri");
		}
		return uri.normalize();
	}
	
	private Optional<URL> resolve() {
		if(this.resourceURL == null) {
			URL url;
			if(this.clazz != null) {
				url = this.clazz.getResource(this.resourceName);
			}
			else {
				url = this.classLoader.getResource(this.resourceName);
			}
			this.resourceURL = Optional.ofNullable(url);
		}
		return this.resourceURL;
	}
	
	@Override
	public String getFilename() throws ResourceException {
		int lastSlashIndex = this.resourceName.lastIndexOf("/");
		if(lastSlashIndex != -1) {
			return this.resourceName.substring(lastSlashIndex + 1);
		}
		else {
			return null;
		}
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public Optional<Boolean> isFile() throws ResourceException {
		return Optional.empty();
	}

	@Override
	public Optional<Boolean> exists() throws ResourceException {
		if(this.exists == null) {
			this.exists = this.resolve().map(url -> true);
		}
		return this.exists;
	}

	@Override
	public Optional<FileTime> lastModified() throws ResourceException {
		return Optional.empty();
	}

	@Override
	public Optional<Long> size() throws ResourceException {
		return Optional.empty();
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() throws ResourceException {
		if(this.clazz != null) {
			return Optional.ofNullable(this.clazz.getResourceAsStream(this.resourceName)).map(Channels::newChannel);
			
		}
		else {
			return Optional.ofNullable(this.classLoader.getResourceAsStream(this.resourceName)).map(Channels::newChannel);
		}
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws ResourceException {
		return Optional.empty();
	}

	@Override
	public boolean delete() throws ResourceException {
		return false;
	}

	@Override
	public NativeResource resolve(Path path) throws ResourceException {
		try {
			URI resolvedUri = new URI(NativeResource.SCHEME_RESOURCE, pathToSanitizedString(Path.of(this.uri.getRawSchemeSpecificPart()).resolve(path)), null);
			NativeResource resolvedResource = new NativeResource(resolvedUri, this.getMediaTypeService());
			resolvedResource.setExecutor(this.getExecutor());
			return resolvedResource;
		} 
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid path", e);
		}
	}

	@Override
	public void close() {

	}
}

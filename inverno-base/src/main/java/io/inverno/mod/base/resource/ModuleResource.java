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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URI of the form {@code module://[MODULE_NAME]/path/to/resource} or {@code module:/path/to/resource} and looks up data from a module.
 * </p>
 *
 * <p>
 * When no module name is specified in the URI, the {@code jdk.module.main} module is considered.
 * </p>
 *
 * <p>
 * When the application runs without modules, the unnamed module is used whatever the module specified in the URI.
 * </p>
 *
 * <p>
 * A typical usage is:
 * </p>
 * 
 * <pre>{@code
 * ModuleResource resource = new ModuleResource(URI.create("module://module/path/to/resource"));
 * ...
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractAsyncResource
 */
public class ModuleResource extends AbstractAsyncResource {

	/**
	 * The module resource scheme
	 */
	public static final String SCHEME_MODULE = "module";
	
	private final URI uri;
	private final ModuleLayer moduleLayer;
	
	private Optional<Module> module;
	private String moduleName;
	private String resourceName;
	
	private Optional<Boolean> exists;
	
	private ResourceException resolutionError;
	
	/**
	 * <p>
	 * Creates a module resource with the specified URI.
	 * </p>
	 *
	 * @param uri the resource URI
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 */
	public ModuleResource(URI uri) throws IllegalArgumentException {
		this(uri, (ModuleLayer)null, null);
	}
	
	/**
	 * <p>
	 * Creates a module resource with the specified URI that looks up data from the module layer that contains the module of which the specified class is a member.
	 * </p>
	 *
	 * @param uri   the resource URI
	 * @param clazz a class
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 *
	 * @see Class#getModule()
	 */
	public ModuleResource(URI uri, Class<?> clazz) throws IllegalArgumentException {
		this(uri, clazz.getModule().getLayer(), null);
	}
	
	/**
	 * <p>
	 * Creates a module resource with the specified URI and media type service that looks up data from the module layer that contains the module of which the specified class is a member.
	 * </p>
	 *
	 * @param uri              the resource URI
	 * @param clazz            a class
	 * @param mediaTypeService a media type service
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 */
	public ModuleResource(URI uri, Class<?> clazz, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		this(uri, clazz.getModule().getLayer(), mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a module resource with the specified URI that looks up data from the specified module layer.
	 * </p>
	 *
	 * @param uri         the resource URI
	 * @param moduleLayer a module layer
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 */
	public ModuleResource(URI uri, ModuleLayer moduleLayer) throws IllegalArgumentException {
		this(uri, moduleLayer, null);
	}
	
	/**
	 * <p>
	 * Creates a module resource with the specified URI and media type service.
	 * </p>
	 *
	 * @param uri              the resource URI
	 * @param mediaTypeService the media type service
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 */
	public ModuleResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		this(uri, (ModuleLayer)null, mediaTypeService);
	}
	
	/**
	 * <p>
	 * Creates a module resource with the specified URI and media type service that looks up data from the specified module layer.
	 * </p>
	 *
	 * @param uri              the resource URI
	 * @param moduleLayer      a module layer
	 * @param mediaTypeService the media type service
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 */
	public ModuleResource(URI uri, ModuleLayer moduleLayer, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.uri = ModuleResource.checkUri(uri);
		
		this.moduleName = this.uri.getAuthority();
		if(this.moduleName == null || this.moduleName.isEmpty()) {
			this.moduleName = System.getProperty("jdk.module.main");
		}
		
		this.resourceName = this.uri.getRawPath();
		if(this.resourceName == null || this.resourceName.isEmpty()) {
			throw new IllegalArgumentException("No resource name specified in uri: " + SCHEME_MODULE + "://[MODULE_NAME]/[RESOURCE_NAME]");
		}
		if(this.resourceName.startsWith("/")) {
			this.resourceName = this.resourceName.substring(1);
		}
		
		if(moduleLayer == null) {
			this.moduleLayer = this.getClass().getModule().getLayer();
		}
		else {
			this.moduleLayer = moduleLayer;
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
	 *
	 * @throws IllegalArgumentException if the specified URI does not designate a module resource
	 */
	public static URI checkUri(URI uri) throws IllegalArgumentException {
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_MODULE)) {
			throw new IllegalArgumentException("Not a " + SCHEME_MODULE + " uri");
		}
		return uri.normalize();
	}
	
	private Optional<Module> resolve() throws ResourceException {
		if(this.resolutionError != null) {
			throw this.resolutionError;
		}
		if(this.module == null) {
			try {
				if(this.moduleLayer != null && this.moduleName != null) {
					this.module = this.moduleLayer.modules().stream().filter(module -> module.getName().equals(this.moduleName)).findFirst();
				}
				else {
					// unnamed module
					ClassLoader classLoader = null;
					try {
						classLoader = Thread.currentThread().getContextClassLoader();
					}
					catch (Throwable ex) {
						classLoader = ClasspathResource.class.getClassLoader();
						if (classLoader == null) {
							classLoader = ClassLoader.getSystemClassLoader();
						}
					}
					this.module = Optional.of(classLoader.getUnnamedModule());
				}
			}
			catch(Throwable t) {
				this.resolutionError = new ResourceException(t);
				throw this.resolutionError;
			}
		}
		return this.module;
	}
	
	@Override
	public URI getURI() {
		return this.uri;
	}
	
	@Override
	public String getFilename() throws ResourceException {
		int lastSlashIndex = this.resourceName.lastIndexOf("/");
		if(lastSlashIndex != -1) {
			return this.resourceName.substring(lastSlashIndex + 1);
		}
		else {
			return this.resourceName;
		}
	}

	@Override
	public Optional<Boolean> isFile() throws ResourceException {
		return Optional.empty();
	}

	@Override
	public Optional<Boolean> exists() throws ResourceException {
		if(this.resolutionError != null) {
			throw this.resolutionError;
		}
		if(this.exists == null) {
			this.exists = this.resolve().map(m -> {
				try(InputStream resourceStream = m.getResourceAsStream(this.resourceName)) {
					return resourceStream != null;
				}
				catch (IOException e) {
					throw new ResourceException(e);
				}
			});
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
		return this.resolve()
			.map(module -> {
				try {
					return Channels.newChannel(module.getResourceAsStream(this.resourceName));
				}
				catch (IOException e) {
					throw new ResourceException(e);
				}
			});
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
	public ModuleResource resolve(Path path) throws ResourceException {
		try {
			URI resolvedUri = new URI(ModuleResource.SCHEME_MODULE, this.uri.getAuthority(), pathToSanitizedString(Path.of(this.uri.getRawPath()).resolve(path)), null, null);
			ModuleResource resolvedResource = new ModuleResource(resolvedUri, this.getMediaTypeService());
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

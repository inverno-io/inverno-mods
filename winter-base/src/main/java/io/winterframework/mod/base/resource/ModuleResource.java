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
package io.winterframework.mod.base.resource;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * @author jkuhn
 *
 */
public class ModuleResource extends AbstractAsyncResource {

	public static final String SCHEME_MODULE = "module";
	
	private final URI uri;
	private final ModuleLayer moduleLayer;
	
	private Optional<Module> module;
	private String moduleName;
	private String resourceName;
	
	public ModuleResource(URI uri) {
		this(uri, (ModuleLayer)null, null);
	}
	
	public ModuleResource(URI uri, Class<?> clazz) {
		this(uri, clazz.getModule().getLayer(), null);
	}
	
	public ModuleResource(URI uri, Class<?> clazz, MediaTypeService mediaTypeService) {
		this(uri, clazz.getModule().getLayer(), mediaTypeService);
	}
	
	public ModuleResource(URI uri, ModuleLayer moduleLayer) {
		this(uri, moduleLayer, null);
	}
	
	public ModuleResource(URI uri, MediaTypeService mediaTypeService) {
		this(uri, (ModuleLayer)null, mediaTypeService);
	}
	
	public ModuleResource(URI uri, ModuleLayer moduleLayer, MediaTypeService mediaTypeService) {
		super(mediaTypeService);
		this.uri = ModuleResource.checkUri(uri);
		
		this.moduleName = this.uri.getAuthority();
		if(this.moduleName == null || this.moduleName.isEmpty()) {
			throw new IllegalArgumentException("No module specified in uri: " + SCHEME_MODULE + "://<MODULE_NAME>/<RESOURCE_NAME>");
		}
		
		this.resourceName = this.uri.getPath();
		if(this.resourceName == null || this.resourceName.isEmpty()) {
			throw new IllegalArgumentException("No resource name specified in uri: " + SCHEME_MODULE + "://<MODULE_NAME>/<RESOURCE_NAME>");
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
	
	public static URI checkUri(URI uri) throws IllegalArgumentException {
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_MODULE)) {
			throw new IllegalArgumentException("Not a " + SCHEME_MODULE + " uri");
		}
		return uri.normalize();
	}
	
	private Optional<Module> resolve() {
		if(this.module == null) {
			this.module = this.moduleLayer.modules().stream().filter(module -> module.getName().equals(this.moduleName)).findFirst();
		}
		return this.module;
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
	public boolean isFile() throws ResourceException {
		return false;
	}

	@Override
	public Boolean exists() throws ResourceException {
		return this.resolve().map(module -> {
			try {
				return module.getResourceAsStream(this.resourceName) != null;
			}
			catch (IOException e) {
				return false;
			}
		}).orElse(false);
	}

	@Override
	public FileTime lastModified() throws ResourceException {
		return null;
	}

	@Override
	public Long size() throws ResourceException {
		return null;
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() throws ResourceException {
		return this.resolve()
			.map(module -> {
				try {
					return module.getResourceAsStream(this.resourceName);
				}
				catch (IOException e) {
					return null;
				}
			})
			.map(Channels::newChannel);
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
	public ModuleResource resolve(URI uri) throws ResourceException {
		return new ModuleResource(this.uri.resolve(uri.normalize()), this.getMediaTypeService());
	}

	@Override
	public void close() {

	}
}

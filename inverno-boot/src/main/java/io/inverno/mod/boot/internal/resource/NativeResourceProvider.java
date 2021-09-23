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
package io.inverno.mod.boot.internal.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Stream;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.resource.AbstractResourceProvider;
import io.inverno.mod.base.resource.AsyncResourceProvider;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.JarResource;
import io.inverno.mod.base.resource.MediaTypeService;
import io.inverno.mod.base.resource.NativeResource;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceException;
import io.inverno.mod.base.resource.ResourceProvider;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.base.resource.ZipResource;

/**
 * <p>
 * {@link ResourceProvider} implementation used to resolve resources
 * in a native image (ie. {@code resource:path/to/resource}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see NativeResource
 * @see AsyncResourceProvider
 * @see ResourceService
 */
@Bean(visibility = Visibility.PRIVATE)
public class NativeResourceProvider extends AbstractResourceProvider<NativeResource> implements AsyncResourceProvider<NativeResource> {

	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public NativeResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		return new NativeResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<Resource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		// we can't support path pattern here, if someone wants to list resources in such a way he should rely on JarResouce
		uri = NativeResource.checkUri(uri);
		String path = uri.isOpaque() ? uri.getRawSchemeSpecificPart() : uri.getRawPath();
		if(path == null) {
			return Stream.of();
		}
		if(path.startsWith("/")) {
			path = path.substring(1);
		}
		
		ClassLoader classLoader;
		try {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			classLoader = NativeResource.class.getClassLoader();
			if (classLoader == null) {
				classLoader = ClassLoader.getSystemClassLoader();
			}
		}
		return classLoader.resources(path).map(this::getResource);
	}
	
	private Resource getResource(URL url) {
		URI uri;
		try {
			uri = url.toURI();
		} 
		catch (URISyntaxException e) {
			throw new ResourceException("Error resolving classpath resource: " + url, e);
		}
		String scheme = uri.getScheme();
		switch(scheme) {
			case FileResource.SCHEME_FILE:
				return new FileResource(uri, this.mediaTypeService);
			case JarResource.SCHEME_JAR:
				return new JarResource(uri, this.mediaTypeService);
			case ZipResource.SCHEME_ZIP:
				return new ZipResource(uri, this.mediaTypeService);
			case NativeResource.SCHEME_RESOURCE:
				return new NativeResource(uri, this.mediaTypeService);
			default:
				throw new ResourceException("Unsupported resource scheme: " + scheme);
		}
	}
	
	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of(NativeResource.SCHEME_RESOURCE);
	}
}

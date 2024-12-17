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
import java.util.Set;
import java.util.stream.Stream;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.resource.AbstractResourceProvider;
import io.inverno.mod.base.resource.AsyncResourceProvider;
import io.inverno.mod.base.resource.MediaTypeService;
import io.inverno.mod.base.resource.NativeResource;
import io.inverno.mod.base.resource.ResourceException;
import io.inverno.mod.base.resource.ResourceProvider;
import io.inverno.mod.base.resource.ResourceService;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.stream.Collectors;

/**
 * <p>
 * {@link ResourceProvider} implementation used to resolve resources in a native image (ie. {@code resource:path/to/resource}).
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

	private static final URI ROOT_RESOURCE_URI = URI.create("resource:/");
	
	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public NativeResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, IllegalStateException, ResourceException {
		return new NativeResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<NativeResource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, IllegalStateException, ResourceException {
		// we can't support path pattern here, if someone wants to list resources in such a way he should rely on JarResouce
		URI resourceURI = NativeResource.checkUri(uri);
		String pathPattern = resourceURI.getPath();
		// We won't create the resource file system since we are using ReferenceCountedFileSystems to get the instance
		try(FileSystem fs = this.getFileSystem(ROOT_RESOURCE_URI)) {
			// We have to collect here because otherwise the file system is closed before the execution of the pattern resolver
        	return PathPatternResolver.resolve(pathPattern, fs.getPath("/"), p -> new NativeResource(p.toUri(), this.mediaTypeService)).collect(Collectors.toList()).stream();
		}
		catch(IOException e) {
			throw new ResourceException("Error resolving resources from pattern: " + pathPattern, e);
		}
	}
	
	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of(NativeResource.SCHEME_RESOURCE);
	}
}

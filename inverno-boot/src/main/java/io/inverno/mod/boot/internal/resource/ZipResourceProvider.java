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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.resource.AbstractResourceProvider;
import io.inverno.mod.base.resource.AsyncResourceProvider;
import io.inverno.mod.base.resource.JarResource;
import io.inverno.mod.base.resource.MediaTypeService;
import io.inverno.mod.base.resource.ResourceException;
import io.inverno.mod.base.resource.ResourceProvider;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.base.resource.ZipResource;

/**
 * <p>
 * {@link ResourceProvider} implementation used to resolve resources in a zip file (ie. {@code zip:/path/to/zip!/path/to/resource}).
 * </p>
 *
 * <p>
 * This implementation supports path patterns and can then resolve multiple resources matching a given URI pattern.
 * </p>
 *
 * <pre>{@code
 * ZipResourceProvider provider = new ZipResourceProvider();
 *
 * // Returns: /path/test1/a, /path/test1/a/b, /path/test2/c...
 * Stream<ZipResource> resources = provider.getResources(URI.create("zip:/path/to/zip!/path/test?/{@literal **}/*");
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see JarResource
 * @see AsyncResourceProvider
 * @see ResourceService
 * @see PathPatternResolver
 */
@Bean(visibility = Visibility.PRIVATE)
public class ZipResourceProvider extends AbstractResourceProvider<ZipResource> implements AsyncResourceProvider<ZipResource> {

	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public ZipResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		return new ZipResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<ZipResource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		final URI zipFsURI;
		final String pathPattern;
		
		uri = ZipResource.checkUri(uri);
		String spec = uri.getSchemeSpecificPart();
		int resourcePathIndex = spec.indexOf("!/");
        if (resourcePathIndex == -1) {
        	throw new IllegalArgumentException("Missing resource path info: ...!/path/to/resource");
        }
        try {
        	zipFsURI = new URI(ZipResource.SCHEME_JAR, spec.substring(0, resourcePathIndex), null);
        	pathPattern = spec.substring(resourcePathIndex + 1);
		} 
        catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid jar resource URI", e);
		}
		
        try(FileSystem fs = this.getFileSystem(zipFsURI)) {
        	// We have to collect here because otherwise the file system is closed before the execution of the pattern resolver
        	return PathPatternResolver.resolve(pathPattern, fs.getPath("/"), p -> new ZipResource(p.toUri(), this.mediaTypeService)).collect(Collectors.toList()).stream();
        } 
        catch (IOException e) {
        	throw new ResourceException("Error resolving paths from pattern: " + spec, e);
		}
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of(JarResource.SCHEME_ZIP);
	}
}

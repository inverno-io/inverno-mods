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
package io.winterframework.mod.boot.internal.resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.base.resource.AbstractResourceProvider;
import io.winterframework.mod.base.resource.AsyncResourceProvider;
import io.winterframework.mod.base.resource.JarResource;
import io.winterframework.mod.base.resource.MediaTypeService;
import io.winterframework.mod.base.resource.ResourceException;
import io.winterframework.mod.base.resource.ResourceProvider;
import io.winterframework.mod.base.resource.ResourceService;

/**
 * <p>
 * {@link ResourceProvider} implementation used to resolve resources in a
 * jar file (ie. {@code jar:/path/to/jar!/path/to/resource}).
 * </p>
 * 
 * <p>
 * This implementation supports path patterns and can then resolve multiple
 * resources matching a given URI pattern.
 * </p>
 * 
 * <blockquote>
 * 
 * <pre>
 * JarResourceProvider provider = new JarResourceProvider();
 * 
 * // Returns: /path/test1/a, /path/test1/a/b, /path/test2/c...
 * Stream{@literal<JarResource>} resources = provider.getResources(URI.create("jar:/path/to/jar!/path/test?/{@literal **}/*");
 * </pre>
 * 
 * </blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see JarResource
 * @see AsyncResourceProvider
 * @see ResourceService
 * @see PathPatternResolver
 */
@Bean(visibility = Visibility.PRIVATE)
public class JarResourceProvider extends AbstractResourceProvider<JarResource> implements AsyncResourceProvider<JarResource> {

	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public JarResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		return new JarResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<JarResource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		final URI jarFsURI;
		final String pathPattern;
		
		uri = JarResource.checkUri(uri);
		String spec = uri.getSchemeSpecificPart();
		int resourcePathIndex = spec.indexOf("!/");
        if (resourcePathIndex == -1) {
        	throw new IllegalArgumentException("Missing resource path info: ...!/path/to/resource");
        }
        String jarSpec = spec.substring(0, resourcePathIndex);
        try {
        	jarFsURI = new URI(JarResource.SCHEME_JAR, jarSpec, null);
        	pathPattern = spec.substring(resourcePathIndex + 1);
		} 
        catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid jar resource URI", e);
		}
		
        try(FileSystem fs = this.getFileSystem(jarFsURI)) {
        	// We have to collect here because otherwise the file system is closed before the execution of the pattern resolver
        	return PathPatternResolver.resolve(fs.getPath(pathPattern), fs.getPath("/"), p -> new JarResource(p.toUri(), this.mediaTypeService)).collect(Collectors.toList()).stream();
        } 
        catch (IOException e) {
        	throw new ResourceException("Error resolving resources from pattern: " + spec, e);
		}
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of(JarResource.SCHEME_JAR);
	}

	
}

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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.resource.AbstractResourceProvider;
import io.inverno.mod.base.resource.AsyncResourceProvider;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.MediaTypeService;
import io.inverno.mod.base.resource.ResourceException;
import io.inverno.mod.base.resource.ResourceProvider;
import io.inverno.mod.base.resource.ResourceService;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * {@link ResourceProvider} implementation used to resolve resources on the
 * file system (ie. {@code file:/path/to/resource}).
 * </p>
 * 
 * <p>
 * This implementation supports path patterns and can then resolve multiple
 * resources matching a given URI pattern.
 * </p>
 * 
 * <pre>{@code
 * FileResourceProvider provider = new FileResourceProvider();
 * 
 * // Returns: /path/test1/a, /path/test1/a/b, /path/test2/c...
 * Stream<FileResource> resources = provider.getResources(URI.create("file:/path/test?/{@literal **}/*");
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see FileResource
 * @see AsyncResourceProvider
 * @see ResourceService
 * @see PathPatternResolver
 */
@Bean(visibility = Visibility.PRIVATE)
public class FileResourceProvider extends AbstractResourceProvider<FileResource> implements AsyncResourceProvider<FileResource> {

	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public FileResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		return new FileResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<FileResource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		Path pathPattern = Path.of(FileResource.checkUri(uri));
		return PathPatternResolver.resolve(pathPattern, path -> new FileResource(path.toUri(), this.mediaTypeService));
	}
	
	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of(FileResource.SCHEME_FILE);
	}
}

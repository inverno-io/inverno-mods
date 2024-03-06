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
import io.inverno.mod.base.resource.ResourceException;
import io.inverno.mod.base.resource.ResourceProvider;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.base.resource.URLResource;

/**
 * <p>
 * {@link ResourceProvider} implementation used to resolve resources at given URL (eg. {@code http://...}, {@code https://...}, {@code ftp://...}).
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see URLResource
 * @see AsyncResourceProvider
 * @see ResourceService
 */
@Bean(visibility = Visibility.PRIVATE)
public class URLResourceProvider extends AbstractResourceProvider<URLResource> {

	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public URLResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		return new URLResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<URLResource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		// we don't have the capability to do more
		return Stream.of(this.getResource(uri));
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of("http", "https", "ftp");
	}
}

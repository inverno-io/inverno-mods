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

import java.net.URI;
import java.util.Set;
import java.util.stream.Stream;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.base.resource.AbstractResourceProvider;
import io.winterframework.mod.base.resource.MediaTypeService;
import io.winterframework.mod.base.resource.ResourceException;
import io.winterframework.mod.base.resource.UrlResource;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class UrlResourceProvider extends AbstractResourceProvider<UrlResource> {

	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public UrlResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		return new UrlResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<UrlResource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		// we don't have the capability to do more
		return Stream.of(this.getResource(uri));
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of("http", "https", "ftp");
	}
}

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
import io.inverno.mod.base.resource.ModuleResource;
import io.inverno.mod.base.resource.ResourceException;
import io.inverno.mod.base.resource.ResourceProvider;
import io.inverno.mod.base.resource.ResourceService;

/**
 * <p>
 * {@link ResourceProvider} implementation used to resolve module resources (ie. {@code module://module/path/to/resource}).
 * </p>
 *
 * <p>
 * This implementation allows to resolve resources with the same name but in different module by specifying '*' instead of a module name in the module resource URI.
 * </p>
 *
 * <pre>{@code
 * ModuleResourceProvider provider = new ModuleResourceProvider();
 *
 * // Returns all resources with name /path/to/module defined in the application modules
 * Stream<ModuleResource> resources = provider.getResources(URI.create("module://{@literal *}/path/to/resource");
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ModuleResource
 * @see AsyncResourceProvider
 * @see ResourceService
 */
@Bean(visibility = Visibility.PRIVATE)
public class ModuleResourceProvider extends AbstractResourceProvider<ModuleResource> {

	@Override
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		super.setMediaTypeService(mediaTypeService);
	}
	
	@Override
	public ModuleResource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		return new ModuleResource(uri, this.mediaTypeService);
	}
	
	@Override
	public Stream<ModuleResource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException {
		String moduleName = uri.getAuthority();
		if(moduleName != null && moduleName.equals("*")) {
			ModuleLayer moduleLayer = this.getClass().getModule().getLayer();
			if(moduleLayer != null) {
				return moduleLayer.modules().stream().map(module -> new ModuleResource(URI.create(ModuleResource.SCHEME_MODULE + "://" + module.getName() + uri.getPath()))).filter(moduleResource -> moduleResource.exists().orElse(false));
			}
		}
		return Stream.of(this.getResource(uri));
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return Set.of(ModuleResource.SCHEME_MODULE);
	}
}

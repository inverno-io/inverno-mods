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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Provide;
import io.winterframework.mod.base.resource.AsyncResourceProvider;
import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.base.resource.ResourceException;
import io.winterframework.mod.base.resource.ResourceProvider;
import io.winterframework.mod.base.resource.ResourceService;

/**
 * @author jkuhn
 *
 */
@Bean(name = "resourceService")
public class GenericResourceService implements @Provide ResourceService {

	private Map<String, ResourceProvider<?>> providers = Collections.emptyMap();
	
	private ExecutorService executor;
	
	public void setProviders(List<ResourceProvider<?>> providers) {
		Map<String, ResourceProvider<?>> providersMap = new HashMap<>();
		
		for(ResourceProvider<?> provider : providers) {
			for(String supportedScheme : provider.getSupportedSchemes()) {
				supportedScheme = supportedScheme.toLowerCase();
				// TODO at some point this is an issue in Spring as well, we should fix this in winter
				// provide annotation for sorting at compile time and be able to inject maps as well 
				// - annotations defined on the beans with some meta data
				// - annotations defined on multiple bean socket to specify sorting for list, array or sets
				// - we can also group by key to inject a map => new multi socket type
				// - this is a bit tricky as for selector when it comes to the injection of list along with single values 
				ResourceProvider<?> previousProvider = providersMap.put(supportedScheme, provider);
				if(previousProvider != null) {
					throw new IllegalStateException("Multiple providers found for scheme " + supportedScheme + ": " + previousProvider.toString() + ", " + provider.toString());
				}
			}
		}
		this.providers = providersMap;
	}
	
	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public Resource get(URI uri) throws IllegalArgumentException, ResourceException {
		String scheme = uri.getScheme();
		ResourceProvider<?> provider = this.providers.get(scheme);
		if(provider != null) {
			if(provider instanceof AsyncResourceProvider) {
				return ((AsyncResourceProvider<?>) provider).get(uri, this.executor);
			}
			else {
				return provider.get(uri);
			}
		}
		else {
			throw new IllegalArgumentException("Unsupported scheme: " + scheme);
		}
	}
}

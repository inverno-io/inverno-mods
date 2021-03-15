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
package io.winterframework.mod.base.resource;

import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * <p>
 * A resource provider providing async resources.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ResourceProvider
 * @see AsyncResource
 * 
 * @param <A> the type of the provided resource 
 */
public interface AsyncResourceProvider<A extends AsyncResource> extends ResourceProvider<A> {

	/**
	 * <p>
	 * Creates an async resource with the specified URI and executor service.
	 * </p>
	 * 
	 * @param uri      a URI
	 * @param executor an executor service
	 * 
	 * @return an async resource
	 * @throws NullPointerException     if the specified URI is null
	 * @throws IllegalArgumentException if the specified URI does not point to a
	 *                                  valid async resource
	 * @throws ResourceException        if there was an error creating the resource
	 */
	default A getResource(URI uri, ExecutorService executor) throws NullPointerException, IllegalArgumentException, ResourceException {
		A resource = this.getResource(uri);
		resource.setExecutor(executor);
		return resource;
	}
}

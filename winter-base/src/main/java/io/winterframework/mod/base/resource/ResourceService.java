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
import java.util.stream.Stream;

/**
 * <p>
 * Provides a unified access to resources, giving the ability to obtain
 * {@link Resource} instances for various kind of resources.
 * </p>
 *
 * <p>
 * Implementations should rely on multiple {@link ResourceProvider} to resolve
 * resources based on their kind specified in a URI schema.
 * </p>
 * 
 * @author jkuhn
 * 
 * @see ResourceProvider
 */
public interface ResourceService {

	/**
	 * <p>
	 * Returns a resource representation of the specified location.
	 * </p>
	 * 
	 * <p>
	 * The resource returned by this method might not actually exist.
	 * </p>
	 * 
	 * @param uri the resource location
	 * 
	 * @return A resource
	 * 
	 * @throws NullPointerException     if the specified URI is null
	 * @throws IllegalArgumentException if the resource type specified in the URI is
	 *                                  not supported by the implementation
	 * @throws ResourceException        if something goes wrong creating the
	 *                                  resource
	 */
	Resource getResource(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException;
	
	/**
	 * <p>
	 * Returns the resources that exist at the specified location.
	 * </p>
	 * 
	 * <p>
	 * The returned result depends first on the type of resource considered and what
	 * the corresponding {@link ResourceProvider} is capable of. For instance a file
	 * resource provider might try to match some kind of pattern in the specified
	 * URI to return multiple resources (eg. file:/a/**&#47;b.txt).
	 * </p>
	 * 
	 * @param uri the location of the resource to resolve
	 * 
	 * @return an array of resources
	 * 
	 * @throws NullPointerException     if the specified URI is null
	 * @throws IllegalArgumentException if the resource type specified in the URI is
	 *                                  not supported by the implementation
	 * @throws ResourceException        if something goes wrong creating the
	 *                                  resources
	 */
	Stream<Resource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException;
}

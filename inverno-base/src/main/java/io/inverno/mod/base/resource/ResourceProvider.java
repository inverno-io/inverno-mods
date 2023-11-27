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
package io.inverno.mod.base.resource;

import java.net.URI;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * A resource provider is used to resolve some particular kinds of resources.
 * </p>
 * 
 * <p>
 * It shall be used by a {@link ResourceService} to resolve resources based on
 * their kind specified in the scheme of the resource URI.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ResourceService
 * 
 * @param <A> the resource type
 */
public interface ResourceProvider<A extends Resource> {
	
	/**
	 * <p>
	 * Resolves the resource identified by the specified URI.
	 * </p>
	 * 
	 * @param uri a uri
	 * 
	 * @return a resource
	 * @throws NullPointerException     if the specified URI is null
	 * @throws IllegalArgumentException if the kind of resource specified in the URI
	 *                                  is not provided by the provider
	 * @throws ResourceException        if there was an error resolving the resource
	 */
	A getResource(URI uri) throws IllegalArgumentException, ResourceException;
	
	/**
	 * <p>
	 * Resolves the resources that exist at the location identified by the specified
	 * URI.
	 * </p>
	 * 
	 * <p>
	 * The returned result depends on the kind of resource considered and the
	 * provider itself. For instance a file resource provider could implement some
	 * kind of pattern in the specified URI to return multiple resources (eg.
	 * <code>file:/a/**&#47;b.txt</code>), but not all kind of resources can be
	 * listed in such a way, for instance it is not possible to list resources on a
	 * classpath or at a given URL.
	 * </p>
	 * 
	 * @param uri a URI
	 * 
	 * @return a stream or resources
	 * @throws NullPointerException     if the specified URI is null
	 * @throws IllegalArgumentException if the kind of resource specified in the URI
	 *                                  is not provided by the provider
	 * @throws ResourceException        if there was an error resolving resources
	 */
	Stream<? extends Resource> getResources(URI uri) throws IllegalArgumentException, ResourceException;
	
	/**
	 * <p>
	 * Returns the kinds of resources supported by the provider.
	 * </p>
	 * 
	 * @return a list of resource kinds
	 */
	Set<String> getSupportedSchemes();
}

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
import java.util.stream.Stream;

/**
 * <p>
 * Provides a unified access to resources, giving the ability to obtain
 * {@link Resource} instances for various kind of resources.
 * </p>
 *
 * <p>
 * Implementations can rely on multiple {@link ResourceProvider} to resolve
 * resources based on their kind specified in the scheme of the resource URI.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * 
 * @see ResourceProvider
 */
public interface ResourceService {

	/**
	 * <p>
	 * Resolves the resource identified by the specified URI.
	 * </p>
	 * 
	 * <p>
	 * The resource returned by this method might not actually exist.
	 * </p>
	 * 
	 * @param uri a URI
	 * 
	 * @return A resource
	 * @throws NullPointerException     if the specified URI is null
	 * @throws IllegalArgumentException if the resource type specified in the URI is
	 *                                  not supported by the implementation
	 * @throws ResourceException        if there was an error resolving the resource
	 */
	Resource getResource(URI uri) throws IllegalArgumentException, ResourceException;

	/**
	 * <p>
	 * Resolves the resources that exist the location identified by the specified
	 * URI.
	 * </p>
	 * 
	 * <p>
	 * The returned result depends on the type of resource considered and the
	 * corresponding {@link ResourceProvider}. For instance a file resource provider
	 * could implement some kind of pattern in the specified URI to return multiple
	 * resources (eg. <code>file:/a/**&#47;b.txt</code>), but not all kind of
	 * resources can be listed in such a way, for instance it is not possible to
	 * list resources on a classpath or at a given URL.
	 * </p>
	 * 
	 * @param uri a URI
	 * 
	 * @return an stream of resources
	 * @throws NullPointerException     if the specified URI is null
	 * @throws IllegalArgumentException if the resource type specified in the URI is
	 *                                  not supported by the implementation
	 * @throws ResourceException        if there was an error resolving the resource
	 */
	Stream<Resource> getResources(URI uri) throws NullPointerException, IllegalArgumentException, ResourceException;
}

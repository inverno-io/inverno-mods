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
import java.nio.file.Paths;
import java.util.Objects;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URI of the
 * form <code>jar:/path/to/resource</code> and looks up data in a jar file on
 * the file system system.
 * </p>
 * 
 * <p>
 * A typical usage is:
 * </p>
 * 
 * <blockquote><pre>
 * JarResource resource = new JarResource(URI.create("jar:/path/to/jar!/path/to/resource"));
 * ...
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ZipResource
 */
public class JarResource extends ZipResource {

	/**
	 * <p>
	 * Creates a jar resource with the specified URI.
	 * </p>
	 * 
	 * @param uri the resource URI
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  jar resource
	 */
	public JarResource(URI uri) throws IllegalArgumentException {
		this(uri, null);
	}
	
	/**
	 * <p>
	 * Creates a jar resource with the specified URI and media type service.
	 * </p>
	 * 
	 * @param uri              the resource URI
	 * @param mediaTypeService a media type service
	 * 
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  jar resource
	 */
	public JarResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(uri, SCHEME_JAR, mediaTypeService);
	}
	
	/**
	 * <p>
	 * Checks that the specified URI is a jar resource URI.
	 * </p>
	 * 
	 * @param uri the uri to check
	 * 
	 * @return the uri if it is a jar resource URI
	 * @throws IllegalArgumentException if the specified URI does not designate a
	 *                                  jar resource
	 */
	public static URI checkUri(URI uri) throws IllegalArgumentException {
		if(!Objects.requireNonNull(uri).getScheme().equals(SCHEME_JAR)) {
			throw new IllegalArgumentException("Not a " + SCHEME_JAR + " uri");
		}
		return uri.normalize();
	}
	
	@Override
	public Resource resolve(URI uri) {
		JarResource resolvedResource = new JarResource(URI.create(this.zipFsUri.toString() + "!" + this.resourcePath.resolve(Paths.get(uri.getPath())).toString()), this.getMediaTypeService());
		resolvedResource.setExecutor(this.getExecutor());
		return resolvedResource;
	}
}

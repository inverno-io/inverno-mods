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
 * @author jkuhn
 *
 */
public class JarResource extends ZipResource {

	public JarResource(URI uri) {
		this(uri, null);
	}
	
	public JarResource(URI uri, MediaTypeService mediaTypeService) {
		super(uri, SCHEME_JAR, mediaTypeService);
	}
	
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

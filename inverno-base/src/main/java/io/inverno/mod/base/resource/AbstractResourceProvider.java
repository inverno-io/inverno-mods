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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;

/**
 * <p>
 * Base implementation for {@link ResourceProvider}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ResourceProvider
 * @see Resource
 * 
 * @param <A> the type of the provided resource 
 */
public abstract class AbstractResourceProvider<A extends Resource> implements ResourceProvider<A> {

	/**
	 * The media type service.
	 */
	protected MediaTypeService mediaTypeService;
	
	/**
	 * <p>
	 * sets the media type service.
	 * </p>
	 * 
	 * @param mediaTypeService the media type service to set
	 */
	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		this.mediaTypeService = mediaTypeService;
	}
	
	/**
	 * <p>
	 * Returns a file system for the specified URI.
	 * </p>
	 * 
	 * <p>
	 * Returned instances are referenced counted so they can be reused when multiple
	 * threads needs to access the same file system.
	 * </p>
	 * 
	 * @param uri a URI a URI
	 * @return a file system a file system
	 * @throws IOException if there was error resolving the file system
	 */
	protected FileSystem getFileSystem(URI uri) throws IOException {
		return ReferenceCountedFileSystems.getFileSystem(uri);
	}
}

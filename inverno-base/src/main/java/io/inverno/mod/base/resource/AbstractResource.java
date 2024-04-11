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

import java.io.File;
import java.nio.file.Path;

/**
 * <p>
 * Base implementation for {@link Resource}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Resource
 */
public abstract class AbstractResource implements Resource {

	/**
	 * Flag indicating whether paths are Windows path (i.e. using {@code \} as path separator).
	 */
	protected static final boolean IS_WINDOWS_PATH = File.separatorChar == '\\';
	
	private static MediaTypeService defaultMediaTypeService;
	
	/**
	 * The media type service.
	 */
	private MediaTypeService mediaTypeService;
	
	/**
	 * <p>
	 * Creates a resource.
	 * </p>
	 */
	public AbstractResource() {
		this(null);
	}

	/**
	 * <p>
	 * Creates a resource with the specified media type service.
	 * </p>
	 * 
	 * @param mediaTypeService a media type service
	 */
	protected AbstractResource(MediaTypeService mediaTypeService) {
		this.setMediaTypeService(mediaTypeService);
	}
	
	private static MediaTypeService getDefaultMediaTypeService() {
		if(defaultMediaTypeService == null) {
			defaultMediaTypeService = new GenericMediaTypeService();
		}
		return defaultMediaTypeService;
	}

	/**
	 * <p>
	 * Returns a sanitized path representation of the specified path by normalizing it and replacing {@code \} path separator by standard {@code /} path separator.
	 * </p>
	 * 
	 * @param path the path to sanitize
	 * 
	 * @return a sanitized representation of the path
	 */
	protected static String pathToSanitizedString(Path path) {
		if(IS_WINDOWS_PATH) {
			return path.normalize().toString().replace("\\", "/");
		}
		return path.normalize().toString();
	}
	
	/**
	 * <p>
	 * Sets the media type service.
	 * </p>
	 * 
	 * @param mediaTypeService the media type service to set
	 */
	public final void setMediaTypeService(MediaTypeService mediaTypeService) {
		this.mediaTypeService = mediaTypeService;
	}
	
	/**
	 * <p>
	 * Returns the media type service.
	 * </p>
	 * 
	 * @return a media type service or the default media type service if not set
	 */
	protected MediaTypeService getMediaTypeService() {
		return this.mediaTypeService != null ? this.mediaTypeService : getDefaultMediaTypeService();
	}
	
	@Override
	public String getMediaType() throws ResourceException {
		return this.getMediaTypeService().getForFilename(this.getFilename());
	}
}

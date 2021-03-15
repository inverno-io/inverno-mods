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
import java.nio.file.Path;

/**
 * <p>
 * A media type service provides methods to determine a media type based on a
 * file name, a file extension, a path or a URI.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface MediaTypeService {

	/**
	 * <p>
	 * Determines the media type of the specified file extension.
	 * </p>
	 * 
	 * @param extension a file extension
	 * 
	 * @return a media type or null if no known media type has been found for the
	 *         specified file extension
	 */
	String getForExtension(String extension);

	/**
	 * <p>
	 * Determines the media type of the specified file name.
	 * </p>
	 * 
	 * @param filename a file name
	 * 
	 * @return a media type or null if no known media type has been found for the
	 *         specified file name
	 */
	String getForFilename(String filename);

	/**
	 * <p>
	 * Determines the media type of the specified path.
	 * </p>
	 * 
	 * @param path a path
	 * 
	 * @return a media type or null if no known media type has been found for the
	 *         specified path
	 */
	String getForPath(Path path);

	/**
	 * <p>
	 * Determines the media type of the specified path.
	 * </p>
	 * 
	 * @param uri a URI
	 * 
	 * @return a media type or null if no known media type has been found for the
	 *         specified URI
	 */
	String getForUri(URI uri);
}

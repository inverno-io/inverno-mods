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
package io.inverno.mod.boot.internal.resource;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.resource.MediaTypeService;
import io.inverno.mod.base.resource.ResourceException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>
 * Generic {@link MediaTypeService} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see MediaTypeService
 */
@Bean(name = "mediaTypeService")
@Overridable
public class GenericMediaTypeService implements @Provide MediaTypeService {

	@Override
	public String getForExtension(String extension) {
		return this.getForPath(Path.of("." + extension));
	}

	@Override
	public String getForFilename(String filename) {
		return this.getForPath(Path.of(filename));
	}

	@Override
	public String getForPath(Path path) {
		try {
			return Files.probeContentType(path);
		} 
		catch (IOException e) {
			throw new ResourceException("Unable to determine resource media type", e);
		}
	}

	@Override
	public String getForUri(URI uri) {
		return this.getForPath(Path.of(uri.getSchemeSpecificPart()));
	}
}

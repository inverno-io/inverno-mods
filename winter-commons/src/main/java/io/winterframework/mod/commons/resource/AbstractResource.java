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
package io.winterframework.mod.commons.resource;

import io.winterframework.mod.commons.internal.resource.GenericMediaTypeService;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractResource implements Resource {

	private static MediaTypeService defaultMediaTypeService;
	
	private MediaTypeService mediaTypeService;
	
	public AbstractResource() {
		this(null);
	}

	protected AbstractResource(MediaTypeService mediaTypeService) {
		this.setMediaTypeService(mediaTypeService);
	}
	
	private static MediaTypeService getDefaultMediaTypeService() {
		if(defaultMediaTypeService == null) {
			defaultMediaTypeService = new GenericMediaTypeService();
		}
		return defaultMediaTypeService;
	}

	public void setMediaTypeService(MediaTypeService mediaTypeService) {
		this.mediaTypeService = mediaTypeService;
	}
	
	protected MediaTypeService getMediaTypeService() {
		return this.mediaTypeService != null ? this.mediaTypeService : getDefaultMediaTypeService();
	}
	
	@Override
	public String getMediaType() {
		return this.getMediaTypeService().getForUri(this.getURI());
	}
}

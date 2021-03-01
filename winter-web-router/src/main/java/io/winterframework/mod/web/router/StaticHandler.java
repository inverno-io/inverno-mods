/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.web.router;

import java.net.URI;
import java.net.URISyntaxException;

import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.web.BadRequestException;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.WebException;

/**
 * @author jkuhn
 *
 */
public class StaticHandler implements WebExchangeHandler<WebExchange> {

	public static final String DEFAULT_PATH_PARAMETER_NAME = "path";
	
	private final Resource baseResource;
	
	private String pathParameterName;
	
	public StaticHandler(Resource baseResource) {
		this(baseResource, DEFAULT_PATH_PARAMETER_NAME);
	}
	
	public StaticHandler(Resource baseResource, String pathParameterName) {
		this.baseResource = baseResource;
		this.pathParameterName = pathParameterName;
	}
	
	public void setPathParameterName(String pathParameterName) {
		this.pathParameterName = pathParameterName;
	}
	
	@Override
	public void handle(WebExchange exchange) throws WebException {
		URI resourceUri;
		try {
			resourceUri = new URI(exchange.request().pathParameters().get(this.pathParameterName).orElseThrow(() -> new BadRequestException(this.pathParameterName + " is empty")).getValue()).normalize();
		}
		catch (URISyntaxException e) {
			throw new InternalServerErrorException(e);
		}
		
		if(resourceUri.isAbsolute()) {
			throw new BadRequestException("Resource can't be absolute");
		}
		if(resourceUri.getPath().startsWith(".")) {
			throw new NotFoundException();
		}

		try(Resource requestedResource = this.baseResource.resolve(resourceUri)) {
			Boolean exists = requestedResource.exists();
			if(exists == null || requestedResource.isFile()) {
				exchange.response().body().resource().value(requestedResource);
			}
			else if(exists) {
				// Try with index.html
				// TODO this should be configurable
				try(Resource requestedResourceIndex = requestedResource.resolve("index.html")) {
					exchange.response().body().resource().value(requestedResourceIndex);
				}
			}
			else {
				throw new NotFoundException();
			}
		}
		catch (NotFoundException e) {
			throw new NotFoundException(resourceUri.toString());
		}
	}
}

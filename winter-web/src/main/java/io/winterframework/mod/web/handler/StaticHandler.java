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
package io.winterframework.mod.web.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import io.winterframework.mod.commons.resource.Resource;
import io.winterframework.mod.web.BadRequestException;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebException;

/**
 * @author jkuhn
 *
 */
public class StaticHandler implements ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> {

	private Resource baseResource;
	
	private String basePath;
	
	// TODO the resource path could be determined by a path parameter
	public StaticHandler(Resource baseResource, String basePath) {
		this.baseResource = baseResource;
		this.basePath = basePath;
	}
	
	@Override
	public void handle(Exchange<RequestBody, ResponseBody> exchange) throws WebException {
		String resourceRelativePath = exchange.request().headers().getPath().substring(this.basePath.length());
		if(resourceRelativePath.startsWith("/")) {
			resourceRelativePath = resourceRelativePath.substring(1);
		}
		try {
			URI resourceUri = new URI(resourceRelativePath).normalize();
			if(resourceUri.isAbsolute()) {
				throw new BadRequestException("Resource can't be absolute");
			}
			if(resourceUri.getPath().startsWith(".")) {
				throw new NotFoundException();
			}
			try(Resource requestedResource = this.baseResource.resolve(resourceUri)) {
				exchange.response().body().resource().data(requestedResource);
			}
		} 
		catch (URISyntaxException | IOException e) {
			throw new InternalServerErrorException(e);
		}
	}
}

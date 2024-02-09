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
package io.inverno.mod.web.server;

import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.ExchangeHandler;
import java.nio.file.Path;
import java.util.Optional;

/**
 * <p>
 * A static handler used to serve static resources resolved from a base resource.
 * </p>
 *
 * <p>
 * This handler is typically used as a handler in a web route to serve static content. It uses a configurable path parameter to determine the path of the resource to serve relative to the base path.
 * </p>
 * 
 * <pre>{@code
 * WebRouter router = ...
 * 
 * router
 *     .route()
 *     .path("/static/{path:.*}")
 *     .handler(new StaticHandler<>(new FileResource("/path/to/resources/"));
 * 
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Resource
 * 
 * @param <A> the exchange context type
 */
public class StaticHandler<A extends ExchangeContext> implements ExchangeHandler<A, WebExchange<A>> {

	/**
	 * The default name of the path parameter defining the path to the resource.
	 */
	public static final String DEFAULT_PATH_PARAMETER_NAME = "path";
	
	/**
	 * The default welcome page file name: {@code index.html}.
	 */
	public static final String DEFAULT_WELCOME_PAGE = "index.html";
	
	private final Resource baseResource;
	
	private String pathParameterName;
	
	private String welcomePage;
	
	/**
	 * <p>
	 * Creates a static handler resolving resources from the specified base resource using the default path parameter name and welcome page.
	 * </p>
	 * 
	 * @param baseResource the base resource
	 * 
	 * @see StaticHandler#DEFAULT_PATH_PARAMETER_NAME
	 * @see StaticHandler#DEFAULT_WELCOME_PAGE
	 */
	public StaticHandler(Resource baseResource) {
		this(baseResource, DEFAULT_PATH_PARAMETER_NAME);
	}
	
	/**
	 * <p>
	 * Creates a static handler resolving resources from the specified base resource using the specified path parameter name.
	 * </p>
	 *
	 * @param baseResource      the base resource
	 * @param pathParameterName the path parameter name
	 *
	 * @see StaticHandler#DEFAULT_PATH_PARAMETER_NAME
	 */
	public StaticHandler(Resource baseResource, String pathParameterName) {
		this.baseResource = baseResource;
		this.pathParameterName = pathParameterName;
		this.welcomePage = DEFAULT_WELCOME_PAGE;
	}
	
	/**
	 * <p>
	 * Sets the name of the path parameter that specifies the path of a resource relative to the base path.
	 * </p>
	 *
	 * @param pathParameterName a path parameter name
	 */
	public void setPathParameterName(String pathParameterName) {
		this.pathParameterName = pathParameterName;
	}

	/**
	 * <p>
	 * Sets the name of the welcome page file to look for when a directory resource is requested.
	 * </p>
	 *
	 * @param welcomePage a file name
	 */
	public void setWelcomePage(String welcomePage) {
		this.welcomePage = welcomePage;
	}
	
	@Override
	public void handle(WebExchange<A> exchange) throws HttpException {
		String pathParameter = exchange.request().pathParameters().get(this.pathParameterName).map(Parameter::getValue).orElse("");
		boolean isDirectory = pathParameter.endsWith("/");
		
		Path resourcePath = Path.of(pathParameter).normalize();
		if(isDirectory) {
			resourcePath.resolve(this.welcomePage);
		}
		
		if(resourcePath.isAbsolute() || resourcePath.getRoot() != null) {
			throw new BadRequestException("Static resource path can't be absolute");
		}
		if(resourcePath.startsWith("..")) {
			throw new NotFoundException();
		}

		try(Resource requestedResource = this.baseResource.resolve(resourcePath)) {
			Optional<Boolean> exists = requestedResource.exists();
			Optional<Boolean> isFile = requestedResource.isFile();
			
			if(!exists.isPresent()) {
				// We can't determine the existence, this indicates an "opaque" resource like a URL, we can only try
				exchange.response().body().resource().value(requestedResource);
			}
			else if(exists.get()) {
				// Resource exists
				if(isFile.isPresent()) {
					// We know what the resource is
					if(isFile.get()) {
						// regular file
						exchange.response().body().resource().value(requestedResource);
					}
					else {
						// directory
						try(Resource requestedResourceIndex = requestedResource.resolve(this.welcomePage)) {
							exchange.response().body().resource().value(requestedResourceIndex);
						}
					}
				}
				else {
					// This might indicate stream based resources like module or URL in which case we'll have content but no file
					exchange.response().body().resource().value(requestedResource);
				}
			}
			else {
				// Resource doesn't exist
				if(!isFile.isPresent()) {
					// This might indicate stream based resources like module or URL in which case we might have a directory
					try(Resource requestedResourceIndex = requestedResource.resolve(this.welcomePage)) {
						exchange.response().body().resource().value(requestedResourceIndex);
					}
				}
				else {
					// Resource doesn't exist for sure
					throw new NotFoundException();
				}
			}
		}
		catch (NotFoundException e) {
			throw new NotFoundException(pathParameter);
		}
	}
}

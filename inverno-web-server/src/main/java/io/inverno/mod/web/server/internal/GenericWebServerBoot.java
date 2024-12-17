/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.ApplicationRuntime;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.web.server.WebServer;
import java.net.URI;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebServer.Boot} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean( name = "webServerBoot")
@Provide(WebServer.Boot.class)
public class GenericWebServerBoot implements WebServer.Boot, ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> {

	private final ServerDataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final ResourceService resourceService;

	private GenericWebServer<?> rootServer;

	/**
	 * <p>
	 * Creates a boot Web server.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 * @param resourceService       the resource service
	 */
	public GenericWebServerBoot(ServerDataConversionService dataConversionService, ObjectConverter<String> parameterConverter, ResourceService resourceService) {
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		this.resourceService = resourceService;
	}

	@Override
	public <T extends ExchangeContext> WebServer<T> webServer(Supplier<T> contextFactory) throws IllegalStateException {
		if(this.rootServer != null) {
			throw new IllegalStateException("A WebServer has already been initialized");
		}
		GenericWebServer<T> server = new GenericWebServer<>(this.dataConversionService, this.parameterConverter, contextFactory);
		this.rootServer = server;
		this.routeFavicon();
		return server;
	}

	/**
	 * <p>
	 * Defines default {@code /favicon.ico} route.
	 * </p>
	 */
	private void routeFavicon() {
		final URI favIconResourceURI;
		switch(ApplicationRuntime.getApplicationRuntime()) {
			case IMAGE_NATIVE: favIconResourceURI = URI.create("resource:/inverno_favicon.svg");
				break;
			case JVM_MODULE: favIconResourceURI = URI.create("module://" + this.getClass().getModule().getName() + "/inverno_favicon.svg");
				break;
			default: favIconResourceURI = URI.create("classpath:/inverno_favicon.svg");
				break;
		}

		this.rootServer.route().path("/favicon.ico").handler(exchange -> {
			try(Resource favicon = this.resourceService.getResource(favIconResourceURI)) {
				exchange.response().body().resource().value(favicon);
			}
			catch (Exception e) {
				throw new NotFoundException();
			}
		});
	}

	@Override
	public ExchangeContext createContext() {
		if(this.rootServer != null) {
			return this.rootServer.createContext();
		}
		return ServerController.super.createContext();
	}

	@Override
	public void handle(Exchange<ExchangeContext> exchange) throws HttpException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Mono<Void> defer(Exchange<ExchangeContext> exchange) throws HttpException {
		if(this.rootServer == null) {
			return Mono.fromRunnable(() -> exchange.response().headers(headers -> headers.status(Status.NOT_FOUND)).body().empty());
		}
		return this.rootServer.defer(exchange);
	}

	@Override
	public Mono<Void> defer(ErrorExchange<ExchangeContext> errorExchange) throws HttpException {
		if(this.rootServer == null) {
			// Delegate to the last resort error handler
			throw new NotFoundException();
		}
		return this.rootServer.defer(errorExchange);
	}
}

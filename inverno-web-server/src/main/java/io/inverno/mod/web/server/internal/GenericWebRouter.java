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
package io.inverno.mod.web.server.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.ApplicationRuntime;
import static io.inverno.mod.base.ApplicationRuntime.IMAGE_NATIVE;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebInterceptedRouter;
import io.inverno.mod.web.server.WebInterceptorManager;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.WebRouteManager;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WebRoutesConfigurer;
import io.inverno.mod.web.server.WebSocketRouteManager;
import java.net.URI;
import java.util.List;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebRouter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean( name = "webRouter" )
public class GenericWebRouter extends AbstractWebRouter implements @Provide WebRouter<ExchangeContext> {

	private final DataConversionService dataConversionService;
	private final ResourceService resourceService;
	private final ObjectConverter<String> parameterConverter;
	
	private final RoutingLink<ExchangeContext, WebExchange<ExchangeContext>, ?, WebRoute<ExchangeContext>> firstLink;

	/**
	 * <p>
	 * Creates a generic web router.
	 * </p>
	 * 
	 * @param resourceService       the resource service
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 */
	public GenericWebRouter(ResourceService resourceService, DataConversionService dataConversionService, ObjectConverter<String> parameterConverter) {
		this.resourceService = resourceService;
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		
		this.firstLink = new PathRoutingLink<>();
		this.firstLink
			.connect(new PathPatternRoutingLink<>())
			.connect(new MethodRoutingLink<>())
			.connect(new ConsumesRoutingLink<>(ACCEPT_CODEC))
			.connect(new ProducesRoutingLink<>(CONTENT_TYPE_CODEC))
			.connect(new LanguageRoutingLink<>(ACCEPT_LANGUAGE_CODEC))
			.connect(new HandlerRoutingLink<>());
	}
	
	@SuppressWarnings("unchecked")
	@Init
	public void init() {
		final URI favIconResourceURI;
		switch(ApplicationRuntime.getApplicationRuntime()) {
			case IMAGE_NATIVE: favIconResourceURI = URI.create("resource:/inverno_favicon.svg");
				break;
			case JVM_MODULE: favIconResourceURI = URI.create("module://" + this.getClass().getModule().getName() + "/inverno_favicon.svg");
				break;
			default: favIconResourceURI = URI.create("classpath:/inverno_favicon.svg");
				break;
		}
		
		this.route().path("/favicon.ico").handler(exchange -> {
			try(Resource favicon = resourceService.getResource(favIconResourceURI)) {
				exchange.response().body().resource().value(favicon);
			} 
			catch (Exception e) {
				throw new NotFoundException();
			}
		});
	}
	
	@Override
	void setRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.setRoute(route);
	}
	
	@Override
	void enableRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.enableRoute(route);
	}
	
	@Override
	void disableRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.disableRoute(route);
	}

	@Override
	void removeRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.removeRoute(route);
	}

	@Override
	public WebInterceptorManager<ExchangeContext, WebInterceptedRouter<ExchangeContext>> intercept() {
		return new GenericWebInterceptorManager(new GenericWebInterceptedRouter(this), CONTENT_TYPE_CODEC, ACCEPT_LANGUAGE_CODEC);
	}

	@Override
	public WebRouteManager<ExchangeContext, WebRouter<ExchangeContext>> route() {
		return new GenericWebRouteManager(this);
	}

	@Override
	public WebSocketRouteManager<ExchangeContext, WebRouter<ExchangeContext>> webSocketRoute() {
		return new GenericWebSocketRouteManager(this);
	}
	
	@Override
	public Set<WebRoute<ExchangeContext>> getRoutes() {
		GenericWebRouteExtractor routeExtractor = new GenericWebRouteExtractor(this, true);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	/**
	 * <p>
	 * Returns the routes defined without unwrapping their interceptors.
	 * </p>
	 * 
	 * @return the set of routes
	 */
	public Set<WebRoute<ExchangeContext>> getFilteredRoutes() {
		GenericWebRouteExtractor routeExtractor = new GenericWebRouteExtractor(this, false);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public Mono<Void> defer(Exchange<ExchangeContext> exchange) {
		return this.firstLink.defer(new GenericWebExchange(exchange, new GenericWebRequest(exchange.request(), this.parameterConverter, this.dataConversionService), new GenericWebResponse(exchange.response(), this.dataConversionService), this.dataConversionService));
	}
	
	/**
	 * <p>
	 * Throws an UnsupportedOperationException as we must always prefer {@link #defer(io.inverno.mod.http.server.Exchange) }
	 * </p>
	 */
	@Override
	public void handle(Exchange<ExchangeContext> exchange) throws HttpException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public WebInterceptedRouter<ExchangeContext> configureInterceptors(WebInterceptorsConfigurer<? super ExchangeContext> configurer) {
		GenericWebInterceptedRouter interceptedRouter = new GenericWebInterceptedRouter(this);
		if(configurer != null) {
			GenericWebInterceptableFacade facade = new GenericWebInterceptableFacade(interceptedRouter);
			configurer.configure(facade);
			
			return facade.getInterceptedRouter();
		}
		return interceptedRouter;
	}

	@Override
	public WebInterceptedRouter<ExchangeContext> configureInterceptors(List<WebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
		GenericWebInterceptedRouter interceptedRouter = new GenericWebInterceptedRouter(this);
		if(configurers != null && !configurers.isEmpty()) {
			GenericWebInterceptableFacade facade = new GenericWebInterceptableFacade(interceptedRouter);
			configurers.forEach(c -> c.configure(facade));
			return facade.getInterceptedRouter();
		}
		return interceptedRouter;
	}

	@Override
	public WebRouter<ExchangeContext> configureRoutes(WebRoutesConfigurer<? super ExchangeContext> configurer) {
		if(configurer != null) {
			GenericWebRoutableFacade<WebRouter<ExchangeContext>> facade = new GenericWebRoutableFacade<>(this);
			configurer.configure(facade);
		}
		return this;
	}
}

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
package io.inverno.mod.web.internal;

import java.net.URI;
import java.util.Set;
import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebConfiguration;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.WebRouteManager;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;
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
public class GenericWebRouter implements @Provide WebRouter<ExchangeContext> {

	private final WebConfiguration configuration;
	private final DataConversionService dataConversionService;
	private final ResourceService resourceService;
	private final ObjectConverter<String> parameterConverter;
	
	private final RoutingLink<ExchangeContext, WebExchange<ExchangeContext>, ?, WebRoute<ExchangeContext>> firstLink;
	private final OpenApiWebRouterConfigurer openApiConfigurer;
	private final WebjarsWebRouterConfigurer webjarsConfigurer;
	
	private WebRouterConfigurer<? extends ExchangeContext> configurer;
	
	/**
	 * <p>
	 * Creates a generic web router.
	 * </p>
	 * 
	 * @param configuration         the web module configuration
	 * @param resourceService       the resource service
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 */
	public GenericWebRouter(WebConfiguration configuration, ResourceService resourceService, DataConversionService dataConversionService, ObjectConverter<String> parameterConverter) {
		this.configuration = configuration;
		this.resourceService = resourceService;
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		this.openApiConfigurer = this.configuration.enable_open_api() ? new OpenApiWebRouterConfigurer(configuration, resourceService) : null;
		this.webjarsConfigurer = this.configuration.enable_webjars() ? new WebjarsWebRouterConfigurer(resourceService) : null;
		
		AcceptCodec acceptCodec = new AcceptCodec(false);
		ContentTypeCodec contentTypeCodec = new ContentTypeCodec();
		AcceptLanguageCodec acceptLanguageCodec = new AcceptLanguageCodec(false);
		
		this.firstLink = new PathRoutingLink<>();
		this.firstLink
			.connect(new PathPatternRoutingLink<>())
			.connect(new MethodRoutingLink<>())
			.connect(new ConsumesRoutingLink<>(acceptCodec))
			.connect(new ProducesRoutingLink<>(contentTypeCodec))
			.connect(new LanguageRoutingLink<>(acceptLanguageCodec))
			.connect(new HandlerRoutingLink<>());
	}
	
	@SuppressWarnings("unchecked")
	@Init
	public void init() {
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> h = exchange -> {
			try(Resource favicon = resourceService.getResource(new URI("module://" + this.getClass().getModule().getName() + "/inverno_favicon.svg"))) {
				exchange.response().body().resource().value(favicon);
			} 
			catch (Exception e) {
				throw new NotFoundException();
			}
		};
		
		this.route().path("/favicon.ico").handler(h);
		
		this.route().path("/favicon.ico").handler(exchange -> {
			try(Resource favicon = resourceService.getResource(new URI("module://" + this.getClass().getModule().getName() + "/inverno_favicon.svg"))) {
				exchange.response().body().resource().value(favicon);
			} 
			catch (Exception e) {
				throw new NotFoundException();
			}
		});
		
		if(this.webjarsConfigurer != null) {
			this.webjarsConfigurer.accept(this);
		}
		if(this.openApiConfigurer != null) {
			this.openApiConfigurer.accept(this);
		}
		
		if(this.configurer != null) {
			// We know it's working because the context is provided by the configurer
			((WebRouterConfigurer<ExchangeContext>)this.configurer).accept(this);
		}
	}
	
	/**
	 * <p>
	 * Sets the web router configurer used to initialize the router.
	 * </p>
	 * 
	 * @param configurer a web router configurer
	 */
	public void setConfigurer(WebRouterConfigurer<? extends ExchangeContext> configurer) {
		this.configurer = configurer;
	}
	
	/**
	 * <p>
	 * Sets the specified web route in the router.
	 * </p>
	 * 
	 * @param route a web route
	 */
	void setRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.setRoute(route);
	}
	
	/**
	 * <p>
	 * Enables the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to enable
	 */
	void enableRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.enableRoute(route);
	}
	
	/**
	 * <p>
	 * Disables the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to disable
	 */
	void disableRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.disableRoute(route);
	}

	/**
	 * <p>
	 * Removes the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to remove
	 */
	void removeRoute(WebRoute<ExchangeContext> route) {
		this.firstLink.removeRoute(route);
	}
	
	@Override
	public WebRouteManager<ExchangeContext> route() {
		return new GenericWebRouteManager(this);
	}
	
	@Override
	public Set<WebRoute<ExchangeContext>> getRoutes() {
		GenericWebRouteExtractor routeExtractor = new GenericWebRouteExtractor(this);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public Mono<Void> defer(Exchange<ExchangeContext> exchange) {
		return this.firstLink.defer(new GenericWebExchange(new GenericWebRequest(exchange.request(), this.dataConversionService, this.parameterConverter), new GenericWebResponse(exchange.response(), this.dataConversionService), exchange::finalizer, exchange.context()));
	}
	
	/**
	 * <p>
	 * Implements the ExchangeHandler contract, however this should not be invoked in order to remain reactive. 
	 * </p>
	 */
	@Override
	public void handle(Exchange<ExchangeContext> exchange) throws HttpException {
		this.defer(exchange).block();
	}
	
	@Override
	public ExchangeContext createContext() {
		return this.configurer != null ? this.configurer.createContext() : null;
	}
	
	/**
	 * <p>
	 * The web router configurer socket.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	@Bean( name = "webRouterConfigurer")
	public static interface ConfigurerSocket extends Supplier<WebRouterConfigurer<? extends ExchangeContext>> {}
}

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
package io.winterframework.mod.web.internal;

import java.net.URI;
import java.util.Set;
import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Init;
import io.winterframework.core.annotation.Provide;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.base.resource.ResourceService;
import io.winterframework.mod.http.base.NotFoundException;
import io.winterframework.mod.http.base.HttpException;
import io.winterframework.mod.http.base.internal.header.AcceptCodec;
import io.winterframework.mod.http.base.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.http.base.internal.header.ContentTypeCodec;
import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.web.WebExchange;
import io.winterframework.mod.web.WebRoute;
import io.winterframework.mod.web.WebRouteManager;
import io.winterframework.mod.web.WebRouter;
import io.winterframework.mod.web.WebConfiguration;
import io.winterframework.mod.web.WebRouterConfigurer;

/**
 * <p>
 * Generic {@link WebRouter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean( name = "webRouter" )
public class GenericWebRouter implements @Provide WebRouter<WebExchange> {

	private final WebConfiguration configuration;
	private final DataConversionService dataConversionService;
	private final ResourceService resourceService;
	private final ObjectConverter<String> parameterConverter;
	
	private final RoutingLink<WebExchange, ?, WebRoute<WebExchange>> firstLink;
	private final OpenApiWebRouterConfigurer openApiConfigurer;
	private final WebjarsWebRouterConfigurer webjarsConfigurer;
	
	private WebRouterConfigurer<WebExchange> configurer;
	
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
	
	@Init
	public void init() {
		this.route().path("/favicon.ico").handler(exchange -> {
			try(Resource favicon = resourceService.getResource(new URI("module://" + this.getClass().getModule().getName() + "/winter_favicon.svg"))) {
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
			this.configurer.accept(this);
		}
	}
	
	/**
	 * <p>
	 * Sets the web router configurer used to initialize the router.
	 * </p>
	 * 
	 * @param configurer a web router configurer
	 */
	public void setConfigurer(WebRouterConfigurer<WebExchange> configurer) {
		this.configurer = configurer;
	}
	
	/**
	 * <p>
	 * Sets the specified web route in the router.
	 * </p>
	 * 
	 * @param route a web route
	 */
	void setRoute(WebRoute<WebExchange> route) {
		this.firstLink.setRoute(route);
	}
	
	/**
	 * <p>
	 * Enables the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to enable
	 */
	void enableRoute(WebRoute<WebExchange> route) {
		this.firstLink.enableRoute(route);
	}
	
	/**
	 * <p>
	 * Disables the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to disable
	 */
	void disableRoute(WebRoute<WebExchange> route) {
		this.firstLink.disableRoute(route);
	}

	/**
	 * <p>
	 * Removes the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to remove
	 */
	void removeRoute(WebRoute<WebExchange> route) {
		this.firstLink.removeRoute(route);
	}
	
	@Override
	public WebRouteManager<WebExchange> route() {
		return new GenericWebRouteManager(this);
	}
	
	@Override
	public Set<WebRoute<WebExchange>> getRoutes() {
		GenericWebRouteExtractor routeExtractor = new GenericWebRouteExtractor(this);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public void handle(Exchange exchange) throws HttpException {
		this.firstLink.handle(new GenericWebExchange(new GenericWebRequest(exchange.request(), this.dataConversionService, this.parameterConverter), new GenericWebResponse(exchange.response(), this.dataConversionService)));
	}
	
	/**
	 * <p>
	 * The web router configurer socket.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	@Bean( name = "webRouterConfigurer")
	public static interface ConfigurerSocket extends Supplier<WebRouterConfigurer<WebExchange>> {}
}

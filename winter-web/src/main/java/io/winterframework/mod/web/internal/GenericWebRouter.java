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
import io.winterframework.mod.http.base.WebException;
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
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
@Bean( name = "webRouter" )
public class GenericWebRouter implements @Provide WebRouter<WebExchange> {

	private final WebConfiguration configuration;
	private final RoutingLink<WebExchange, ?, WebRoute<WebExchange>> firstLink;
	private final ResourceService resourceService;
	private final DataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final OpenApiWebRouterConfigurer openApiConfigurer;
	private final WebjarsWebRouterConfigurer webjarsConfigurer;
	
	private WebRouterConfigurer<WebExchange> configurer;
	
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
			.connect(new ProducesRoutingLink<>(acceptCodec, contentTypeCodec))
			.connect(new LanguageRoutingLink<>(acceptLanguageCodec))
			.connect(new HandlerRoutingLink<>());
	}
	
	@Init
	public void init() {
		this.route().path("/favicon.ico").handler(exchange -> {
			if(exchange.request().getPathAbsolute().equalsIgnoreCase("/favicon.ico")) {
				try(Resource favicon = resourceService.getResource(new URI("module://" + this.getClass().getModule().getName() + "/winter_favicon.svg"))) {
					exchange.response().body().resource().value(favicon);
				} 
				catch (Exception e) {
					throw new NotFoundException();
				}
			}
			else {
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
	
	public void setConfigurer(WebRouterConfigurer<WebExchange> configurer) {
		this.configurer = configurer;
	}
	
	void setRoute(WebRoute<WebExchange> route) {
		this.firstLink.setRoute(route);
	}
	
	void enableRoute(WebRoute<WebExchange> route) {
		this.firstLink.enableRoute(route);
	}
	
	void disableRoute(WebRoute<WebExchange> route) {
		this.firstLink.disableRoute(route);
	}

	void removeRoute(WebRoute<WebExchange> route) {
		this.firstLink.removeRoute(route);
	}
	
	@Override
	public Set<WebRoute<WebExchange>> getRoutes() {
		GenericWebRouteExtractor routeExtractor = new GenericWebRouteExtractor(this);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public WebRouteManager<WebExchange> route() {
		return new GenericWebRouteManager(this);
	}

	@Override
	public void handle(Exchange exchange) throws WebException {
		this.firstLink.handle(new GenericWebExchange(new GenericWebRequest(exchange.request(), this.dataConversionService, this.parameterConverter), new GenericWebResponse(exchange.response(), this.dataConversionService)));
	}
	
	@Bean( name = "webRouterConfigurer")
	public static interface ConfigurerSocket extends Supplier<WebRouterConfigurer<WebExchange>> {}
}

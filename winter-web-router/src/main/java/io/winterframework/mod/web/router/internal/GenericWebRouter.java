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
package io.winterframework.mod.web.router.internal;

import java.net.URI;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Init;
import io.winterframework.core.annotation.Provide;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.base.resource.ResourceService;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRoute;
import io.winterframework.mod.web.router.WebRouteManager;
import io.winterframework.mod.web.router.WebRouter;
import io.winterframework.mod.web.server.Exchange;

/**
 * @author jkuhn
 *
 */
@Bean( name = "webRouter" )
public class GenericWebRouter implements @Provide WebRouter<WebExchange> {

	private RoutingLink<WebExchange, ?, WebRoute<WebExchange>> firstLink;
	
	private Consumer<WebRouter<WebExchange>> configurer;
	
	private ResourceService resourceService;
	
	private BodyConversionService bodyConversionService;
	
	private ObjectConverter<String> parameterConverter;
	
	public GenericWebRouter(ResourceService resourceService, BodyConversionService bodyConversionService, ObjectConverter<String> parameterConverter) {
		this.resourceService = resourceService;
		this.bodyConversionService = bodyConversionService;
		this.parameterConverter = parameterConverter;
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
			if(exchange.request().headers().getPath().equalsIgnoreCase("/favicon.ico")) {
				try(Resource favicon = this.resourceService.get(URI.create("classpath:/winter_favicon.svg"))) {
					exchange.response().body().resource().data(favicon);
				} 
				catch (Exception e) {
					throw new NotFoundException();
				}
			}
			else {
				throw new NotFoundException();
			}
		});
		if(this.configurer != null) {
			this.configurer.accept(this);
		}
	}
	
	public void setConfigurer(Consumer<WebRouter<WebExchange>> configurer) {
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
		this.firstLink.handle(new GenericWebExchange(new GenericWebRequest(exchange.request(), this.bodyConversionService, this.parameterConverter), new GenericWebResponse(exchange.response(), this.bodyConversionService)));
	}
	
	@Bean( name = "WebRouterConfigurer")
	public static interface ConfigurerSocket extends Supplier<Consumer<WebRouter<WebExchange>>> {}
}

/*
 * Copyright 2022 Jeremy KUHN
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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.ErrorWebRouter;
import io.inverno.mod.web.ErrorWebRouterConfigurer;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.web.WebServerControllerConfigurer;

/**
 * <p>
 * Generic {@link ServerController} implementation which uses a {@link WebRouter} and an {@link ErrorWebRouter} to handle exchanges.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "webServerController", visibility = Bean.Visibility.PRIVATE )
public class GenericWebServerController implements @Provide ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> {

	private final WebRouter<ExchangeContext> router;
	
	private final ErrorWebRouter<ExchangeContext> errorRouter;
	
	private WebServerControllerConfigurer<? extends ExchangeContext> configurer;

	/**
	 * <p>
	 * Creates a generic Web Controller.
	 * </p>
	 * 
	 * @param router      the underlying exchange router.
	 * @param errorRouter the underlying error exchange router.
	 */
	public GenericWebServerController(WebRouter<ExchangeContext> router, ErrorWebRouter<ExchangeContext> errorRouter) {
		this.router = router;
		this.errorRouter = errorRouter;
	}
	
	@Init
	@SuppressWarnings("unchecked")
	public void init() {
		if(this.configurer != null) {
			this.router.configure((WebRouterConfigurer<ExchangeContext>)this.configurer);
			this.errorRouter.configure((ErrorWebRouterConfigurer<ExchangeContext>)this.configurer);
		}
	}
	
	/**
	 * <p>
	 * Sets the web controller configurer used to initialize the router and the error router.
	 * </p>
	 * 
	 * @param configurer a web controller configurer
	 */
	public void setConfigurer(WebServerControllerConfigurer<? extends ExchangeContext> configurer) {
		this.configurer = configurer;
	}
	
	@Override
	public Mono<Void> defer(Exchange<ExchangeContext> exchange) {
		return this.router.defer(exchange);
	}

	@Override
	public Mono<Void> defer(ErrorExchange<ExchangeContext> errorExchange) {
		return this.errorRouter.defer(errorExchange);
	}

	@Override
	public ExchangeContext createContext() {
		return this.configurer != null ? this.configurer.createContext() : null;
	}

	@Override
	public void handle(Exchange<ExchangeContext> exchange) {
		this.router.handle(exchange);
	}

	@Override
	public void handle(ErrorExchange<ExchangeContext> errorExchange) {
		this.errorRouter.handle(errorExchange);
	}
	
	/**
	 * <p>
	 * The web controller configurer socket.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	@Bean( name = "controllerConfigurer")
	public static interface ConfigurerSocket extends Supplier<WebServerControllerConfigurer<? extends ExchangeContext>> {}
}

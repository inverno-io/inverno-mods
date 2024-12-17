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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.ErrorWebRouteInterceptorManager;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRoute;
import io.inverno.mod.web.server.ErrorWebRouteManager;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRouteInterceptorManager;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.WebRouteManager;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.WebSocketRoute;
import io.inverno.mod.web.server.WebSocketRouteManager;
import io.inverno.mod.web.server.internal.router.InternalErrorWebRouteInterceptorRouter;
import io.inverno.mod.web.server.internal.router.InternalErrorWebRouter;
import io.inverno.mod.web.server.internal.router.InternalWebRouteInterceptorRouter;
import io.inverno.mod.web.server.internal.router.InternalWebRouter;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebServer} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public class GenericWebServer<A extends ExchangeContext> implements WebServer<A> {

	private final ServerDataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final InternalWebRouteInterceptorRouter<A> interceptorRouter;
	private final InternalWebRouter<A> router;
	private final InternalErrorWebRouteInterceptorRouter<A> errorInterceptorRouter;
	private final InternalErrorWebRouter<A> errorRouter;
	private final Supplier<A> contextFactory;

	/**
	 * <p>
	 * Creates a generic Web server.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    a parameter converter
	 * @param contextFactory        an exchange context factory
	 */
	public GenericWebServer(ServerDataConversionService dataConversionService, ObjectConverter<String> parameterConverter, Supplier<A> contextFactory) {
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		this.interceptorRouter = new InternalWebRouteInterceptorRouter<>();
		this.router = new InternalWebRouter<>();
		this.errorInterceptorRouter = new InternalErrorWebRouteInterceptorRouter<>();
		this.errorRouter = new InternalErrorWebRouter<>();
		this.contextFactory = contextFactory;
	}

	/**
	 * <p>
	 * Resolves the route matching the exchange and processes the exchange.
	 * </p>
	 *
	 * @param exchange the exchange to process
	 *
	 * @return the mono returned by the route exchange handler
	 *
	 * @throws NotFoundException if there was no route matching the exchange
	 */
	public Mono<Void> defer(Exchange<? super A> exchange) throws NotFoundException {
		@SuppressWarnings("unchecked")
		GenericWebExchange<A> webExchange = new GenericWebExchange<>(this.dataConversionService, this.parameterConverter, (Exchange<A>)exchange);
		WebRouteHandler<A> handler = this.router.resolve(webExchange);
		if(handler == null) {
			throw new NotFoundException();
		}
		return handler.defer(webExchange);
	}

	/**
	 * <p>
	 * Resolves the route matching the error exchange and processes the error exchange.
	 * </p>
	 *
	 * @param errorExchange the error exchange to process
	 *
	 * @return the mono returned by the route error exchange handler
	 *
	 * @throws HttpException                the original HTTP error if there was no error route matching the error exchange
	 * @throws InternalServerErrorException the original exception wrapped in an {@code InternalServerErrorException} if the original exception is not an {@link HttpException} and there was no error
	 *                                      route matching the error exchange
	 */
	public Mono<Void> defer(ErrorExchange<? super A> errorExchange) throws HttpException, InternalServerErrorException {
		@SuppressWarnings("unchecked")
		GenericErrorWebExchange<A> errorWebExchange = new GenericErrorWebExchange<>(this.dataConversionService, this.parameterConverter, (ErrorExchange<A>)errorExchange);
		ErrorWebRouteHandler<A> handler = this.errorRouter.resolve(errorWebExchange);
		if(handler == null) {
			if(errorExchange.getError() instanceof HttpException) {
				throw (HttpException)errorExchange.getError();
			}
			else if(errorExchange.getError() instanceof RuntimeException) {
				throw (RuntimeException)errorExchange.getError();
			}
			else {
				throw new InternalServerErrorException("Can't handle error", errorExchange.getError());
			}
		}
		return handler.defer(errorWebExchange);
	}

	/**
	 * <p>
	 * Creates a new exchange context.
	 * </p>
	 *
	 * @return an exchange context
	 */
	public A createContext() {
		return this.contextFactory.get();
	}

	@Override
	public Set<WebRoute<A>> getRoutes() {
		return this.router.getRoutes().stream()
			.filter(route -> route.get().getWebSocketHandler() == null)
			.map(GenericWebRoute::new)
			.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public Set<WebSocketRoute<A>> getWebSocketRoutes() {
		return this.router.getRoutes().stream()
			.filter(route -> route.get().getWebSocketHandler() != null)
			.map(GenericWebSocketRoute::new)
			.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public WebRouteInterceptorManager<A, Intercepted<A>> intercept() {
		return new GenericWebServer<A>.GenericInterceptedWebServer().intercept();
	}

	@Override
	public WebRouteManager<A, ? extends WebServer<A>> route() {
		return new GenericWebRouteManager<>(this, this.router.route());
	}

	@Override
	public WebSocketRouteManager<A, ? extends WebServer<A>> webSocketRoute() {
		return new GenericWebSocketRouteManager<>(this, this.router.route());
	}

	@Override
	public Intercepted<A> configureInterceptors(WebRouteInterceptor.Configurer<? super A> configurer) {
		return new GenericWebServer<A>.GenericInterceptedWebServer().configureInterceptors(configurer);
	}

	@Override
	public Intercepted<A> configureInterceptors(List<WebRouteInterceptor.Configurer<? super A>> configurers) {
		return new GenericWebServer<A>.GenericInterceptedWebServer().configureInterceptors(configurers);
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebServer<A> configureRoutes(WebRouter.Configurer<? super A> configurer) {
		if(configurer != null) {
			((WebRouter.Configurer<A>)configurer).configure(new WebRouterFacade<>(this));
		}
		return this;
	}

	@Override
	public WebServer<A> configureRoutes(List<WebRouter.Configurer<? super A>> configurers) {
		if(configurers != null && !configurers.isEmpty()) {
			for(WebRouter.Configurer<? super A> configurer : configurers) {
				 this.configureRoutes(configurer);
			}
		}
		return this;
	}

	@Override
	public Set<ErrorWebRoute<A>> getErrorRoutes() {
		return this.errorRouter.getRoutes().stream()
			.map(GenericErrorWebRoute::new)
			.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public ErrorWebRouteInterceptorManager<A, Intercepted<A>> interceptError() {
		return new GenericWebServer<A>.GenericInterceptedWebServer().interceptError();
	}

	@Override
	public ErrorWebRouteManager<A, ? extends WebServer<A>> routeError() {
		return new GenericErrorWebRouteManager<>(this, this.errorRouter.route());
	}

	@Override
	public Intercepted<A> configureErrorInterceptors(ErrorWebRouteInterceptor.Configurer<? super A> configurer) {
		return new GenericWebServer<A>.GenericInterceptedWebServer().configureErrorInterceptors(configurer);
	}

	@Override
	public Intercepted<A> configureErrorInterceptors(List<ErrorWebRouteInterceptor.Configurer<? super A>> configurers) {
		return new GenericWebServer<A>.GenericInterceptedWebServer().configureErrorInterceptors(configurers);
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebServer<A> configureErrorRoutes(ErrorWebRouter.Configurer<? super A> configurer) {
		if(configurer != null) {
			((ErrorWebRouter.Configurer<A>)configurer).configure(new ErrorWebRouterFacade<>(this));
		}
		return this;
	}

	@Override
	public WebServer<A> configureErrorRoutes(List<ErrorWebRouter.Configurer<? super A>> configurers) {
		if(configurers != null && !configurers.isEmpty()) {
			for(ErrorWebRouter.Configurer<? super A> configurer : configurers) {
				this.configureErrorRoutes(configurer);
			}
		}
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebServer<A> configure(Configurer<? super A> configurer) {
		if(configurer != null) {
			return ((Configurer<A>)configurer).configure(this);
		}
		return this;
	}

	@Override
	public WebServer<A> configure(List<Configurer<? super A>> configurers) {
		WebServer<A> server = this;
		if(configurers != null && !configurers.isEmpty()) {
			for(WebServer.Configurer<? super A> configurer : configurers) {
				server = this.configure(configurer);
			}
		}
		return server;
	}

	/**
	 * <p>
	 * Generic {@link WebServer.Intercepted} implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class GenericInterceptedWebServer implements WebServer.Intercepted<A>, Intercepting<A> {

		private final WebServer<A> parent;
		private final boolean chrooted;

		private InternalWebRouteInterceptorRouter<A> interceptorRouter;
		private InternalErrorWebRouteInterceptorRouter<A> errorInterceptorRouter;

		/**
		 * <p>
		 * Creates a generic intercepted Web server with no interceptors.
		 * </p>
		 */
		public GenericInterceptedWebServer() {
			this.parent = GenericWebServer.this;
			this.interceptorRouter = GenericWebServer.this.interceptorRouter;
			this.errorInterceptorRouter = GenericWebServer.this.errorInterceptorRouter;
			this.chrooted = false;
		}

		/**
		 * <p>
		 * Creates a generic intercepted Web server inheriting interceptors from parent.
		 * </p>
		 *
		 * @param parent the parent Web server
		 */
		public GenericInterceptedWebServer(WebServer<A> parent) {
			this(parent, false);
		}

		/**
		 * <p>
		 * Creates a chrooted generic intercepted Web server inheriting interceptors from parent.
		 * </p>
		 *
		 * <p>
		 * A chrooted intercepted Web server returns a {@link WebServerFacade} wrapping its parent when invoking {@link #unwrap()}. This allows to limit the scope to the originating Web server when
		 * configuring interceptors and routes using {@code configure*()} methods.
		 * </p>
		 *
		 * @param parent   the parent Web server parent
		 * @param chrooted true to chroot the intercepted server, false otherwise
		 */
		public GenericInterceptedWebServer(WebServer<A> parent, boolean chrooted) {
			this.parent = parent;
			this.chrooted = chrooted;
		}

		@Override
		@SuppressWarnings("unchecked")
		public final InternalWebRouteInterceptorRouter<A> getWebRouteInterceptorRouter() {
			if(this.interceptorRouter != null) {
				return this.interceptorRouter;
			}
			if(this.parent instanceof Intercepting) {
				return ((Intercepting<A>) this.parent).getWebRouteInterceptorRouter();
			}
			// should never happen: interceptorRouter and errorInterceptorRouter are always defined on the root intercepted server
			throw new IllegalStateException();
		}

		@Override
		public final void setWebRouteInterceptorRouter(InternalWebRouteInterceptorRouter<A> interceptorRouter) {
			this.interceptorRouter = interceptorRouter;
		}

		@Override
		@SuppressWarnings("unchecked")
		public final InternalErrorWebRouteInterceptorRouter<A> getErrorWebRouteInterceptorRouter() {
			if(this.errorInterceptorRouter != null) {
				return this.errorInterceptorRouter;
			}
			if(this.parent instanceof Intercepting) {
				return ((Intercepting<A>) this.parent).getErrorWebRouteInterceptorRouter();
			}
			// should never happen: interceptorRouter and errorInterceptorRouter are always defined on the root intercepted server
			throw new IllegalStateException();
		}

		@Override
		public final void setErrorWebRouteInterceptorRouter(InternalErrorWebRouteInterceptorRouter<A> errorInterceptorRouter) {
			this.errorInterceptorRouter = errorInterceptorRouter;
		}

		@Override
		public Set<WebRoute<A>> getRoutes() {
			return this.parent.getRoutes();
		}

		@Override
		public Set<WebSocketRoute<A>> getWebSocketRoutes() {
			return this.parent.getWebSocketRoutes();
		}

		@Override
		public WebRouteInterceptorManager<A, Intercepted<A>> intercept() {
			return new GenericWebRouteInterceptorManager<>(new GenericWebServer<A>.GenericInterceptedWebServer(this), this.getWebRouteInterceptorRouter().route());
		}

		@Override
		public WebRouteManager<A, WebServer.Intercepted<A>> route() {
			return new GenericWebRouteManager<>(this, GenericWebServer.this.router.route());
		}

		@Override
		public WebSocketRouteManager<A, WebServer.Intercepted<A>> webSocketRoute() {
			return new GenericWebSocketRouteManager<>(this, GenericWebServer.this.router.route());
		}

		@Override
		@SuppressWarnings("unchecked")
		public WebServer.Intercepted<A> configureInterceptors(WebRouteInterceptor.Configurer<? super A> configurer) {
			if(configurer != null) {
				return ((WebRouteInterceptorFacade<A>)((WebRouteInterceptor.Configurer<A>)configurer).configure(new WebRouteInterceptorFacade<>(this))).unwrap();
			}
			return this;
		}

		@Override
		public Intercepted<A> configureInterceptors(List<WebRouteInterceptor.Configurer<? super A>> configurers) {
			WebServer.Intercepted<A> server = this;
			if(configurers != null && !configurers.isEmpty()) {
				for(WebRouteInterceptor.Configurer<? super A> configurer : configurers) {
					server = server.configureInterceptors(configurer);
				}
			}
			return server;
		}

		@Override
		@SuppressWarnings("unchecked")
		public WebServer.Intercepted<A> configureRoutes(WebRouter.Configurer<? super A> configurer) {
			if(configurer != null) {
				((WebRouter.Configurer<A>)configurer).configure(new WebRouterFacade<>(this));
			}
			return this;
		}

		@Override
		public Intercepted<A> configureRoutes(List<WebRouter.Configurer<? super A>> configurers) {
			if(configurers != null && !configurers.isEmpty()) {
				for(WebRouter.Configurer<? super A> configurer : configurers) {
					this.configureRoutes(configurer);
				}
			}
			return this;
		}

		@Override
		public Set<ErrorWebRoute<A>> getErrorRoutes() {
			return this.parent.getErrorRoutes();
		}

		@Override
		public ErrorWebRouteInterceptorManager<A, Intercepted<A>> interceptError() {
			return new GenericErrorWebRouteInterceptorManager<>(new GenericWebServer<A>.GenericInterceptedWebServer(this), this.getErrorWebRouteInterceptorRouter().route());
		}

		@Override
		public ErrorWebRouteManager<A, WebServer.Intercepted<A>> routeError() {
			return new GenericErrorWebRouteManager<>(this, GenericWebServer.this.errorRouter.route());
		}

		@Override
		@SuppressWarnings("unchecked")
		public WebServer.Intercepted<A> configureErrorInterceptors(ErrorWebRouteInterceptor.Configurer<? super A> configurer) {
			if(configurer != null) {
				return ((ErrorWebRouteInterceptorFacade<A>)((ErrorWebRouteInterceptor.Configurer<A>)configurer).configure(new ErrorWebRouteInterceptorFacade<>(this))).unwrap();
			}
			return this;
		}

		@Override
		public Intercepted<A> configureErrorInterceptors(List<ErrorWebRouteInterceptor.Configurer<? super A>> configurers) {
			WebServer.Intercepted<A> server = this;
			if(configurers != null && !configurers.isEmpty()) {
				for(ErrorWebRouteInterceptor.Configurer<? super A> configurer : configurers) {
					server = server.configureErrorInterceptors(configurer);
				}
			}
			return server;
		}

		@Override
		@SuppressWarnings("unchecked")
		public WebServer.Intercepted<A> configureErrorRoutes(ErrorWebRouter.Configurer<? super A> configurer) {
			if(configurer != null) {
				((ErrorWebRouter.Configurer<A>)configurer).configure(new ErrorWebRouterFacade<>(this));
			}
			return this;
		}

		@Override
		public Intercepted<A> configureErrorRoutes(List<ErrorWebRouter.Configurer<? super A>> configurers) {
			if(configurers != null && !configurers.isEmpty()) {
				for(ErrorWebRouter.Configurer<? super A> configurer : configurers) {
					this.configureErrorRoutes(configurer);
				}
			}
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public WebServer.Intercepted<A> configure(Configurer<? super A> configurer) {
			if(configurer != null) {
				WebServer<A> result = ((Configurer<A>)configurer).configure(new GenericWebServer<A>.GenericInterceptedWebServer(this, true));
				return result instanceof WebServerFacade ? ((WebServerFacade<A>)result).unwrap() : (WebServer.Intercepted<A>)result;
			}
			return this;
		}

		@Override
		public Intercepted<A> configure(List<Configurer<? super A>> configurers) {
			WebServer.Intercepted<A> server = this;
			if(configurers != null && !configurers.isEmpty()) {
				for(WebServer.Configurer<? super A> configurer : configurers) {
					server = this.configure(configurer);
				}
			}
			return server;
		}

		@Override
		public WebServer<A> unwrap() {
			return this.chrooted && this.parent instanceof WebServer.Intercepted ? new WebServerFacade<>((WebServer.Intercepted<A>)this.parent) : this.parent;
		}
	}
}

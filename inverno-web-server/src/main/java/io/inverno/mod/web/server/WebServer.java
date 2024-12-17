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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.ExchangeContext;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * The Web server intercepts routes exchanges to matching handlers.
 * </p>
 *
 * <p>
 * It must be initialized using the {@link WebServer.Boot} by providing an {@link ExchangeContext} factory which creates the context attached to any exchange and used during its processing.
 * </p>
 *
 * <p>
 * It is the entry point for configuring the Web routes used to route Web exchanges to a matching Web exchange handlers and the error Web routes used to route error Web exchanges to a matching error
 * Web exchange handlers.
 * </p>
 *
 * <p>
 * Web routes and error Web routes interceptors are defined in trees of {@link WebServer.Intercepted} which are created by defining successive route interceptors which returns intercepted Web server
 * containing the successive interceptor definitions. In order to be intercepted, a Web route or an error Web route must be specified on such intercepted Web server. This allows to isolate interceptor
 * and route definitions which is particularly appreciated when defining them in multiple modules.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebRouter
 * @see WebRouteInterceptor
 * @see ErrorWebRouter
 * @see ErrorWebRouteInterceptor
 *
 * @param <A> the exchange context type
 */
public interface WebServer<A extends ExchangeContext> extends WebRouter<A>, WebRouteInterceptor<A>, ErrorWebRouter<A>, ErrorWebRouteInterceptor<A> {

	@Override
	Set<WebRoute<A>> getRoutes();

	@Override
	Set<WebSocketRoute<A>> getWebSocketRoutes();

	@Override
	WebRouteInterceptorManager<A, WebServer.Intercepted<A>> intercept();

	@Override
	default WebServer.Intercepted<A> intercept(Function<WebRouteInterceptorManager<A, ? extends WebRouteInterceptor<A>>, ? extends WebRouteInterceptor<A>> configurer) {
		return (WebServer.Intercepted<A>)configurer.apply(this.intercept());
	}

	@Override
	WebRouteManager<A, ? extends WebServer<A>> route();

	@Override
	default WebServer<A> route(Consumer<WebRouteManager<A, ? extends WebRouter<A>>> configurer) {
		configurer.accept(this.route());
		return this;
	}

	@Override
	WebSocketRouteManager<A, ? extends WebServer<A>> webSocketRoute();

	@Override
	default WebServer<A> webSocketRoute(Consumer<WebSocketRouteManager<A, ? extends WebRouter<A>>> configurer) {
		configurer.accept(this.webSocketRoute());
		return this;
	}

	@Override
	WebServer.Intercepted<A> configureInterceptors(WebRouteInterceptor.Configurer<? super A> configurer);

	@Override
	WebServer.Intercepted<A> configureInterceptors(List<WebRouteInterceptor.Configurer<? super A>> configurers);

	@Override
	WebServer<A> configureRoutes(WebRouter.Configurer<? super A> configurer);

	@Override
	WebServer<A> configureRoutes(List<WebRouter.Configurer<? super A>> configurers);

	@Override
	Set<ErrorWebRoute<A>> getErrorRoutes();

	@Override
	ErrorWebRouteInterceptorManager<A, WebServer.Intercepted<A>> interceptError();

	@Override
	default WebServer.Intercepted<A> interceptError(Function<ErrorWebRouteInterceptorManager<A, ? extends ErrorWebRouteInterceptor<A>>, ? extends ErrorWebRouteInterceptor<A>> configurer) {
		return (WebServer.Intercepted<A>)configurer.apply(this.interceptError());
	}

	@Override
	ErrorWebRouteManager<A, ? extends WebServer<A>> routeError();

	@Override
	default WebServer<A> routeError(Consumer<ErrorWebRouteManager<A, ? extends ErrorWebRouter<A>>> configurer) {
		configurer.accept(this.routeError());
		return this;
	}

	@Override
	WebServer.Intercepted<A> configureErrorInterceptors(ErrorWebRouteInterceptor.Configurer<? super A> configurer);

	@Override
	WebServer.Intercepted<A> configureErrorInterceptors(List<ErrorWebRouteInterceptor.Configurer<? super A>> configurers);

	@Override
	WebServer<A> configureErrorRoutes(ErrorWebRouter.Configurer<? super A> configurer);

	@Override
	WebServer<A> configureErrorRoutes(List<ErrorWebRouter.Configurer<? super A>> configurers);

	/**
	 * <p>
	 * Configures the Web server.
	 * </p>
	 *
	 * <p>
	 * The resulting Web server can be the originating Web server or an intercepted Web server depending on whether interceptors are defined in the configurer and the intercepted Web server thus
	 * obtained is returned by the configurer.
	 * </p>
	 *
	 * @param configurer A Web server configurer
	 *
	 * @return a Web server
	 */
	WebServer<A> configure(Configurer<? super A> configurer);

	/**
	 * <p>
	 * Configures the Web server.
	 * </p>
	 *
	 * <p>
	 * The resulting Web server can be the originating Web server or an intercepted Web server depending on whether interceptors are defined in the configurers and the intercepted Web server thus
	 * obtained is returned by the <strong>last</strong> configurer.
	 * </p>
	 *
	 * @param configurers A Web server configurer
	 *
	 * @return a Web server
	 */
	WebServer<A> configure(List<Configurer<? super A>> configurers);

	/**
	 * <p>
	 * Initializes the Web server in order to be able to specify context aware interceptors and routes.
	 * </p>
	 *
	 * <p>
	 * A Web server is responsible for creating the {@link ExchangeContext} for each requests and route them to the right exchange handler and, in case of error, route the error Web exchange to the right
	 * error Web exchange handler. The boot Web server must be used on startup to provide the exchange context factory that will be used to create the context on each request.
	 * </p>
	 *
	 * <p>
	 * The resulting {@link WebServer} is then used to define the interceptors, routes, error interceptors and error routes.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	interface Boot {

		/**
		 * <p>
		 * Initializes the Web server with an empty context factory (i.e. supplying {@code null}).
		 * </p>
		 *
		 * <p>
		 * Applications which do not rely on the exchange context to serve requests can be initialized that way.
		 * </p>
		 *
		 * @return the initialized Web server
		 *
		 * @throws IllegalStateException if the Web server has already been initialized
		 */
		default WebServer<ExchangeContext> webServer() throws IllegalStateException {
			return this.webServer(() -> null);
		}

		/**
		 * <p>
		 * Initializes the Web server with the specified context factory.
		 * </p>
		 *
		 * <p>
		 * The context type is unique for all interceptors and routes defined subsequently on the server as a result the usage of polymorphism and interfaces should be preferred all the way to give a
		 * maximum of flexibility.
		 * </p>
		 *
		 * @param <T> the context type, ideally an interface
		 * @param contextFactory the context factory
		 *
		 * @return the initialized Web server
		 *
		 * @throws IllegalStateException if the Web server has already been initialized
		 */
		<T extends ExchangeContext> WebServer<T> webServer(Supplier<T> contextFactory) throws IllegalStateException;
	}

	/**
	 * <p>
	 * An intercepted Web server applies interceptors when defining Web routes or error Web routes when their criteria are matching interceptor definitions.
	 * </p>
	 *
	 * <p>
	 * Intercepted Web servers are returned by {@link WebRouteInterceptor} or {@link ErrorWebRouteInterceptor} after defining interceptors resulting in trees of intercepted Web server where each node
	 * can apply all previously defined interceptors.
	 * </p>
	 *
	 * <p>
	 * When defining a route, the intercepted Web server resolves all interceptors matching the route's criteria and applies them to the route handler. When the set of exchanges matched by the route
	 * is bigger than the set of exchanges matched by the interceptor definition, the exchange interceptor is wrapped to order to filter exchanges that exactly match the interceptor's criteria.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	interface Intercepted<A extends ExchangeContext> extends WebServer<A> {

		@Override
		WebRouteManager<A, WebServer.Intercepted<A>> route();

		@Override
		default WebServer.Intercepted<A> route(Consumer<WebRouteManager<A, ? extends WebRouter<A>>> configurer) {
			configurer.accept(this.route());
			return this;
		}

		@Override
		WebSocketRouteManager<A, WebServer.Intercepted<A>> webSocketRoute();

		@Override
		default WebServer.Intercepted<A> webSocketRoute(Consumer<WebSocketRouteManager<A, ? extends WebRouter<A>>> configurer) {
			configurer.accept(this.webSocketRoute());
			return this;
		}

		@Override
		WebServer.Intercepted<A> configureRoutes(WebRouter.Configurer<? super A> configurer);

		@Override
		WebServer.Intercepted<A> configureRoutes(List<WebRouter.Configurer<? super A>> configurers);

		@Override
		ErrorWebRouteManager<A, WebServer.Intercepted<A>> routeError();

		@Override
		default WebServer.Intercepted<A> routeError(Consumer<ErrorWebRouteManager<A, ? extends ErrorWebRouter<A>>> configurer) {
			configurer.accept(this.routeError());
			return this;
		}

		@Override
		WebServer.Intercepted<A> configureErrorRoutes(ErrorWebRouter.Configurer<? super A> configurer);

		@Override
		WebServer.Intercepted<A> configureErrorRoutes(List<ErrorWebRouter.Configurer<? super A>> configurers);

		@Override
		WebServer.Intercepted<A> configure(WebServer.Configurer<? super A> configurer);

		@Override
		WebServer.Intercepted<A> configure(List<WebServer.Configurer<? super A>> configurers);

		/**
		 * <p>
		 * Returns the originating Web server.
		 * </p>
		 *
		 * <p>
		 * Since intercepted Web servers are defined in a hierarchical way, the returned Web server can be an intercepted Web server.
		 * </p>
		 *
		 * @return an intercepted Web server or the root Web server
		 */
		WebServer<A> unwrap();
	}

	/**
	 * <p>
	 * A configurer used to configure a Web server.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	interface Configurer<A extends ExchangeContext> {

		/**
		 * <p>
		 * Configures interceptors and routes in a Web server.
		 * </p>
		 *
		 * @param server the Web server to configure
		 *
		 * @return the resulting Web server
		 */
		WebServer<A> configure(WebServer<A> server);
	}
}

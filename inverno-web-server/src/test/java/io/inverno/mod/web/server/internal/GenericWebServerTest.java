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

import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.ErrorWebRoute;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * <p>
 *
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebServerTest {

	@Test
	public void testIntercept() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicInteger test_intercept_interceptor_invoked = new AtomicInteger();
		AtomicBoolean test_intercept_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_no_intercept_handler_invoked = new AtomicBoolean();
		AtomicInteger json_interceptor_invoked = new AtomicInteger();
		AtomicBoolean test_intercept_json_handler_invoked = new AtomicBoolean();
		AtomicBoolean json_handler_invoked = new AtomicBoolean();

		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> pathInterceptor = exchange -> {
			test_intercept_interceptor_invoked.incrementAndGet();
			return Mono.just(exchange);
		};

		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> jsonInterceptor = exchange -> {
			json_interceptor_invoked.incrementAndGet();
			return Mono.just(exchange);
		};

		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
			.intercept().path("/test_intercept").interceptor(pathInterceptor)
			.route().path("/test_no_intercept").handler(exchange -> test_no_intercept_handler_invoked.set(true))
			.route().path("/test_intercept").handler(exchange -> test_intercept_handler_invoked.set(true))
			.intercept().consume("application/json").interceptor(jsonInterceptor)
			.route().consume("application/json").handler(exchange -> json_handler_invoked.set(true))
			.route().path("/test_intercept").consume("application/json").handler(exchange -> test_intercept_json_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertEquals(0, test_intercept_interceptor_invoked.get());
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_intercept").build()).block();
		Assertions.assertEquals(1, test_intercept_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(0, json_interceptor_invoked.get());
		Assertions.assertTrue(test_intercept_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_no_intercept").build()).block();
		Assertions.assertEquals(0, test_intercept_interceptor_invoked.get());
		Assertions.assertEquals(0, json_interceptor_invoked.get());
		Assertions.assertTrue(test_no_intercept_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/undefined").contentType("application/json").build()).block();
		Assertions.assertEquals(0, test_intercept_interceptor_invoked.get());
		Assertions.assertEquals(1, json_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(json_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test_intercept").contentType("application/json").build()).block();
		Assertions.assertEquals(1, test_intercept_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(1, json_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_intercept_json_handler_invoked.getAndSet(false));

		Set<WebRoute<ExchangeContext>> routes = server.getRoutes();
		Assertions.assertEquals(6, routes.size());
		for(WebRoute<ExchangeContext> route : routes) {
			switch(route.getPath()) {
				case "/favicon.ico":
				case "/test":
				case "/test_no_intercept": {
					Assertions.assertTrue(route.getInterceptors().isEmpty());
					break;
				}
				case "/test_intercept": {
					switch(route.getConsume()) {
						case "application/json": {
							Assertions.assertEquals(List.of(pathInterceptor, jsonInterceptor), route.getInterceptors());
							break;
						}
						case null: {
							Assertions.assertEquals(List.of(pathInterceptor), route.getInterceptors());
							break;
						}
						default: Assertions.fail("/test_intercept route with unexpected consumed content type: " + route.getConsume());
					}
					break;
				}
				case null: {
					Assertions.assertEquals("application/json", route.getConsume());
					// Both interceptors are defined in this route because they both match the route:
					// - pathInterceptor matches the route when the request's path is /test_intercept
					// - jsonInterceptor matches the route when the request's content type is application/json
					// we can have a request to /test_intercept and application/json content type
					Assertions.assertEquals(List.of(pathInterceptor, jsonInterceptor), route.getInterceptors());
					break;
				}
				default: Assertions.fail("Route with unexpected path: " + route.getPath());
			}
		}
	}

	@Test
	public void testRoute() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicBoolean json_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		Assertions.assertThrows(NotFoundException.class, () -> bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/undefined").build()).block());
		Assertions.assertFalse(test_handler_invoked.get());

		server
			.route().consume("application/json").handler(exchange -> json_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test_json").contentType("application/json").build()).block();
		Assertions.assertTrue(json_handler_invoked.getAndSet(false));
	}

	@Test
	public void test_route_with_context() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<GenericWebServerTest.TestContext> server = bootServer.webServer(GenericWebServerTest.TestContext::new);

		AtomicBoolean root_handler_invoked = new AtomicBoolean();
		AtomicInteger root_interceptor_invoked = new AtomicInteger();
		server = server
			.route().path("/root").handler(exchange -> root_handler_invoked.set(true))
			.intercept().path("/root_intercept").interceptor(exchange -> {
				root_interceptor_invoked.incrementAndGet();
				return Mono.just(exchange);
			});

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_post_handler_invoked = new AtomicBoolean();
		AtomicBoolean root_intercept_handler_invoked = new AtomicBoolean();
		AtomicInteger root_post_interceptor_invoked = new AtomicInteger();
		AtomicInteger test_post_interceptor_invoked = new AtomicInteger();
		AtomicReference<String> test_context_handler_invoked = new AtomicReference<>();
		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
			.intercept().path("/root_intercept").method(Method.POST).interceptor(exchange -> {
				root_post_interceptor_invoked.incrementAndGet();
				return Mono.just(exchange);
			})
			.intercept().path("/test").method(Method.POST).interceptor(exchange -> {
				test_post_interceptor_invoked.incrementAndGet();
				return Mono.just(exchange);
			})
			.route().path("/root_intercept").handler(exchange -> root_intercept_handler_invoked.set(true))
			.route().path("/test").method(Method.POST).handler(exchange -> test_post_handler_invoked.set(true))
			.route().path("/test_context").method(Method.GET).handler(exchange -> test_context_handler_invoked.set(exchange.context().getFoo()));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/root").build()).block();
		Assertions.assertEquals(0, root_interceptor_invoked.get());
		Assertions.assertEquals(0, root_post_interceptor_invoked.get());
		Assertions.assertEquals(0, test_post_interceptor_invoked.get());
		Assertions.assertTrue(root_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertEquals(0, root_interceptor_invoked.get());
		Assertions.assertEquals(0, root_post_interceptor_invoked.get());
		Assertions.assertEquals(0, test_post_interceptor_invoked.get());
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/root_intercept").build()).block();
		Assertions.assertEquals(1, root_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(0, root_post_interceptor_invoked.get());
		Assertions.assertEquals(0, test_post_interceptor_invoked.get());
		Assertions.assertTrue(root_intercept_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/root_intercept").build()).block();
		Assertions.assertEquals(1, root_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(1, root_post_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(0, test_post_interceptor_invoked.get());
		Assertions.assertTrue(root_intercept_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test").build()).block();
		Assertions.assertEquals(0, root_interceptor_invoked.get());
		Assertions.assertEquals(0, root_post_interceptor_invoked.get());
		Assertions.assertEquals(1, test_post_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_post_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.<ExchangeContext>exchangeBuilder(Method.GET, "/test_context", new GenericWebServerTest.TestContext()).build()).block();
		Assertions.assertEquals(0, root_interceptor_invoked.get());
		Assertions.assertEquals(0, root_post_interceptor_invoked.get());
		Assertions.assertEquals(0, test_post_interceptor_invoked.get());
		Assertions.assertEquals("foo", test_context_handler_invoked.getAndSet(null));
	}

	@Test
	public void testRoute_path() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean static_handler_invoked = new AtomicBoolean();
		AtomicBoolean pattern_handler_invoked = new AtomicBoolean();
		Map<String, String> parameters = new HashMap<>();
		server
			.route().path("/a/b").handler(exchange -> static_handler_invoked.set(true))
			.route().path("/{p1}/{p2}").handler(exchange -> {
				exchange.request().pathParameters().getAll().forEach((key, value) -> parameters.put(key, value.getValue()));
				pattern_handler_invoked.set(true);
			});

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/a/b").build()).block();
		Assertions.assertTrue(static_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/x/y").build()).block();
		Assertions.assertTrue(pattern_handler_invoked.getAndSet(true));
		Assertions.assertEquals(Map.of("p1", "x", "p2", "y"), parameters);
		parameters.clear();
	}

	@Test
	public void testRoute_method() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_get_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_post_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").method(Method.GET).handler(exchange -> test_get_handler_invoked.set(true))
			.route().path("/test").method(Method.POST).handler(exchange -> test_post_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test").build()).block();
		Assertions.assertTrue(test_post_handler_invoked.getAndSet(false));

		Assertions.assertEquals(Set.of(Method.GET, Method.POST), Assertions.assertThrows(MethodNotAllowedException.class, () ->  bootServer.defer(MockExchanges.exchangeBuilder(Method.DELETE, "/test").contentType("text/plain").build()).block()).getAllowedMethods());

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").contentType("application/json").build()).block();
		Assertions.assertTrue(test_get_handler_invoked.getAndSet(false));

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test").build()).block();
		Assertions.assertTrue(test_post_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.DELETE, "/test").contentType("text/plain").build()).block();
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").contentType("application/json").build()).block();
		Assertions.assertTrue(test_get_handler_invoked.getAndSet(false));
	}

	@Test
	public void testRoute_consume() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_json_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
			.route().path("/test").consume("application/json").handler(exchange -> test_json_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		Assertions.assertThrows(UnsupportedMediaTypeException.class, () -> bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").contentType("text/plain").build()).block());

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").contentType("application/json").build()).block();
		Assertions.assertTrue(test_json_handler_invoked.getAndSet(false));
	}

	@Test
	public void testRoute_produce() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_json_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").produce("application/json").handler(exchange -> test_json_handler_invoked.set(true))
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		Assertions.assertEquals(Set.of("application/json"), Assertions.assertThrows(NotAcceptableException.class, () -> bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").accept("text/plain").build()).block()).getAcceptableMediaTypes());

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").accept("application/json").build()).block();
		Assertions.assertTrue(test_json_handler_invoked.getAndSet(false));
	}

	@Test
	public void testRoute_language() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_fr_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").language("fr").handler(exchange -> test_fr_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertTrue(test_fr_handler_invoked.getAndSet(false));

		Assertions.assertThrows(NotFoundException.class, () -> bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").acceptLanguage("en").build()).block());

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").acceptLanguage("fr").build()).block();
		Assertions.assertTrue(test_fr_handler_invoked.getAndSet(false));

		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").acceptLanguage("en").build()).block();
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").acceptLanguage("fr").build()).block();
		Assertions.assertTrue(test_fr_handler_invoked.getAndSet(false));
	}

	@Test
	public void testConfigureRoutes() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_intercept_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_no_intercept_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_intercept_external_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_no_intercept_external_handler_invoked = new AtomicBoolean();
		AtomicInteger test_interceptor_invoked = new AtomicInteger();
		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
			.intercept().path("/test_intercept").interceptor(exchange -> {
				test_interceptor_invoked.incrementAndGet();
				return Mono.just(exchange);
			})
			.configureRoutes(routes -> {
				routes
					.route().path("/test_intercept").method(Method.GET).handler(exchange -> test_intercept_handler_invoked.set(true))
					.route().path("/test_no_intercept").handler(exchange -> test_no_intercept_handler_invoked.set(true));
			})
			.route().path("/test_intercept").method(Method.POST).handler(exchange -> test_intercept_external_handler_invoked.set(true))
			.route().path("/test_no_intercept_external").handler(exchange -> test_no_intercept_external_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").acceptLanguage("en").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_intercept").acceptLanguage("en").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_intercept_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_no_intercept").acceptLanguage("en").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_no_intercept_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test_intercept").acceptLanguage("en").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_intercept_external_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_no_intercept_external").acceptLanguage("en").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_no_intercept_external_handler_invoked.getAndSet(false));
	}

	@Test
	public void testInterceptError() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean bad_request_handler_invoked = new AtomicBoolean();
		AtomicBoolean not_found_handler_invoked = new AtomicBoolean();
		AtomicBoolean not_acceptable_handler_invoked = new AtomicBoolean();
		AtomicBoolean error_json_handler_invoked = new AtomicBoolean();
		AtomicBoolean not_found_json_handler_invoked = new AtomicBoolean();
		AtomicInteger not_found_interceptor_invoked = new AtomicInteger();
		AtomicInteger error_json_interceptor_invoked = new AtomicInteger();

		ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>> notFoundErrorInterceptor = exchange -> {
			not_found_interceptor_invoked.incrementAndGet();
			return Mono.just(exchange);
		};

		ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>> errorJsonInterceptor = exchange -> {
			error_json_interceptor_invoked.incrementAndGet();
			return Mono.just(exchange);
		};

		server
			.routeError().error(BadRequestException.class).handler(exchange -> bad_request_handler_invoked.set(true))
			.interceptError().error(NotFoundException.class).interceptor(notFoundErrorInterceptor)
			.routeError().error(NotAcceptableException.class).handler(exchange -> not_acceptable_handler_invoked.set(true))
			.routeError().error(NotFoundException.class).handler(exchange -> not_found_handler_invoked.set(true))
			.interceptError().produce("application/json").interceptor(errorJsonInterceptor)
			.routeError().produce("application/json").handler(exchange -> error_json_handler_invoked.set(true))
			.routeError().error(NotFoundException.class).produce("application/json").handler(exchange -> not_found_json_handler_invoked.set(true));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new BadRequestException(), "/test").build()).block();
		Assertions.assertTrue(bad_request_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotAcceptableException(), "/test").build()).block();
		Assertions.assertEquals(0, not_found_interceptor_invoked.get());
		Assertions.assertEquals(0, error_json_interceptor_invoked.get());
		Assertions.assertTrue(not_acceptable_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").build()).block();
		Assertions.assertEquals(1, not_found_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(0, error_json_interceptor_invoked.get());
		Assertions.assertTrue(not_found_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotAcceptableException(), "/test").accept("application/json").build()).block();
		Assertions.assertEquals(0, not_found_interceptor_invoked.get());
		Assertions.assertEquals(0, error_json_interceptor_invoked.get());
		Assertions.assertTrue(not_acceptable_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new UnsupportedMediaTypeException(), "/test").accept("application/json").build()).block();
		Assertions.assertEquals(0, not_found_interceptor_invoked.get());
		Assertions.assertEquals(1, error_json_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(error_json_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").accept("application/json").build()).block();
		Assertions.assertEquals(1, not_found_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(1, error_json_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(not_found_json_handler_invoked.getAndSet(false));

		Set<ErrorWebRoute<ExchangeContext>> errorRoutes = server.getErrorRoutes();
		Assertions.assertEquals(5, errorRoutes.size());
		for(ErrorWebRoute<ExchangeContext> route : errorRoutes) {
			if(route.getErrorType() == null) {
				Assertions.assertEquals("application/json", route.getProduce());
				Assertions.assertEquals(List.of(notFoundErrorInterceptor, errorJsonInterceptor), route.getInterceptors());
			}
			else if(route.getErrorType().equals(BadRequestException.class)) {
				Assertions.assertTrue(route.getInterceptors().isEmpty());
			}
			else if(route.getErrorType().equals(NotFoundException.class)) {
				switch(route.getProduce()) {
					case "application/json": {
						Assertions.assertEquals(List.of(notFoundErrorInterceptor, errorJsonInterceptor), route.getInterceptors());
						break;
					}
					case null: {
						Assertions.assertEquals(List.of(notFoundErrorInterceptor), route.getInterceptors());
						break;
					}
					default: Assertions.fail("/test_intercept route with unexpected accepted content type: " + route.getConsume());
				}
			}
			else if(route.getErrorType().equals(NotAcceptableException.class)) {
				Assertions.assertTrue(route.getInterceptors().isEmpty());
			}
			else {
				Assertions.fail("Route with unexpected error type: " + route.getErrorType());
			}
		}
	}

	@Test
	public void testRouteError() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean not_found_handler_invoked = new AtomicBoolean();
		AtomicBoolean not_found_json_handler_invoked = new AtomicBoolean();
		server
			.routeError().error(NotFoundException.class).handler(exchange -> not_found_handler_invoked.set(true))
			.routeError().error(NotFoundException.class).produce("application/json").handler(exchange -> not_found_json_handler_invoked.set(true));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").build()).block();
		Assertions.assertTrue(not_found_handler_invoked.getAndSet(false));

		Assertions.assertEquals(Set.of("application/json"), Assertions.assertThrows(NotAcceptableException.class, () -> bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").accept("text/plain").build()).block()).getAcceptableMediaTypes());

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").accept("text/plain,*/*").build()).block();
		Assertions.assertTrue(not_found_handler_invoked.getAndSet(false));

		Assertions.assertThrows(NotFoundException.class, () -> bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").accept("application/json").build()).block());
		Assertions.assertFalse(not_found_json_handler_invoked.get());
	}

	@Test
	public void testWebSocket() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicInteger ws_interceptor_invoked = new AtomicInteger();
		WebSocketExchangeHandler<? super ExchangeContext, Web2SocketExchange<ExchangeContext>> wsHandler1 = wsExchange -> {};
		server
			.intercept().interceptor(exchange -> {
				ws_interceptor_invoked.incrementAndGet();
				return Mono.just(exchange);
			})
			.webSocketRoute().path("/ws").handler(wsHandler1);

		Exchange<ExchangeContext> mockWsExchange1 = MockExchanges.exchangeBuilder(Method.GET, "/ws").build();
		bootServer.defer(mockWsExchange1).block();
		Assertions.assertEquals(1, ws_interceptor_invoked.getAndSet(0));
		Mockito.verify(mockWsExchange1).webSocket();
		Mockito.verify(mockWsExchange1.webSocket().get()).handler(Mockito.any());

		Exchange<ExchangeContext> mockWsExchange2 = MockExchanges.exchangeBuilder(Method.GET, "/ws").subprotocol("json").build();
		bootServer.defer(mockWsExchange2).block();
		Assertions.assertEquals(1, ws_interceptor_invoked.getAndSet(0));
		Mockito.verify(mockWsExchange2).webSocket();
		Mockito.verify(mockWsExchange2.webSocket().get()).handler(Mockito.any());

		WebSocketExchangeHandler<? super ExchangeContext, Web2SocketExchange<ExchangeContext>> wsHandler2 = wsExchange -> {};
		server
			.webSocketRoute().path("/ws").subprotocol("json").handler(wsHandler2);

		Exchange<ExchangeContext> mockWsExchange3 = MockExchanges.exchangeBuilder(Method.GET, "/ws").build();
		bootServer.defer(mockWsExchange3).block();
		Assertions.assertEquals(1, ws_interceptor_invoked.getAndSet(0));
		Mockito.verify(mockWsExchange3).webSocket();
		Mockito.verify(mockWsExchange3.webSocket().get()).handler(Mockito.any());

		Exchange<ExchangeContext> mockWsExchange4 = MockExchanges.exchangeBuilder(Method.GET, "/ws").subprotocol("json").supportedSubProtocol(List.of("json")).build();
		bootServer.defer(mockWsExchange4).block();
		// Route was added on the original server which is not intercepted
		Assertions.assertEquals(0, ws_interceptor_invoked.get());
		Mockito.verify(mockWsExchange4).webSocket("json");
		Mockito.verify(mockWsExchange4.webSocket("json").get()).handler(Mockito.any());
	}

	@Test
	public void testConfigureErrorRoutes() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean not_found_handler_invoked = new AtomicBoolean();
		AtomicBoolean bad_request_intercepted_handler_invoked = new AtomicBoolean();
		AtomicBoolean not_acceptable_handler_invoked = new AtomicBoolean();
		AtomicBoolean bad_request_intercepted_external_handler_invoked = new AtomicBoolean();
		AtomicBoolean method_not_allowed_external_handler_invoked = new AtomicBoolean();
		AtomicInteger bad_request_interceptor_invoked = new AtomicInteger();
		server
			.routeError().error(NotFoundException.class).handler(exchange -> not_found_handler_invoked.set(true))
			.interceptError().error(BadRequestException.class).interceptor(exchange -> {
				bad_request_interceptor_invoked.incrementAndGet();
				return Mono.just(exchange);
			})
			.configureErrorRoutes(errorRoutes -> {
				errorRoutes
					.routeError().error(BadRequestException.class).language("fr").handler(exchange -> bad_request_intercepted_handler_invoked.set(true))
					.routeError().error(NotAcceptableException.class).handler(exchange -> not_acceptable_handler_invoked.set(true));
			})
			.routeError().error(BadRequestException.class).language("en").handler(exchange -> bad_request_intercepted_external_handler_invoked.set(true))
			.routeError().error(MethodNotAllowedException.class).handler(exchange -> method_not_allowed_external_handler_invoked.set(true));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").build()).block();
		Assertions.assertEquals(0, bad_request_interceptor_invoked.get());
		Assertions.assertTrue(not_found_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new BadRequestException(), "/test").acceptLanguage("fr").build()).block();
		Assertions.assertEquals(1, bad_request_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(bad_request_intercepted_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotAcceptableException(), "/test").build()).block();
		Assertions.assertEquals(0, bad_request_interceptor_invoked.get());
		Assertions.assertTrue(not_acceptable_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new BadRequestException(), "/test").acceptLanguage("en").build()).block();
		Assertions.assertEquals(1, bad_request_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(bad_request_intercepted_external_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new MethodNotAllowedException(Set.of(Method.GET)), "/test").build()).block();
		Assertions.assertEquals(0, bad_request_interceptor_invoked.get());
		Assertions.assertTrue(method_not_allowed_external_handler_invoked.getAndSet(false));
	}

	@Test
	public void testConfigureInterceptors() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicInteger test_interceptor_invoked = new AtomicInteger();
		AtomicBoolean test_get_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_external_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
			.configureInterceptors(interceptors -> {
				return interceptors
					.intercept().method(Method.GET).interceptor(exchange -> {
						test_interceptor_invoked.incrementAndGet();
						return Mono.just(exchange);
					});
			})
			.route().path("/test_get").method(Method.GET).handler(exchange -> test_get_handler_invoked.set(true))
			.route().path("/test_external").handler(exchange -> test_external_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_get").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_get_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_external").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test_external").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));
	}

	@Test
	public void testConfigureInterceptors2() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		WebServer<ExchangeContext> interceptor = server
			.intercept().path("/test1").interceptor(exchange -> Mono.just(exchange))
			.configureInterceptors(interceptors -> {
				return interceptors
					.intercept().path("/test2").interceptor(exchange -> Mono.just(exchange))
					.configureInterceptors(interceptors2 -> interceptors2.intercept().path("/test3").interceptor(exchange -> Mono.just(exchange)))
					.intercept().method(Method.GET).interceptor(exchange -> Mono.just(exchange));
			})
			.intercept()
			.path("/test4").interceptor(exchange -> Mono.just(exchange));

		System.out.println("");

	}

	@Test
	public void testConfigureInterceptors_withNonFinalInterceptor() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicInteger test_interceptor_invoked = new AtomicInteger();
		AtomicBoolean test_get_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_external_handler_invoked = new AtomicBoolean();
		server
			.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
			.configureInterceptors(interceptors -> {
				WebRouteInterceptor<ExchangeContext> webRouteInterceptor = interceptors
					.intercept().path("/test_get").interceptor(exchange -> {
						test_interceptor_invoked.incrementAndGet();
						return Mono.just(exchange);
					});

				webRouteInterceptor
					.intercept().method(Method.GET).interceptor(exchange -> {
						test_interceptor_invoked.incrementAndGet();
						return Mono.just(exchange);
					});

				return webRouteInterceptor;
			})
			.route().path("/test_get").method(Method.GET).handler(exchange -> test_get_handler_invoked.set(true))
			.route().path("/test_external").handler(exchange -> test_external_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_get").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_get_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_external").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test_external").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));
	}

	@Test
	public void testConfigureErrorInterceptors() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean not_found_handler_invoked = new AtomicBoolean();
		AtomicBoolean bad_request_intercepted_handler_invoked = new AtomicBoolean();
		AtomicBoolean not_acceptable_handler_invoked = new AtomicBoolean();
		AtomicInteger bad_request_interceptor_invoked = new AtomicInteger();
		server
			.routeError().error(NotFoundException.class).handler(exchange -> not_found_handler_invoked.set(true))
			.configureErrorInterceptors(interceptors -> {
				return interceptors
					.interceptError().error(BadRequestException.class).interceptor(exchange -> {
						bad_request_interceptor_invoked.incrementAndGet();
						return Mono.just(exchange);
					});
			})
			.routeError().error(BadRequestException.class).handler(exchange -> bad_request_intercepted_handler_invoked.set(true))
			.routeError().error(NotAcceptableException.class).handler(exchange -> not_acceptable_handler_invoked.set(true));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").build()).block();
		Assertions.assertEquals(0, bad_request_interceptor_invoked.get());
		Assertions.assertTrue(not_found_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new BadRequestException(), "/test").build()).block();
		Assertions.assertEquals(1, bad_request_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(bad_request_intercepted_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotAcceptableException(), "/test").build()).block();
		Assertions.assertEquals(0, bad_request_interceptor_invoked.get());
		Assertions.assertTrue(not_acceptable_handler_invoked.getAndSet(false));
	}

	@Test
	public void testConfigureErrorInterceptors_withNonFinalInterceptor() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean not_found_handler_invoked = new AtomicBoolean();
		AtomicBoolean bad_request_intercepted_handler_invoked = new AtomicBoolean();
		AtomicBoolean not_acceptable_handler_invoked = new AtomicBoolean();
		AtomicInteger bad_request_interceptor_invoked = new AtomicInteger();
		AtomicInteger not_acceptable_interceptor_invoked = new AtomicInteger();
		server
			.routeError().error(NotFoundException.class).handler(exchange -> not_found_handler_invoked.set(true))
			.configureErrorInterceptors(interceptors -> {
				ErrorWebRouteInterceptor<ExchangeContext> webRouteInterceptor = interceptors
					.interceptError().error(BadRequestException.class).interceptor(exchange -> {
						bad_request_interceptor_invoked.incrementAndGet();
						return Mono.just(exchange);
					});

				webRouteInterceptor.interceptError().error(NotAcceptableException.class).interceptor(exchange -> {
					not_acceptable_interceptor_invoked.incrementAndGet();
					return Mono.just(exchange);
				});

				return webRouteInterceptor;
			})
			.routeError().error(BadRequestException.class).handler(exchange -> bad_request_intercepted_handler_invoked.set(true))
			.routeError().error(NotAcceptableException.class).handler(exchange -> not_acceptable_handler_invoked.set(true));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotFoundException(), "/test").build()).block();
		Assertions.assertEquals(0, bad_request_interceptor_invoked.get());
		Assertions.assertEquals(0, not_acceptable_interceptor_invoked.get());
		Assertions.assertTrue(not_found_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new BadRequestException(), "/test").build()).block();
		Assertions.assertEquals(1, bad_request_interceptor_invoked.getAndSet(0));
		Assertions.assertEquals(0, not_acceptable_interceptor_invoked.get());
		Assertions.assertTrue(bad_request_intercepted_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.errorExchangeBuilder(new NotAcceptableException(), "/test").build()).block();
		Assertions.assertEquals(0, bad_request_interceptor_invoked.get());
		Assertions.assertEquals(0, not_acceptable_interceptor_invoked.get());
		Assertions.assertTrue(not_acceptable_handler_invoked.getAndSet(false));
	}

	@Test
	public void testConfigure() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicInteger test_interceptor_invoked = new AtomicInteger();
		AtomicBoolean test_get_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_external_handler_invoked = new AtomicBoolean();
		server
			.configure(svr -> {
				return svr
					.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
					.intercept().method(Method.GET).interceptor(exchange -> {
						test_interceptor_invoked.incrementAndGet();
						return Mono.just(exchange);
					})
					.route().path("/test_get").method(Method.GET).handler(exchange -> test_get_handler_invoked.set(true));
			})
			.route().path("/test_external").handler(exchange -> test_external_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_get").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_get_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_external").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test_external").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));
	}

	@Test
	public void testConfigure_withUnwrap() {
		GenericWebServerBoot bootServer = new GenericWebServerBoot(null, null, null);
		WebServer<ExchangeContext> server = bootServer.webServer();

		AtomicBoolean test_handler_invoked = new AtomicBoolean();
		AtomicInteger test_interceptor_invoked = new AtomicInteger();
		AtomicBoolean test_get_handler_invoked = new AtomicBoolean();
		AtomicBoolean test_external_handler_invoked = new AtomicBoolean();
		server
			.configure(svr -> {
				return svr
					.route().path("/test").handler(exchange -> test_handler_invoked.set(true))
					.intercept().method(Method.GET).interceptor(exchange -> {
						test_interceptor_invoked.incrementAndGet();
						return Mono.just(exchange);
					})
					.route().path("/test_get").method(Method.GET).handler(exchange -> test_get_handler_invoked.set(true))
					.unwrap();
			})
			.route()
			.path("/test_external")
			.handler(exchange -> test_external_handler_invoked.set(true));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_get").build()).block();
		Assertions.assertEquals(1, test_interceptor_invoked.getAndSet(0));
		Assertions.assertTrue(test_get_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.GET, "/test_external").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));

		bootServer.defer(MockExchanges.exchangeBuilder(Method.POST, "/test_external").build()).block();
		Assertions.assertEquals(0, test_interceptor_invoked.get());
		Assertions.assertTrue(test_external_handler_invoked.getAndSet(false));
	}

	public static class TestContext implements ExchangeContext {

		public String getFoo() {
			return "foo";
		}
	}
}

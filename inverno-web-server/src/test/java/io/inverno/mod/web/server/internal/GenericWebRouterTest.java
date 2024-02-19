/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.internal.mock.MockWebExchange;
import io.inverno.mod.web.server.spi.Route;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericWebRouterTest {
	
	@SuppressWarnings("unchecked")
	private static ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler() {
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler = Mockito.mock(ExchangeHandler.class);
		Mockito.when(mockExchangeHandler.defer(Mockito.any())).thenReturn(Mono.empty());
		return mockExchangeHandler;
	}
	
	@Test
	public void testGetRoutes() {
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.route().consumes(MediaTypes.APPLICATION_JSON).consumes(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").handler(exhange -> {})
			.route().produces(MediaTypes.APPLICATION_JSON).handler(exhange -> {})
			.route().produces(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().path("/hello", true).handler(exchange -> {})
			.route().path("/hello/{param1}").handler(exchange -> {})
			.route().path("/hello/{param1}/{param2:[a-b]*}").handler(exchange -> {});
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		// 1. consume=JSON *
		// 2. consume=HTML *
		// 3. method=GET, language=fr-FR *
		// 4. method=GET, language=en-US *
		// 5. method=POST, language=fr-FR *
		// 6. method=POST, language=en-US *
		// 7. produce=JSON *
		// 8. produce=HTML *
		// 9. path = /hello
		// 10. path = /hello/
		// 11. path = /hello/{param1}
		// 12. path = /hello/{param1}/{param2:[a-b]*}
		
		Assertions.assertEquals(12, routes.size());
		
		GenericWebRoute route1 = new GenericWebRoute(router);
		route1.setConsume(MediaTypes.APPLICATION_JSON);
		
		GenericWebRoute route2 = new GenericWebRoute(router);
		route2.setConsume(MediaTypes.TEXT_HTML);
		
		GenericWebRoute route3 = new GenericWebRoute(router);
		route3.setMethod(Method.GET);
		route3.setLanguage("fr-FR");
		
		GenericWebRoute route4 = new GenericWebRoute(router);
		route4.setMethod(Method.GET);
		route4.setLanguage("en-US");
		
		GenericWebRoute route5 = new GenericWebRoute(router);
		route5.setMethod(Method.POST);
		route5.setLanguage("fr-FR");
		
		GenericWebRoute route6 = new GenericWebRoute(router);
		route6.setMethod(Method.POST);
		route6.setLanguage("en-US");
		
		GenericWebRoute route7 = new GenericWebRoute(router);
		route7.setProduce(MediaTypes.APPLICATION_JSON);
		
		GenericWebRoute route8 = new GenericWebRoute(router);
		route8.setProduce(MediaTypes.TEXT_HTML);
		
		GenericWebRoute route9 = new GenericWebRoute(router);
		route9.setPath("/hello");
		
		GenericWebRoute route10 = new GenericWebRoute(router);
		route10.setPath("/hello/");
		
		GenericWebRoute route11 = new GenericWebRoute(router);
		route11.setPath("/hello/{param1}");
		route11.setPathPattern(URIs.uri("/hello/{param1}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(false));
		
		GenericWebRoute route12 = new GenericWebRoute(router);
		route12.setPath("/hello/{param1}/{param2:[a-b]*}");
		route12.setPathPattern(URIs.uri("/hello/{param1}/{param2:[a-b]*}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(false));
		
		Assertions.assertEquals(Set.of(route1, route2, route3, route4, route5, route6, route7, route8, route9, route10, route11, route12), routes);
	}
	
	@Test
	public void testFindRoutes() {
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.route().consumes(MediaTypes.APPLICATION_JSON).consumes(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").handler(exhange -> {})
			.route().produces(MediaTypes.APPLICATION_JSON).handler(exhange -> {})
			.route().produces(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().path("/hello", true).handler(exchange -> {})
			.route().path("/hello/{param1}").handler(exchange -> {})
			.route().path("/hello/{param1}/{param2:[a-b]*}").handler(exchange -> {});
		
		GenericWebRoute route1 = new GenericWebRoute(router);
		route1.setConsume(MediaTypes.APPLICATION_JSON);
		
		GenericWebRoute route2 = new GenericWebRoute(router);
		route2.setMethod(Method.GET);
		route2.setLanguage("fr-FR");
		
		GenericWebRoute route3 = new GenericWebRoute(router);
		route3.setMethod(Method.POST);
		route3.setLanguage("fr-FR");
		
		Set<WebRoute<ExchangeContext>> routes = router.route().language("fr-FR").findRoutes();
		Assertions.assertEquals(2, routes.size());
		Assertions.assertEquals(Set.of(route2, route3), routes);
		
		routes = router.route().method(Method.GET).language("fr-FR").findRoutes();
		Assertions.assertEquals(1, routes.size());
		Assertions.assertEquals(Set.of(route2), routes);
		
		routes = router.route().method(Method.PUT).language("fr-FR").findRoutes();
		Assertions.assertEquals(0, routes.size());
		
		routes = router.route().consumes(MediaTypes.APPLICATION_JSON).findRoutes();
		Assertions.assertEquals(1, routes.size());
		Assertions.assertEquals(Set.of(route1), routes);
	}
	
	@Test
	public void testRouteRemove() {
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.route().consumes(MediaTypes.APPLICATION_JSON).consumes(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").handler(exhange -> {})
			.route().produces(MediaTypes.APPLICATION_JSON).handler(exhange -> {})
			.route().produces(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().path("/hello", true).handler(exchange -> {})
			.route().path("/hello/{param1}").handler(exchange -> {})
			.route().path("/hello/{param1}/{param2:[a-b]*}").handler(exchange -> {});
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		
		routes = router.route().consumes(MediaTypes.APPLICATION_JSON).findRoutes();
		WebRoute<ExchangeContext> removedRoute = routes.iterator().next();
		removedRoute.remove();
		
		routes = router.getRoutes();
		Assertions.assertEquals(11, routes.size());
	}
	
	@Test
	public void testRouteEnableDisable() {
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.route().consumes(MediaTypes.APPLICATION_JSON).consumes(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").handler(exhange -> {})
			.route().produces(MediaTypes.APPLICATION_JSON).handler(exhange -> {})
			.route().produces(MediaTypes.TEXT_HTML).handler(exhange -> {})
			.route().path("/hello", true).handler(exchange -> {})
			.route().path("/hello/{param1}").handler(exchange -> {})
			.route().path("/hello/{param1}/{param2:[a-b]*}").handler(exchange -> {});
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		
		routes = router.route().consumes(MediaTypes.APPLICATION_JSON).findRoutes();
		WebRoute<ExchangeContext> disabledRoute = routes.iterator().next();
		disabledRoute.disable();
		
		routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		
		Optional<WebRoute<ExchangeContext>> disabledRouteOptional = routes.stream().filter(Route::isDisabled).findFirst();
		Assertions.assertTrue(disabledRouteOptional.isPresent());
		Assertions.assertEquals(disabledRoute, disabledRouteOptional.get());
		
		disabledRoute.enable();
		
		routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		Assertions.assertTrue(routes.stream().noneMatch(Route::isDisabled));
	}
	
	@Test
	public void testMixPathroute() {
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.route()
				.path("/hello", true)
				.path("/hello/{param1}")
				.path("/hello/{param1}/{param2:[a-b]*}")
				.handler(exchange -> {});

		// Should result in 4 routes
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		Assertions.assertEquals(4, routes.size());
	}
	
	@Test
	public void testMixMethodWithSamePath() {
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockHandler1 = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockHandler2 = mockExchangeHandler();
		
		router
			.route()
				.path("/hello", false)
				.method(Method.GET)
				.produces("text/plain")
				.handler(mockHandler1)
			.route()
				.path("/hello", false)
				.method(Method.POST)
				.consumes("text/plain")
				.produces("text/plain")
				.handler(mockHandler2);
		
		MockWebExchange exchange1 = MockWebExchange.from("/hello", Method.GET).build();
		router.defer(exchange1).block();
		Mockito.verify(mockHandler1).defer(Mockito.any());
		
		MockWebExchange exchange2 = MockWebExchange.from("/hello", Method.POST).headers(Map.of("content-type", List.of("text/plain"), "accept", List.of("text/plain"))).build();
		router.defer(exchange2).block();
		Mockito.verify(mockHandler2).defer(Mockito.any());
	}
}

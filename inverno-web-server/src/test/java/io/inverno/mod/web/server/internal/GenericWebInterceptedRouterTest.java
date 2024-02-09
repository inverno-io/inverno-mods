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

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.internal.mock.MockWebExchange;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericWebInterceptedRouterTest {
	
	@SuppressWarnings("unchecked")
	private static ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler() {
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler = Mockito.mock(ExchangeHandler.class);
		Mockito.when(mockExchangeHandler.defer(Mockito.any())).thenReturn(Mono.empty());
		return mockExchangeHandler;
	}
	
	@SuppressWarnings("unchecked")
	private static ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeInterceptor = Mockito.mock(ExchangeInterceptor.class);
		Mockito.when(mockExchangeInterceptor.intercept(Mockito.any())).then(iom -> Mono.just(iom.getArgument(0)));
		return mockExchangeInterceptor;
	}
	
	@Test
	public void testMultiInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_book = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.path("/hello")
				.path("/book/**")
				.interceptor(interceptor)
			.route()
				.path("/book/{id}", false)
				.method(Method.GET)
				.produces("application/json")
				.handler(handler_book);

		MockWebExchange mockExchange = MockWebExchange.from("/book/123").build();
		router.defer(mockExchange).block();
		
		Mockito.verify(interceptor, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_book, Mockito.times(1)).defer(Mockito.any());
	}
	
	@Test
	public void testPathInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_foo = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_bar = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.interceptor(interceptor1)
			.intercept()
				.path("/bar")
				.interceptor(interceptor2)
			.route()
				.path("/foo")
				.handler(handler_foo)
			.route()
				.path("/bar")
				.handler(handler_bar);
		

		MockWebExchange mockExchange = MockWebExchange.from("/foo").build();
		router.defer(mockExchange).block();
		
		// mockExchange is wrapped into a GenericWebExchange in GenericWebRouter#defer()
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_foo, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_bar, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_foo, handler_bar);
		
		mockExchange = MockWebExchange.from("/bar").build();
		router.defer(mockExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_foo, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_bar, Mockito.times(1)).defer(Mockito.any());
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(2, routes.size());

		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			switch (route.getPath()) {
				case "/foo":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				case "/bar":
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
					break;
				default:
					Assertions.fail("Unexpected route: " + route.toString());
					break;
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
	
	@Test
	public void testPathPatternInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_1 = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_2 = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_default = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.interceptor(interceptor1)
			.intercept()
				.path("/a/b/*")
				.interceptor(interceptor2)
			.route()
				.path("/a/*/c")
				.handler(handler_1)
			.route()
				.path("/a/b/d")
				.handler(handler_2)
			.route()
				.handler(handler_default);
		
		MockWebExchange abcExchange = MockWebExchange.from("/a/b/c").build();
		router.defer(abcExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_1, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_2, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_1, handler_2, handler_default);
		
		MockWebExchange abdExchange = MockWebExchange.from("/a/b/d").build();
		router.defer(abdExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_1, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_2, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_1, handler_2, handler_default);
		
		MockWebExchange abeExchange = MockWebExchange.from("/a/b/e").build();
		router.defer(abeExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_1, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_2, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_1, handler_2, handler_default);
		
		MockWebExchange afExchange = MockWebExchange.from("/a/f").build();
		router.defer(afExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_1, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_2, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_1, handler_2, handler_default);
		
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(3, routes.size());

		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			if(route.getPathPattern() == null) {
				if(route.getPath() == null) {
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
				}
				else if(route.getPath().equals("/a/b/d")) {
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
				}
				else {
					Assertions.fail("Unexpected route: " + route.toString());
				}
			}
			else if(route.getPathPattern().getValue().equals("/a/*/c")) {
				Assertions.assertEquals(2, routeInterceptors.size());
				Assertions.assertTrue(routeInterceptors.contains(interceptor1));
				Assertions.assertTrue(routeInterceptors.contains(interceptor2));
			}
			else {
				Assertions.fail("Unexpected route: " + route.toString());
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
	
	@Test
	public void testMethodInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_get = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_post = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_default = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.interceptor(interceptor1)
			.intercept()
				.method(Method.GET)
				.interceptor(interceptor2)
			.route()
				.path("/route")
				.method(Method.GET)
				.handler(handler_get)
			.route()
				.path("/route")
				.method(Method.POST)
				.handler(handler_post)
			.route()
				.handler(handler_default);

		MockWebExchange getRouteExchange = MockWebExchange.from("/route", Method.GET).build();
		router.defer(getRouteExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_get, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_post, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_get, handler_post, handler_default);
		
		MockWebExchange postRouteExchange = MockWebExchange.from("/route", Method.POST).build();
		router.defer(postRouteExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_get, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_post, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_get, handler_post, handler_default);
		
		MockWebExchange putDefaultExchange = MockWebExchange.from("/default", Method.PUT).build();
		router.defer(putDefaultExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_get, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_post, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_get, handler_post, handler_default);
		
		MockWebExchange getDefaultExchange = MockWebExchange.from("/default", Method.GET).build();
		router.defer(getDefaultExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_get, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_post, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_get, handler_post, handler_default);
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(3, routes.size());
		
		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			if(route.getPath() == null) {
				Assertions.assertEquals(2, routeInterceptors.size());
				Assertions.assertTrue(routeInterceptors.contains(interceptor1));
				Assertions.assertTrue(routeInterceptors.contains(interceptor2));
			}
			else if(route.getPath().equals("/route") && route.getMethod() == Method.GET) {
				Assertions.assertEquals(2, routeInterceptors.size());
				Assertions.assertTrue(routeInterceptors.contains(interceptor1));
				Assertions.assertTrue(routeInterceptors.contains(interceptor2));
			}
			else if(route.getPath().equals("/route") && route.getMethod() == Method.POST) {
				Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
			}
			else {
				Assertions.fail("Unexpected route: " + route.toString());
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
	
	@Test
	public void testConsumeInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_json = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_xml = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_default = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.interceptor(interceptor1)
			.intercept()
				.consumes(MediaTypes.APPLICATION_JSON)
				.interceptor(interceptor2)
			.route()
				.path("/json")
				.consumes("*/json")
				.handler(handler_json)
			.route()
				.path("/xml")
				.consumes(MediaTypes.APPLICATION_XML)
				.handler(handler_xml)
			.route()
				.handler(handler_default);
		
		MockWebExchange appJsonExchange = MockWebExchange.from("/json", Method.GET).headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of(MediaTypes.APPLICATION_JSON))).build();
		router.defer(appJsonExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_default);
		
		MockWebExchange dataJsonExchange = MockWebExchange.from("/json", Method.GET).headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("data/json"))).build();
		router.defer(dataJsonExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_default);
		
		MockWebExchange appXmlExchange = MockWebExchange.from("/xml", Method.GET).headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of(MediaTypes.APPLICATION_XML))).build();
		router.defer(appXmlExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_default);
		
		MockWebExchange defTextPlainExchange = MockWebExchange.from("/default", Method.GET).headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of(MediaTypes.TEXT_PLAIN))).build();
		router.defer(defTextPlainExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_default);
		
		MockWebExchange defAppJsonExchange = MockWebExchange.from("/default", Method.GET).headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of(MediaTypes.APPLICATION_JSON))).build();
		router.defer(defAppJsonExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_default, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_default);
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(3, routes.size());
		
		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			if(null == route.getPath()) {
				Assertions.assertEquals(2, routeInterceptors.size());
				Assertions.assertTrue(routeInterceptors.contains(interceptor1));
				Assertions.assertTrue(routeInterceptors.contains(interceptor2));
			}
			else switch (route.getPath()) {
				case "/json":
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
					break;
				case "/xml":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				default:
					Assertions.fail("Unexpected route: " + route.toString());
					break;
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
	
	@Test
	public void testProduceInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_json = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_xml = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_undefined = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.interceptor(interceptor1)
			.intercept()
				.produces("*/json")
				.interceptor(interceptor2)
			.route()
				.path("/json")
				.produces(MediaTypes.APPLICATION_JSON)
				.handler(handler_json)
			.route()
				.path("/xml")
				.produces(MediaTypes.APPLICATION_XML)
				.handler(handler_xml)
			.route()
				.path("/undefined")
				.handler(handler_undefined);
		
		MockWebExchange jsonExchange = MockWebExchange.from("/json", Method.GET).build();
		router.defer(jsonExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_undefined, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_undefined);
		
		MockWebExchange xmlExchange = MockWebExchange.from("/xml", Method.GET).build();
		router.defer(xmlExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_undefined, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_undefined);
		
		MockWebExchange undefinedExchange = MockWebExchange.from("/undefined", Method.GET).build();
		router.defer(undefinedExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_json, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_xml, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_undefined, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_json, handler_xml, handler_undefined);
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(3, routes.size());
		
		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			switch (route.getPath()) {
				case "/json":
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
					break;
				case "/xml":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				case "/undefined":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				default:
					Assertions.fail("Unexpected route: " + route.toString());
					break;
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
	
	@Test
	public void testLanguageInterceptor() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_fr = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_en = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_undefined = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.interceptor(interceptor1)
			.intercept()
				.language("fr")
				.interceptor(interceptor2)
			.route()
				.path("/fr")
				.language("fr-FR")
				.handler(handler_fr)
			.route()
				.path("/en")
				.language("en-EN")
				.handler(handler_en)
			.route()
				.path("/undefined")
				.handler(handler_undefined);
		
		MockWebExchange frExchange = MockWebExchange.from("/fr", Method.GET).build();
		router.defer(frExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_fr, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_en, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_undefined, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_fr, handler_en, handler_undefined);
		
		MockWebExchange enExchange = MockWebExchange.from("/en", Method.GET).build();
		router.defer(enExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_fr, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_en, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_undefined, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_fr, handler_en, handler_undefined);
		
		MockWebExchange undefinedExchange = MockWebExchange.from("/undefined", Method.GET).build();
		router.defer(undefinedExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_fr, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_en, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_undefined, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_fr, handler_en, handler_undefined);
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(3, routes.size());
		
		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			switch (route.getPath()) {
				case "/fr":
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
					break;
				case "/en":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				case "/undefined":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				default:
					Assertions.fail("Unexpected route: " + route.toString());
					break;
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
	
	@Test
	public void testSetInterceptors() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_1 = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler_2 = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.route()
				.path("/1")
				.handler(handler_1)
			.route()
				.path("/2")
				.handler(handler_2);
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(2, routes.size());
		
		Consumer<WebRoute<ExchangeContext>> routeVisitor = route -> {
			switch(route.getPath()) {
				case "/1": 
					Assertions.assertTrue(route.getInterceptors().isEmpty());
					route.setInterceptors(List.of(interceptor1, interceptor2));
					break;
				case "/2":
					Assertions.assertTrue(route.getInterceptors().isEmpty());
					route.setInterceptors(List.of(interceptor1));
					break;
				default:
					Assertions.fail("Unexpected route: " + route.toString());
					break;
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeVisitor.accept(routesIterator.next());
		}
		
		MockWebExchange exchange1 = MockWebExchange.from("/1", Method.GET).build();
		router.defer(exchange1).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler_1, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler_2, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_1, handler_2);
		
		MockWebExchange exchange2 = MockWebExchange.from("/2", Method.GET).build();
		router.defer(exchange2).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler_1, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler_2, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler_1, handler_2);
		
		routes = router.getRoutes();
		
		Assertions.assertEquals(2, routes.size());
		
		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			switch (route.getPath()) {
				case "/1":
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
					break;
				case "/2":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				default:
					Assertions.fail("Unexpected route: " + route.toString());
					break;
			}
		};
		
		routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
	
	@Test
	public void testMultiWrappedInterceptors() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = mockExchangeHandler();
		
		GenericWebRouter router = new GenericWebRouter(null, null, null);
		router
			.intercept()
				.interceptor(interceptor1)
			.intercept()
				.path("/a/b/c")
				.method(Method.GET)
				.interceptor(interceptor2)
			.route()
				.path("/a/*/c")
				.handler(handler);

		MockWebExchange get_abcExchange = MockWebExchange.from("/a/b/c", Method.GET).build();
		router.defer(get_abcExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler);
		
		MockWebExchange post_abcExchange = MockWebExchange.from("/a/b/c", Method.POST).build();
		router.defer(post_abcExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler);
		
		MockWebExchange get_adcExchange = MockWebExchange.from("/a/d/c", Method.GET).build();
		router.defer(get_adcExchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler);
		
		Set<WebRoute<ExchangeContext>> routes = router.getRoutes();
		
		Assertions.assertEquals(1, routes.size());
		
		WebRoute<ExchangeContext> route = routes.iterator().next();
		List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
		Assertions.assertEquals(2, routeInterceptors.size());
		Assertions.assertTrue(routeInterceptors.contains(interceptor1));
		Assertions.assertTrue(routeInterceptors.contains(interceptor2));
	}
	
	@Test
	public void testConfigure() {
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = mockExchangeInterceptor();
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = mockExchangeInterceptor();
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler1 = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler2 = mockExchangeHandler();
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler3 = mockExchangeHandler();
		
		GenericWebRouter genericRouter = new GenericWebRouter(null, null, null);
		
		genericRouter
			.configureInterceptors(interceptable -> {
				interceptable
					.intercept()
						.interceptor(interceptor1);
			})
			.configure(router -> {
				router
					.intercept()
						.interceptor(interceptor2)
					.route()
						.path("/a")
						.handler(handler1);
			})
			.configureRoutes(routable -> {
				routable
					.route()
						.path("/b")
						.handler(handler2);
			})
			.route()
				.path("/c")
				.handler(handler3);
		
		
		MockWebExchange a_exchange = MockWebExchange.from("/a", Method.GET).build();
		genericRouter.defer(a_exchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(handler1, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler2, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler3, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler1, handler2, handler3);
		
		MockWebExchange b_exchange = MockWebExchange.from("/b", Method.GET).build();
		genericRouter.defer(b_exchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler1, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler2, Mockito.times(1)).defer(Mockito.any());
		Mockito.verify(handler3, Mockito.times(0)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler1, handler2, handler3);
		
		MockWebExchange c_exchange = MockWebExchange.from("/c", Method.GET).build();
		genericRouter.defer(c_exchange).block();
		
		Mockito.verify(interceptor1, Mockito.times(1)).intercept(Mockito.any());
		Mockito.verify(interceptor2, Mockito.times(0)).intercept(Mockito.any());
		Mockito.verify(handler1, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler2, Mockito.times(0)).defer(Mockito.any());
		Mockito.verify(handler3, Mockito.times(1)).defer(Mockito.any());
		
		Mockito.clearInvocations(interceptor1, interceptor2, handler1, handler2, handler3);
		
		Set<WebRoute<ExchangeContext>> routes = genericRouter.getRoutes();
		
		Assertions.assertEquals(3, routes.size());
		
		Consumer<WebRoute<ExchangeContext>> routeAssert = route -> {
			List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>> routeInterceptors = route.getInterceptors();
			switch (route.getPath()) {
				case "/a":
					Assertions.assertEquals(2, routeInterceptors.size());
					Assertions.assertTrue(routeInterceptors.contains(interceptor1));
					Assertions.assertTrue(routeInterceptors.contains(interceptor2));
					break;
				case "/b":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				case "/c":
					Assertions.assertEquals(1, routeInterceptors.size());
					Assertions.assertEquals(interceptor1, routeInterceptors.get(0));
					break;
				default:
					Assertions.fail("Unexpected route: " + route.toString());
					break;
			}
		};
		
		Iterator<WebRoute<ExchangeContext>> routesIterator = routes.iterator();
		while(routesIterator.hasNext()) {
			routeAssert.accept(routesIterator.next());
		}
	}
}

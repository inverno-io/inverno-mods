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
package io.winterframework.mod.web.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.winterframework.mod.base.net.URIs;
import io.winterframework.mod.http.base.NotFoundException;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.web.WebExchange;
import io.winterframework.mod.web.WebRoute;
import io.winterframework.mod.web.internal.mock.MockWebExchange;

/**
 * @author jkuhn
 *
 */
public class PathPatternRoutingLinkTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testHandle() {
		List<MockRoutingLink<WebExchange, WebRoute<WebExchange>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<WebExchange, WebRoute<WebExchange>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		PathPatternRoutingLink<WebExchange, WebRoute<WebExchange>> routingLink = new PathPatternRoutingLink<>();
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setPathPattern(URIs.uri("/a/{p1}_{p2}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setPathPattern(URIs.uri("/a/{p}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route2.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setPathPattern(URIs.uri("/a/b_{p}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route3.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setPathPattern(URIs.uri("/a/b/{p}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route4.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route4);
		
		GenericWebRoute route5 = new GenericWebRoute(null);
		route5.setPathPattern(URIs.uri("/a/{p}/c", URIs.Option.PARAMETERIZED).buildPathPattern());
		route5.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route5);
		
		GenericWebRoute route6 = new GenericWebRoute(null);
		route6.setPathPattern(URIs.uri("/a/{p:.*}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route6.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route6);
		
		GenericWebRoute route7 = new GenericWebRoute(null);
		route7.setPathPattern(URIs.uri("/a/{p1}/{p2}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route7.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route7);
		
		Assertions.assertEquals(7, linkRegistry.size());
		
		MockWebExchange exchange1 = MockWebExchange.from("/a/1_2").build();
		routingLink.handle(exchange1);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/a/b_1").build();
		routingLink.handle(exchange2);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/a/b/c").build();
		routingLink.handle(exchange3);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/a/b").build();
		routingLink.handle(exchange4);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/a/2/c").build();
		routingLink.handle(exchange5);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/a/b/c/d").build();
		routingLink.handle(exchange6);
		Mockito.verify(route6.getHandler(), Mockito.times(1)).handle(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/a/1/2").build();
		routingLink.handle(exchange7);
		Mockito.verify(route7.getHandler(), Mockito.times(1)).handle(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/unknown").build();
		try {
			routingLink.handle(exchange8);
			Assertions.fail("Should throw " + NotFoundException.class);
		}
		catch(NotFoundException e) {
		}
		
		MockWebExchange exchange9 = MockWebExchange.from("/a/1/2").build();
		routingLink.handle(exchange9);
		Assertions.assertEquals(Map.of("p1", "1", "p2", "2"), exchange9.request().pathParameters().getAll().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getValue())));
	}
}

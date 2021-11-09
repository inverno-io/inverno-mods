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
package io.inverno.mod.web.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.internal.mock.MockWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class PathPatternRoutingLinkTest {

	@SuppressWarnings("unchecked")
	private static ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler() {
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler = Mockito.mock(ExchangeHandler.class);
		Mockito.when(mockExchangeHandler.defer(Mockito.any())).thenReturn(Mono.empty());
		return mockExchangeHandler;
	}
	
	@Test
	public void testHandle() {
		List<MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		PathPatternRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> routingLink = new PathPatternRoutingLink<>();
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setPathPattern(URIs.uri("/a/{p1}_{p2}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route1.setHandler(mockExchangeHandler());
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setPathPattern(URIs.uri("/a/{p}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route2.setHandler(mockExchangeHandler());
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setPathPattern(URIs.uri("/a/b_{p}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route3.setHandler(mockExchangeHandler());
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setPathPattern(URIs.uri("/a/b/{p}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route4.setHandler(mockExchangeHandler());
		routingLink.setRoute(route4);
		
		GenericWebRoute route5 = new GenericWebRoute(null);
		route5.setPathPattern(URIs.uri("/a/{p}/c", URIs.Option.PARAMETERIZED).buildPathPattern());
		route5.setHandler(mockExchangeHandler());
		routingLink.setRoute(route5);
		
		GenericWebRoute route6 = new GenericWebRoute(null);
		route6.setPathPattern(URIs.uri("/a/{p:.*}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route6.setHandler(mockExchangeHandler());
		routingLink.setRoute(route6);
		
		GenericWebRoute route7 = new GenericWebRoute(null);
		route7.setPathPattern(URIs.uri("/a/{p1}/{p2}", URIs.Option.PARAMETERIZED).buildPathPattern());
		route7.setHandler(mockExchangeHandler());
		routingLink.setRoute(route7);
		
		Assertions.assertEquals(7, linkRegistry.size());
		
		MockWebExchange exchange1 = MockWebExchange.from("/a/1_2").build();
		routingLink.defer(exchange1).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route1.getHandler(), Mockito.times(0))).handle(exchange1);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).defer(exchange1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/a/b_1").build();
		routingLink.defer(exchange2).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange2);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/a/b/c").build();
		routingLink.defer(exchange3).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange3);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/a/b").build();
		routingLink.defer(exchange4).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange4);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/a/2/c").build();
		routingLink.defer(exchange5).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange5);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/a/b/c/d").build();
		routingLink.defer(exchange6).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route6.getHandler(), Mockito.times(0))).handle(exchange6);
		Mockito.verify(route6.getHandler(), Mockito.times(1)).defer(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/a/1/2").build();
		routingLink.defer(exchange7).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route7.getHandler(), Mockito.times(0))).handle(exchange7);
		Mockito.verify(route7.getHandler(), Mockito.times(1)).defer(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/unknown").build();
		try {
			routingLink.defer(exchange8).block();
			Assertions.fail("Should throw " + NotFoundException.class);
		}
		catch(NotFoundException e) {
		}
		
		MockWebExchange exchange9 = MockWebExchange.from("/a/1/2").build();
		routingLink.defer(exchange9).block();
		Assertions.assertEquals(Map.of("p1", "1", "p2", "2"), exchange9.request().pathParameters().getAll().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getValue())));
	}
}

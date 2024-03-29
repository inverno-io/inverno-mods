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
import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebRoute;
import io.inverno.mod.web.server.internal.mock.MockWebExchange;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class ConsumesRoutingLinkTest {

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
		ConsumesRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> routingLink = new ConsumesRoutingLink<>(new AcceptCodec(false));
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setConsume(MediaTypes.APPLICATION_JSON);
		route1.setHandler(mockExchangeHandler());
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setConsume(MediaTypes.TEXT_PLAIN);
		route2.setHandler(mockExchangeHandler());
		routingLink.setRoute(route2);
		
		Assertions.assertEquals(2, linkRegistry.size());
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("application/json"))).build();
		routingLink.defer(exchange1).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route1.getHandler(), Mockito.times(0))).handle(exchange1);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).defer(exchange1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/plain"))).build();
		routingLink.defer(exchange2).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange2);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("application/xml"))).build();
		try {
			routingLink.defer(exchange3).block();
			Assertions.fail("Should throw " + UnsupportedMediaTypeException.class);
		} 
		catch (UnsupportedMediaTypeException e) {
		}
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setConsume("text/plain;version=1");
		route3.setHandler(mockExchangeHandler());
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setConsume("text/plain;version=2");
		route4.setHandler(mockExchangeHandler());
		routingLink.setRoute(route4);
		
		GenericWebRoute route5 = new GenericWebRoute(null);
		route5.setConsume("text/plain;version=2;p=1");
		route5.setHandler(mockExchangeHandler());
		routingLink.setRoute(route5);
		
		Assertions.assertEquals(5, linkRegistry.size());
		
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/plain;version=1"))).build();
		routingLink.defer(exchange4).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange4);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/plain;version=2"))).build();
		routingLink.defer(exchange5).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange5);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/plain;version=3"))).build();
		routingLink.defer(exchange6).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange6);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/plain;version=2;p=1"))).build();
		routingLink.defer(exchange7).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange7);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/plain;p=1"))).build();
		routingLink.defer(exchange8).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange8);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange8);
		
		GenericWebRoute route6 = new GenericWebRoute(null);
		route6.setConsume("text/*;q=0.5");
		route6.setHandler(mockExchangeHandler());
		routingLink.setRoute(route6);
		
		GenericWebRoute route7 = new GenericWebRoute(null);
		route7.setConsume("*/html;q=0.5");
		route7.setHandler(mockExchangeHandler());
		routingLink.setRoute(route7);
		
		MockWebExchange exchange9 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/html"))).build();
		routingLink.defer(exchange9).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route6.getHandler(), Mockito.times(0))).handle(exchange9);
		Mockito.verify(route6.getHandler(), Mockito.times(1)).defer(exchange9);
		
		MockWebExchange exchange10 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("application/html"))).build();
		routingLink.defer(exchange10).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route7.getHandler(), Mockito.times(0))).handle(exchange10);
		Mockito.verify(route7.getHandler(), Mockito.times(1)).defer(exchange10);
		
		GenericWebRoute route8 = new GenericWebRoute(null);
		route8.setConsume("text/*;q=0.5");
		route8.setHandler(mockExchangeHandler());
		routingLink.setRoute(route8);
		
		GenericWebRoute route9 = new GenericWebRoute(null);
		route9.setConsume("*/xml;q=0.7");
		route9.setHandler(mockExchangeHandler());
		routingLink.setRoute(route9);
		
		MockWebExchange exchange11 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_CONTENT_TYPE, List.of("text/xml"))).build();
		routingLink.defer(exchange11).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route9.getHandler(), Mockito.times(0))).handle(exchange11);
		Mockito.verify(route9.getHandler(), Mockito.times(1)).defer(exchange11);
	}
}

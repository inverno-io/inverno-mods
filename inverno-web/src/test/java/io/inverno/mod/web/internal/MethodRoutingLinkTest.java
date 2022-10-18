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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.internal.mock.MockWebExchange;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MethodRoutingLinkTest {

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
		MethodRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> routingLink = new MethodRoutingLink<>();
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route_default = new GenericWebRoute(null);
		
		route_default.setHandler(mockExchangeHandler());
		routingLink.setRoute(route_default);
		
		MockWebExchange exchange1 = MockWebExchange.from("/", Method.PUT).build();
		routingLink.defer(exchange1).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route_default.getHandler(), Mockito.times(0))).handle(exchange1);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).defer(exchange1);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setMethod(Method.GET);
		route1.setHandler(mockExchangeHandler());
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setMethod(Method.POST);
		route2.setHandler(mockExchangeHandler());
		routingLink.setRoute(route2);
		
		Assertions.assertEquals(2, linkRegistry.size());
		
		MockWebExchange exchange2 = MockWebExchange.from("/", Method.GET).build();
		routingLink.defer(exchange2).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route1.getHandler(), Mockito.times(0))).handle(exchange2);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).defer(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/", Method.POST).build();
		routingLink.defer(exchange3).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange3);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/", Method.PUT).build();
		try {
			routingLink.defer(exchange4).block();
			Assertions.fail("Should throw " + MethodNotAllowedException.class);
		} 
		catch (MethodNotAllowedException e) {
			
		}
	}

}

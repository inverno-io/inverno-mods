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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.internal.mock.MockWebExchange;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MethodRoutingLinkTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testHandle() {
		List<MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		MethodRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> routingLink = new MethodRoutingLink<>();
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route_default = new GenericWebRoute(null);
		route_default.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route_default);
		
		MockWebExchange exchange1 = MockWebExchange.from("/", Method.PUT).build();
		routingLink.handle(exchange1);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange1);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setMethod(Method.GET);
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setMethod(Method.POST);
		route2.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route2);
		
		Assertions.assertEquals(2, linkRegistry.size());
		
		MockWebExchange exchange2 = MockWebExchange.from("/", Method.GET).build();
		routingLink.handle(exchange2);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/", Method.POST).build();
		routingLink.handle(exchange3);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/", Method.PUT).build();
		try {
			routingLink.handle(exchange4);
			Assertions.fail("Should throw " + MethodNotAllowedException.class);
		} 
		catch (MethodNotAllowedException e) {
			
		}
	}

}

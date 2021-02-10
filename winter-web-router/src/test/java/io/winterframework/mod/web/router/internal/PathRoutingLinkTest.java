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
package io.winterframework.mod.web.router.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRoute;
import io.winterframework.mod.web.router.mock.MockWebExchange;
import io.winterframework.mod.web.server.ExchangeHandler;

/**
 * @author jkuhn
 *
 */
public class PathRoutingLinkTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testHandle() {
		List<MockRoutingLink<WebExchange, WebRoute<WebExchange>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<WebExchange, WebRoute<WebExchange>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		PathRoutingLink<WebExchange, WebRoute<WebExchange>> routingLink = new PathRoutingLink<>();
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route_default = new GenericWebRoute(null);
		route_default.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route_default);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setPath("/a/b/c");
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setPath("/a/b/c/");
		route2.setHandler(route1.getHandler());
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setPath("/a/b/c/d");
		route3.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route3);
		
		Assertions.assertEquals(3, linkRegistry.size());
		
		MockWebExchange exchange1 = MockWebExchange.from("/a/b/c").build();
		routingLink.handle(exchange1);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/a/b/c/").build();
		routingLink.handle(exchange2);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/a/b/c/d").build();
		routingLink.handle(exchange3);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/unknown").build();
		routingLink.handle(exchange4);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange4);
	}

}

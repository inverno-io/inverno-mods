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
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.internal.mock.MockWebExchange;
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
public class ProducesRoutingLinkTest {

	@SuppressWarnings("unchecked")
	private static ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler() {
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> mockExchangeHandler = Mockito.mock(ExchangeHandler.class);
		Mockito.when(mockExchangeHandler.defer(Mockito.any())).thenReturn(Mono.empty());
		return mockExchangeHandler;
	}
	
	@Test
	public void testHandle_with_default() {
		List<MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		ProducesRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> routingLink = new ProducesRoutingLink<>(new ContentTypeCodec());
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route_default = new GenericWebRoute(null);
		route_default.setHandler(mockExchangeHandler());
		routingLink.setRoute(route_default);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.defer(exchange1).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route_default.getHandler(), Mockito.times(0))).handle(exchange1);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).defer(exchange1);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setProduce("text/plain");
		route1.setHandler(mockExchangeHandler());
		routingLink.setRoute(route1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.defer(exchange2).block();
		// We explicitly set a '*/*' route (ie. route with no produce), so if this is
		// what was requested (accept: */*, we want the default behaviour) we have to
		// comply
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route_default.getHandler(), Mockito.times(0))).handle(exchange2);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).defer(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		try {
			routingLink.defer(exchange3).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setProduce("application/json");
		route2.setHandler(mockExchangeHandler());
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setProduce("application/json;version=1");
		route3.setHandler(mockExchangeHandler());
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setProduce("application/json;version=2");
		route4.setHandler(mockExchangeHandler());
		routingLink.setRoute(route4);
		
		GenericWebRoute route5 = new GenericWebRoute(null);
		route5.setProduce("application/json;version=2;p=1");
		route5.setHandler(mockExchangeHandler());
		routingLink.setRoute(route5);
		
		// Note that when no parameter is defined the best match is considered to be the most one, ie the one with most parameters,
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.defer(exchange4).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange4);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=1"))).build();
		routingLink.defer(exchange5).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange5);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2"))).build();
		routingLink.defer(exchange6).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange6);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2;p=1"))).build();
		routingLink.defer(exchange7).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange7);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;p=1"))).build();
		try {
			routingLink.defer(exchange8).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}

		MockWebExchange exchange9 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json"))).build();
		routingLink.defer(exchange9).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange9);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange9);
		
		MockWebExchange exchange10 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=1"))).build();
		routingLink.defer(exchange10).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange10);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange10);
		
		MockWebExchange exchange11 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2"))).build();
		routingLink.defer(exchange11).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange11);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange11);
		
		MockWebExchange exchange12 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2;p=1"))).build();
		routingLink.defer(exchange12).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange12);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange12);
		
		MockWebExchange exchange13 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;p=1"))).build();
		try {
			routingLink.defer(exchange13).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange14 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*"))).build();
		routingLink.defer(exchange14).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange14);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange14);
		
		MockWebExchange exchange15 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=1"))).build();
		routingLink.defer(exchange15).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange15);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange15);
		
		MockWebExchange exchange16 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2"))).build();
		routingLink.defer(exchange16).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange16);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange16);
		
		MockWebExchange exchange17 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2;p=1"))).build();
		routingLink.defer(exchange17).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange17);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange17);
		
		MockWebExchange exchange18 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;p=1"))).build();
		try {
			routingLink.defer(exchange18).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange19 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.defer(exchange19).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route_default.getHandler(), Mockito.times(0))).handle(exchange19);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).defer(exchange19);
		
		MockWebExchange exchange20 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=1"))).build();
		routingLink.defer(exchange20).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange20);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange20);
		
		MockWebExchange exchange21 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2"))).build();
		routingLink.defer(exchange21).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange21);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange21);
		
		MockWebExchange exchange22 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2;p=1"))).build();
		routingLink.defer(exchange22).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange22);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange22);
		
		MockWebExchange exchange23 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;p=1"))).build();
		try {
			routingLink.defer(exchange23).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
	}
	
	@Test
	public void testHandle_no_default_parameters() {
		List<MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		ProducesRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> routingLink = new ProducesRoutingLink<>(new ContentTypeCodec());
		routingLink.connect(mockRoutingLink);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		try {
			routingLink.defer(exchange1).block();
			Assertions.fail("Should throw " + NotFoundException.class);
		}
		catch (NotFoundException e) {
		}
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setProduce("text/plain");
		route1.setHandler(mockExchangeHandler());
		routingLink.setRoute(route1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.defer(exchange2).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route1.getHandler(), Mockito.times(0))).handle(exchange2);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).defer(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		try {
			routingLink.defer(exchange3).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setProduce("application/json");
		route2.setHandler(mockExchangeHandler());
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setProduce("application/json;version=1");
		route3.setHandler(mockExchangeHandler());
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setProduce("application/json;version=2");
		route4.setHandler(mockExchangeHandler());
		routingLink.setRoute(route4);
		
		GenericWebRoute route5 = new GenericWebRoute(null);
		route5.setProduce("application/json;version=2;p=1");
		route5.setHandler(mockExchangeHandler());
		routingLink.setRoute(route5);
		
		// Note that when no parameter is defined the best match is considered to be the most one, ie the one with most parameters,
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.defer(exchange4).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange4);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=1"))).build();
		routingLink.defer(exchange5).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange5);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2"))).build();
		routingLink.defer(exchange6).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange6);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2;p=1"))).build();
		routingLink.defer(exchange7).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange7);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;p=1"))).build();
		try {
			routingLink.defer(exchange8).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}

		MockWebExchange exchange9 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json"))).build();
		routingLink.defer(exchange9).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange9);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange9);
		
		MockWebExchange exchange10 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=1"))).build();
		routingLink.defer(exchange10).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange10);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange10);
		
		MockWebExchange exchange11 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2"))).build();
		routingLink.defer(exchange11).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange11);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange11);
		
		MockWebExchange exchange12 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2;p=1"))).build();
		routingLink.defer(exchange12).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange12);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange12);
		
		MockWebExchange exchange13 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;p=1"))).build();
		try {
			routingLink.defer(exchange13).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange14 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*"))).build();
		routingLink.defer(exchange14).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange14);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange14);
		
		MockWebExchange exchange15 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=1"))).build();
		routingLink.defer(exchange15).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange15);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange15);
		
		MockWebExchange exchange16 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2"))).build();
		routingLink.defer(exchange16).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange16);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange16);
		
		MockWebExchange exchange17 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2;p=1"))).build();
		routingLink.defer(exchange17).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange17);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange17);
		
		MockWebExchange exchange18 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;p=1"))).build();
		try {
			routingLink.defer(exchange18).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange19 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.defer(exchange19).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange19);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange19);
		
		MockWebExchange exchange20 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=1"))).build();
		routingLink.defer(exchange20).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange20);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange20);
		
		MockWebExchange exchange21 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2"))).build();
		routingLink.defer(exchange21).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route4.getHandler(), Mockito.times(0))).handle(exchange21);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).defer(exchange21);
		
		MockWebExchange exchange22 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2;p=1"))).build();
		routingLink.defer(exchange22).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route5.getHandler(), Mockito.times(0))).handle(exchange22);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).defer(exchange22);
		
		MockWebExchange exchange23 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;p=1"))).build();
		try {
			routingLink.defer(exchange23).block();
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
	}

	@Test
	public void testHandle_quality_no_default() {
		List<MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		ProducesRoutingLink<ExchangeContext, WebExchange<ExchangeContext>, WebRoute<ExchangeContext>> routingLink = new ProducesRoutingLink<>(new ContentTypeCodec());
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setProduce("application/json");
		route1.setHandler(mockExchangeHandler());
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setProduce("text/plain");
		route2.setHandler(mockExchangeHandler());
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setProduce("application/xml;version=1");
		route3.setHandler(mockExchangeHandler());
		routingLink.setRoute(route3);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=1,text/plain;q=0.5"))).build();
		routingLink.defer(exchange1).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route1.getHandler(), Mockito.times(0))).handle(exchange1);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).defer(exchange1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=1,text/*;q=0.5"))).build();
		routingLink.defer(exchange2).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route1.getHandler(), Mockito.times(0))).handle(exchange2);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).defer(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=0.5,text/*;q=1"))).build();
		routingLink.defer(exchange3).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange3);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.defer(exchange4).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route1.getHandler(), Mockito.times(0))).handle(exchange4);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).defer(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("text/plain"))).build();
		routingLink.defer(exchange5).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange5);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=0.5,text/plain;q=1"))).build();
		routingLink.defer(exchange6).block();
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route2.getHandler(), Mockito.times(0))).handle(exchange6);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).defer(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("image/png;q=1,*/*;q=0.5"))).build();
		routingLink.defer(exchange7).block();
		// It's not accepted to specify quality values in produce as they are considered
		// as content types as a result the best match here is the most precise one, in
		// this example the one that specify a parameter.
		// If one wants to specify a default route it has to add a no produce route which will map */* no matter what
		((ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>)Mockito.verify(route3.getHandler(), Mockito.times(0))).handle(exchange7);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).defer(exchange7);
	}
}

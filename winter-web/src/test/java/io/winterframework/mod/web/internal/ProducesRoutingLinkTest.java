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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.winterframework.mod.http.base.NotAcceptableException;
import io.winterframework.mod.http.base.NotFoundException;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.base.internal.header.ContentTypeCodec;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.web.WebExchange;
import io.winterframework.mod.web.WebRoute;
import io.winterframework.mod.web.internal.mock.MockWebExchange;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class ProducesRoutingLinkTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testHandle_with_default() {
		List<MockRoutingLink<WebExchange, WebRoute<WebExchange>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<WebExchange, WebRoute<WebExchange>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		ProducesRoutingLink<WebExchange, WebRoute<WebExchange>> routingLink = new ProducesRoutingLink<>(new ContentTypeCodec());
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route_default = new GenericWebRoute(null);
		route_default.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route_default);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.handle(exchange1);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange1);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setProduce("text/plain");
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.handle(exchange2);
		// We explicitly set a '*/*' route (ie. route with no produce), so if this is
		// what was requested (accept: */*, we want the default behaviour) we have to
		// comply
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		try {
			routingLink.handle(exchange3);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setProduce("application/json");
		route2.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setProduce("application/json;version=1");
		route3.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setProduce("application/json;version=2");
		route4.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route4);
		
		GenericWebRoute route5 = new GenericWebRoute(null);
		route5.setProduce("application/json;version=2;p=1");
		route5.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route5);
		
		// Note that when no parameter is defined the best match is considered to be the most one, ie the one with most parameters,
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.handle(exchange4);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=1"))).build();
		routingLink.handle(exchange5);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2"))).build();
		routingLink.handle(exchange6);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2;p=1"))).build();
		routingLink.handle(exchange7);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;p=1"))).build();
		try {
			routingLink.handle(exchange8);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}

		MockWebExchange exchange9 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json"))).build();
		routingLink.handle(exchange9);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange9);
		
		MockWebExchange exchange10 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=1"))).build();
		routingLink.handle(exchange10);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange10);
		
		MockWebExchange exchange11 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2"))).build();
		routingLink.handle(exchange11);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange11);
		
		MockWebExchange exchange12 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2;p=1"))).build();
		routingLink.handle(exchange12);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange12);
		
		MockWebExchange exchange13 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;p=1"))).build();
		try {
			routingLink.handle(exchange13);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange14 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*"))).build();
		routingLink.handle(exchange14);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange14);
		
		MockWebExchange exchange15 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=1"))).build();
		routingLink.handle(exchange15);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange15);
		
		MockWebExchange exchange16 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2"))).build();
		routingLink.handle(exchange16);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange16);
		
		MockWebExchange exchange17 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2;p=1"))).build();
		routingLink.handle(exchange17);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange17);
		
		MockWebExchange exchange18 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;p=1"))).build();
		try {
			routingLink.handle(exchange18);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange19 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.handle(exchange19);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange19);
		
		MockWebExchange exchange20 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=1"))).build();
		routingLink.handle(exchange20);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange20);
		
		MockWebExchange exchange21 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2"))).build();
		routingLink.handle(exchange21);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange21);
		
		MockWebExchange exchange22 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2;p=1"))).build();
		routingLink.handle(exchange22);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange22);
		
		MockWebExchange exchange23 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;p=1"))).build();
		try {
			routingLink.handle(exchange23);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testHandle_no_default_parameters() {
		List<MockRoutingLink<WebExchange, WebRoute<WebExchange>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<WebExchange, WebRoute<WebExchange>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		ProducesRoutingLink<WebExchange, WebRoute<WebExchange>> routingLink = new ProducesRoutingLink<>(new ContentTypeCodec());
		routingLink.connect(mockRoutingLink);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		try {
			routingLink.handle(exchange1);
			Assertions.fail("Should throw " + NotFoundException.class);
		}
		catch (NotFoundException e) {
		}
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setProduce("text/plain");
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.handle(exchange2);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		try {
			routingLink.handle(exchange3);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setProduce("application/json");
		route2.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setProduce("application/json;version=1");
		route3.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setProduce("application/json;version=2");
		route4.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route4);
		
		GenericWebRoute route5 = new GenericWebRoute(null);
		route5.setProduce("application/json;version=2;p=1");
		route5.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route5);
		
		// Note that when no parameter is defined the best match is considered to be the most one, ie the one with most parameters,
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.handle(exchange4);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=1"))).build();
		routingLink.handle(exchange5);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2"))).build();
		routingLink.handle(exchange6);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;version=2;p=1"))).build();
		routingLink.handle(exchange7);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;p=1"))).build();
		try {
			routingLink.handle(exchange8);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}

		MockWebExchange exchange9 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json"))).build();
		routingLink.handle(exchange9);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange9);
		
		MockWebExchange exchange10 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=1"))).build();
		routingLink.handle(exchange10);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange10);
		
		MockWebExchange exchange11 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2"))).build();
		routingLink.handle(exchange11);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange11);
		
		MockWebExchange exchange12 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;version=2;p=1"))).build();
		routingLink.handle(exchange12);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange12);
		
		MockWebExchange exchange13 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/json;p=1"))).build();
		try {
			routingLink.handle(exchange13);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange14 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*"))).build();
		routingLink.handle(exchange14);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange14);
		
		MockWebExchange exchange15 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=1"))).build();
		routingLink.handle(exchange15);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange15);
		
		MockWebExchange exchange16 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2"))).build();
		routingLink.handle(exchange16);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange16);
		
		MockWebExchange exchange17 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;version=2;p=1"))).build();
		routingLink.handle(exchange17);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange17);
		
		MockWebExchange exchange18 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/*;p=1"))).build();
		try {
			routingLink.handle(exchange18);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
		
		MockWebExchange exchange19 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*"))).build();
		routingLink.handle(exchange19);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange19);
		
		MockWebExchange exchange20 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=1"))).build();
		routingLink.handle(exchange20);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange20);
		
		MockWebExchange exchange21 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2"))).build();
		routingLink.handle(exchange21);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange21);
		
		MockWebExchange exchange22 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;version=2;p=1"))).build();
		routingLink.handle(exchange22);
		Mockito.verify(route5.getHandler(), Mockito.times(1)).handle(exchange22);
		
		MockWebExchange exchange23 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("*/*;p=1"))).build();
		try {
			routingLink.handle(exchange23);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		}
		catch (NotAcceptableException e) {
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandle_quality_no_default() {
		List<MockRoutingLink<WebExchange, WebRoute<WebExchange>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<WebExchange, WebRoute<WebExchange>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		ProducesRoutingLink<WebExchange, WebRoute<WebExchange>> routingLink = new ProducesRoutingLink<>(new ContentTypeCodec());
		routingLink.connect(mockRoutingLink);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setProduce("application/json");
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setProduce("text/plain");
		route2.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setProduce("application/xml;version=1");
		route3.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route3);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=1,text/plain;q=0.5"))).build();
		routingLink.handle(exchange1);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange1);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=1,text/*;q=0.5"))).build();
		routingLink.handle(exchange2);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=0.5,text/*;q=1"))).build();
		routingLink.handle(exchange3);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json"))).build();
		routingLink.handle(exchange4);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("text/plain"))).build();
		routingLink.handle(exchange5);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("application/json;q=0.5,text/plain;q=1"))).build();
		routingLink.handle(exchange6);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT, List.of("image/png;q=1,*/*;q=0.5"))).build();
		routingLink.handle(exchange7);
		// It's not accepted to specify quality values in produce as they are considered
		// as content types as a result the best match here is the most precise one, in
		// this example the one that specify a parameter.
		// If one wants to specify a default route it has to add a no produce route which will map */* no matter what
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange7);
	}
}

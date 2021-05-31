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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.internal.mock.MockWebExchange;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class LanguageRoutingLinkTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testHandle_with_default() {
		List<MockRoutingLink<WebExchange, WebRoute<WebExchange>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<WebExchange, WebRoute<WebExchange>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		LanguageRoutingLink<WebExchange, WebRoute<WebExchange>> routingLink = new LanguageRoutingLink<>(new AcceptLanguageCodec(false));
		routingLink.connect(mockRoutingLink);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("*"))).build();
		try {
			routingLink.handle(exchange1);
			Assertions.fail("Should throw " + NotFoundException.class);
		}
		catch (NotFoundException e) {
		}
		
		GenericWebRoute route_default = new GenericWebRoute(null);
		route_default.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route_default);
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setLanguage("fr-FR");
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setLanguage("en-GB");
		route2.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setLanguage("en-US");
		route3.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setLanguage("en");
		route4.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route4);
		
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("it-IT"))).build();
		routingLink.handle(exchange2);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange2);
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-FR"))).build();
		routingLink.handle(exchange3);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-US"))).build();
		routingLink.handle(exchange4);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU"))).build();
		routingLink.handle(exchange5);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("*"))).build();
		routingLink.handle(exchange6);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr"))).build();
		routingLink.handle(exchange7);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange7);
		
		MockWebExchange exchange8 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-*"))).build();
		try {
			routingLink.handle(exchange8);
			Assertions.fail("Should throw " + NotAcceptableException.class);
		} catch (NotAcceptableException e) {
			Assertions.assertEquals("Invalid language tag: fr-*", e.getMessage());
		}
		
		MockWebExchange exchange9 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-US,en-GB;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange9);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange9);
		
		MockWebExchange exchange10 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-GB;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange10);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange10);
		
		MockWebExchange exchange11 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-ZA;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange11);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange11);
		
		MockWebExchange exchange12 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-GB;q=0.9,en;q=0.8,fr-FR"))).build();
		routingLink.handle(exchange12);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange12);
		
		MockWebExchange exchange13 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-GB;q=0.9,en;q=0.8,fr-FR;q=0.8,fr;q=0.7"))).build();
		routingLink.handle(exchange13);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange13);
		
		MockWebExchange exchange14 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-CA,fr;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange14);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange14);
		
		MockWebExchange exchange15 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-CA,fr;q=0.9"))).build();
		routingLink.handle(exchange15);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange15);
		
		MockWebExchange exchange16 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-CA"))).build();
		routingLink.handle(exchange16);
		Mockito.verify(route_default.getHandler(), Mockito.times(1)).handle(exchange16);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testHandle_no_default() {
		List<MockRoutingLink<WebExchange, WebRoute<WebExchange>>> linkRegistry = new ArrayList<>();
		MockRoutingLink<WebExchange, WebRoute<WebExchange>> mockRoutingLink = new MockRoutingLink<>(linkRegistry);
		LanguageRoutingLink<WebExchange, WebRoute<WebExchange>> routingLink = new LanguageRoutingLink<>(new AcceptLanguageCodec(false));
		routingLink.connect(mockRoutingLink);
		
		MockWebExchange exchange1 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("*"))).build();
		try {
			routingLink.handle(exchange1);
			Assertions.fail("Should throw " + NotFoundException.class);
		}
		catch (NotFoundException e) {
		}
		
		GenericWebRoute route1 = new GenericWebRoute(null);
		route1.setLanguage("fr-FR");
		route1.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route1);
		
		GenericWebRoute route2 = new GenericWebRoute(null);
		route2.setLanguage("en-GB");
		route2.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route2);
		
		GenericWebRoute route3 = new GenericWebRoute(null);
		route3.setLanguage("en-US");
		route3.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route3);
		
		GenericWebRoute route4 = new GenericWebRoute(null);
		route4.setLanguage("en");
		route4.setHandler(Mockito.mock(ExchangeHandler.class));
		routingLink.setRoute(route4);
		
		MockWebExchange exchange2 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("it-IT"))).build();
		try {
			routingLink.handle(exchange2);
			Assertions.fail("Should throw " + NotFoundException.class);
		} 
		catch (NotFoundException e) {
		}
		
		MockWebExchange exchange3 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-FR"))).build();
		routingLink.handle(exchange3);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange3);
		
		MockWebExchange exchange4 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-US"))).build();
		routingLink.handle(exchange4);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange4);
		
		MockWebExchange exchange5 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU"))).build();
		routingLink.handle(exchange5);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange5);
		
		MockWebExchange exchange6 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("*"))).build();
		routingLink.handle(exchange6);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange6);
		
		MockWebExchange exchange7 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr"))).build();
		routingLink.handle(exchange7);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange7);
		
		MockWebExchange exchange9 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-US,en-GB;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange9);
		Mockito.verify(route3.getHandler(), Mockito.times(1)).handle(exchange9);
		
		MockWebExchange exchange10 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-GB;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange10);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange10);
		
		MockWebExchange exchange11 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-ZA;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange11);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange11);
		
		MockWebExchange exchange12 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-GB;q=0.9,en;q=0.8,fr-FR"))).build();
		routingLink.handle(exchange12);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange12);
		
		MockWebExchange exchange13 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("en-AU,en-GB;q=0.9,en;q=0.8,fr-FR;q=0.8,fr;q=0.7"))).build();
		routingLink.handle(exchange13);
		Mockito.verify(route2.getHandler(), Mockito.times(1)).handle(exchange13);
		
		MockWebExchange exchange14 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-CA,fr;q=0.9,en;q=0.8"))).build();
		routingLink.handle(exchange14);
		Mockito.verify(route4.getHandler(), Mockito.times(1)).handle(exchange14);
		
		MockWebExchange exchange15 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-CA,fr;q=0.9"))).build();
		routingLink.handle(exchange15);
		Mockito.verify(route1.getHandler(), Mockito.times(1)).handle(exchange15);
		
		MockWebExchange exchange16 = MockWebExchange.from("/").headers(Map.of(Headers.NAME_ACCEPT_LANGUAGE, List.of("fr-CA"))).build();
		try {
			routingLink.handle(exchange16);
			Assertions.fail("Should throw " + NotFoundException.class);
		} 
		catch (NotFoundException e) {
		}
	}
}

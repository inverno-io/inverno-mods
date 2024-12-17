/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.base.router.link;

import io.inverno.mod.http.base.router.WebSocketSubprotocolRoute;
import io.inverno.mod.http.base.ws.UnsupportedProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WebSocketProtocolRoutingLinkTest {

	@Test
	public void testResolve() {
		WebSocketSubprotocolRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("p1", "r1"));
		link.setRoute(new TestRoute("p2", "r2"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("p1")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("p2")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("unknown", "p2")));
		Assertions.assertEquals(Set.of("p1", "p2"), Assertions.assertThrows(UnsupportedProtocolException.class, () -> link.resolve(new TestInput("unknown"))).getSupportedProtocols());
	}

	@Test
	public void testResolveAll() {
		WebSocketSubprotocolRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("p1", "r1"));
		link.setRoute(new TestRoute("p2", "r2"));

		Assertions.assertEquals(List.of("r1"), new ArrayList<>(link.resolveAll(new TestInput("p1"))));
		Assertions.assertEquals(List.of("r2", "r1"), new ArrayList<>(link.resolveAll(new TestInput("unknown", "p2", "p1"))));
		Assertions.assertEquals(List.of(), new ArrayList<>(link.resolveAll(new TestInput())));
	}

	@Test
	public void testRemoveRoute() {
		WebSocketSubprotocolRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("p1", "r1"));
		link.setRoute(new TestRoute("p2", "r2"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("p1")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("p2")));

		link.removeRoute(new TestRoute("p1"));

		Assertions.assertEquals(Set.of("p2"), Assertions.assertThrows(UnsupportedProtocolException.class, () -> link.resolve(new TestInput("p1"))).getSupportedProtocols());
		Assertions.assertEquals("r2", link.resolve(new TestInput("p2")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(Set.of(new TestRoute("p2", "r2")), extractor.getRoutes());
	}

	@Test
	public void testExtractRoutes() {
		WebSocketSubprotocolRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("p1", "r1"));
		link.setRoute(new TestRoute("p2", "r2"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(Set.of(new TestRoute("p1", "r1"), new TestRoute("p2", "r2")), extractor.getRoutes());
	}

	@Test
	public void testEnableDisableRoute() {
		WebSocketSubprotocolRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("p1", "r1"));
		link.setRoute(new TestRoute("p2", "r2"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("p1")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("p2")));

		link.disableRoute(new TestRoute("p1"));

		Assertions.assertEquals(Set.of("p2"), Assertions.assertThrows(UnsupportedProtocolException.class, () -> link.resolve(new TestInput("p1"))).getSupportedProtocols());
		Assertions.assertEquals("r2", link.resolve(new TestInput("p2")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(2, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getSubprotocol().equals("p1")).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute("p1"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("p1")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("p2")));
	}

	private static class TestRoutingLink extends WebSocketSubprotocolRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected List<String> getSubprotocols(TestInput input) {
			return input.getSubprotocols();
		}
	}

	private static class TestRoute implements WebSocketSubprotocolRoute<String> {

		private final String subprotocol;
		private final String resource;

		private boolean disabled;

		public TestRoute(String subprotocol) {
			this(subprotocol, null);
		}

		public TestRoute(String subprotocol, String resource) {
			this.subprotocol = subprotocol;
			this.resource = resource;
		}

		@Override
		public String getSubprotocol() {
			return this.subprotocol;
		}

		@Override
		public String get() {
			return this.resource;
		}

		@Override
		public void enable() {

		}

		@Override
		public void disable() {

		}

		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}

		@Override
		public boolean isDisabled() {
			return this.disabled;
		}

		@Override
		public void remove() {

		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestRoute testRoute = (TestRoute) o;
			return disabled == testRoute.disabled && Objects.equals(subprotocol, testRoute.subprotocol) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(subprotocol, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements WebSocketSubprotocolRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private String subprotocol;

		private TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor subprotocol(String subprotocol) {
			this.subprotocol = subprotocol;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.subprotocol, resource);
			route.setDisabled(disabled);
			this.routes.add(route);
			this.subprotocol = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final List<String> subprotocols;

		public TestInput(String... subprotocols) {
			this.subprotocols = Arrays.asList(subprotocols);
		}

		public List<String> getSubprotocols() {
			return subprotocols;
		}
	}
}

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

import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class OutboundAcceptContentRoutingLinkTest {

	private static final HeaderCodec<? extends Headers.Accept> ACCEPT_CODEC = new AcceptCodec(true);

	@Test
	public void testResolve() {
		OutboundAcceptContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("text/plain", "r1"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("*/*")));
		Assertions.assertThrows(NotAcceptableException.class, () -> link.resolve(new TestInput("application/xml")));

		link.setRoute(new TestRoute("application/json", "r2"));
		link.setRoute(new TestRoute("application/json;version=1", "r3"));
		link.setRoute(new TestRoute("application/json;version=2", "r4"));
		link.setRoute(new TestRoute("application/json;version=2;p=1", "r5"));

		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("application/json;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("application/json;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json;version=2;p=1")));
		Assertions.assertThrows(NotAcceptableException.class, () -> link.resolve(new TestInput("application/json;p=1")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("*/json")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("*/json;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("*/json;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("*/json;version=2;p=1")));
		Assertions.assertThrows(NotAcceptableException.class, () -> link.resolve(new TestInput("*/json;p=1")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("*/json")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("application/*;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("application/*;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/*;version=2;p=1")));

		Assertions.assertThrows(NotAcceptableException.class, () -> link.resolve(new TestInput("application/*;p=1")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("*/*")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("*/*;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("*/*;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("*/*;version=2;p=1")));

		Assertions.assertThrows(NotAcceptableException.class, () -> link.resolve(new TestInput("*/*;p=1")));
	}

	@Test
	public void testResolveAll() {
		OutboundAcceptContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("text/plain", "r1"));
		link.setRoute(new TestRoute("application/json", "r2"));
		link.setRoute(new TestRoute("application/json;version=1", "r3"));
		link.setRoute(new TestRoute("application/json;version=2", "r4"));
		link.setRoute(new TestRoute("application/json;version=2;p=1", "r5"));
		link.setRoute(new TestRoute("text/html", "r6"));

		Assertions.assertEquals(List.of("r5", "r3", "r4", "r1", "r2", "r6"), new ArrayList<>(link.resolveAll(new TestInput("*/*"))));
		Assertions.assertEquals(List.of("r1", "r6"), new ArrayList<>(link.resolveAll(new TestInput("text/*"))));
		Assertions.assertEquals(List.of("r6"), new ArrayList<>(link.resolveAll(new TestInput("text/html"))));
		Assertions.assertEquals(List.of("r5", "r3", "r4", "r2"), new ArrayList<>(link.resolveAll(new TestInput("application/json"))));
		Assertions.assertEquals(List.of("r4"), new ArrayList<>(link.resolveAll(new TestInput("application/json;version=2"))));
		Assertions.assertEquals(List.of("r5"), new ArrayList<>(link.resolveAll(new TestInput("application/json;version=2;p=1"))));
	}

	@Test
	public void testRemoveRoute() {
		OutboundAcceptContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("text/plain", "r1"));
		link.setRoute(new TestRoute("application/json", "r2"));
		link.setRoute(new TestRoute("application/json;version=1", "r3"));
		link.setRoute(new TestRoute("application/json;version=2", "r4"));
		link.setRoute(new TestRoute("application/json;version=2;p=1", "r5"));
		link.setRoute(new TestRoute("text/html", "r6"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("text/*")));
		// TODO the exact match is ignored when resolving application/json in favor of the most precise route: application/json;version=2;p=1, is this the correct behaviour?
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("application/json;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("application/json;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json;version=2;p=1")));
		Assertions.assertEquals("r6", link.resolve(new TestInput("text/html")));

		link.removeRoute(new TestRoute("text/plain"));

		Assertions.assertEquals("r6", link.resolve(new TestInput("text/*")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("application/json;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("application/json;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json;version=2;p=1")));
		Assertions.assertEquals("r6", link.resolve(new TestInput("text/html")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute("application/json", "r2"),
				new TestRoute("application/json;version=1", "r3"),
				new TestRoute("application/json;version=2", "r4"),
				new TestRoute("application/json;p=1;version=2", "r5"),
				new TestRoute("text/html", "r6")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		OutboundAcceptContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("text/plain", "r1"));
		link.setRoute(new TestRoute("application/json", "r2"));
		link.setRoute(new TestRoute("application/json;version=1", "r3"));
		link.setRoute(new TestRoute("application/json;version=2", "r4"));
		link.setRoute(new TestRoute("application/json;version=2;p=1", "r5"));
		link.setRoute(new TestRoute("text/html", "r6"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute("text/plain", "r1"),
				new TestRoute("application/json", "r2"),
				new TestRoute("application/json;version=1", "r3"),
				new TestRoute("application/json;version=2", "r4"),
				new TestRoute("application/json;p=1;version=2", "r5"),
				new TestRoute("text/html", "r6")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		OutboundAcceptContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("text/plain", "r1"));
		link.setRoute(new TestRoute("application/json", "r2"));
		link.setRoute(new TestRoute("application/json;version=1", "r3"));
		link.setRoute(new TestRoute("application/json;version=2", "r4"));
		link.setRoute(new TestRoute("application/json;version=2;p=1", "r5"));
		link.setRoute(new TestRoute("text/html", "r6"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("text/*")));
		// TODO the exact match is ignored when resolving application/json in favor of the most precise route: application/json;version=2;p=1, is this the correct behaviour?
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("application/json;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("application/json;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json;version=2;p=1")));
		Assertions.assertEquals("r6", link.resolve(new TestInput("text/html")));

		link.disableRoute(new TestRoute("text/plain"));

		Assertions.assertEquals("r6", link.resolve(new TestInput("text/*")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("application/json;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("application/json;version=2")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("application/json;version=2;p=1")));
		Assertions.assertEquals("r6", link.resolve(new TestInput("text/html")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(6, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getAccept().equals("text/plain")).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute("text/plain"));
		Assertions.assertEquals("r1", link.resolve(new TestInput("text/*")));
	}

	private static class TestRoutingLink extends OutboundAcceptContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected List<Headers.Accept> getAllAcceptHeaders(TestInput input) {
			return input.getAllAcceptHeaders();
		}
	}

	private static class TestRoute implements AcceptContentRoute<String> {

		private final String accept;
		private final String resource;

		private boolean disabled;

		public TestRoute(String accept) {
			this(accept, null);
		}

		public TestRoute(String accept, String resource) {
			this.accept = accept;
			this.resource = resource;
		}

		@Override
		public String getAccept() {
			return this.accept;
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
			return disabled == testRoute.disabled && Objects.equals(accept, testRoute.accept) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(accept, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements AcceptContentRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private String accept;

		public TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor accept(String accept) {
			this.accept = accept;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.accept, resource);
			route.setDisabled(disabled);
			this.routes.add(route);
			this.accept = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final Headers.Accept acceptHeader;

		private TestInput(String accept) {
			this.acceptHeader = ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, accept);
		}

		public List<Headers.Accept> getAllAcceptHeaders() {
			return List.of(this.acceptHeader);
		}
	}
}

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

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.router.URIRoute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class URIRoutingLinkTest {

	@Test
	public void testResolve() {
		URIRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestURIRoutingLink();

		link.setRoute(new TestRoute("http://localhost:8080/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("http://localhost:8080/abc/1/d/2", "abc1d2"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("http://localhost:8080/abc")));
		Assertions.assertEquals("abc1d2", link.resolve(new TestInput("http://localhost:8080/abc/1/d/2")));
		Assertions.assertNull(link.resolve(new TestInput("http://localhost:8085/abc/1/d/2")));

		TestInput input = new TestInput("http://localhost:8080/abc/x/d/y");
		Assertions.assertEquals("abcp1dp2", link.resolve(input));
		Assertions.assertEquals(Map.of("p1", "x", "p2", "y"), input.getParameters());
	}

	@Test
	public void testResolveAll() {
		URIRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestURIRoutingLink();

		link.setRoute(new TestRoute("http://localhost:8080/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("http://localhost:8080/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("http://localhost:8080/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "p0"));

		Assertions.assertEquals(List.of("abc1d2", "abcp1dp2"), new ArrayList<>(link.resolveAll(new TestInput("http://localhost:8080/abc/1/d/2"))));
		Assertions.assertEquals(List.of("abc", "p0"), new ArrayList<>(link.resolveAll(new TestInput("http://localhost:8080/abc"))));
		Assertions.assertEquals(List.of(), new ArrayList<>(link.resolveAll(new TestInput(null))));
	}

	@Test
	public void testRemoveRoute() {
		URIRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestURIRoutingLink();

		link.setRoute(new TestRoute("http://localhost:8080/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("http://localhost:8080/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("http://localhost:8080/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "p0"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("http://localhost:8080/abc")));
		Assertions.assertEquals("abc1d2", link.resolve(new TestInput("http://localhost:8080/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/x/d/y")));

		link.removeRoute(new TestRoute("http://localhost:8080/abc"));
		link.removeRoute(new TestRoute("http://localhost:8080/abc/1/d/2"));

		Assertions.assertEquals("p0", link.resolve(new TestInput("http://localhost:8080/abc")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/x/d/y")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(
			Set.of(
				new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "abcp1dp2"),
				new TestRoute("http://localhost:8080/x/y", "xy"),
				new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "p0")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		URIRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestURIRoutingLink();

		link.setRoute(new TestRoute("http://localhost:8080/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("http://localhost:8080/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("http://localhost:8080/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "p0"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(
			Set.of(
				new TestRoute("http://localhost:8080/abc", "abc"),
				new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "abcp1dp2"),
				new TestRoute("http://localhost:8080/abc/1/d/2", "abc1d2"),
				new TestRoute("http://localhost:8080/x/y", "xy"),
				new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "p0")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		URIRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestURIRoutingLink();

		link.setRoute(new TestRoute("http://localhost:8080/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("http://localhost:8080/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("http://localhost:8080/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).scheme("http").host("localhost").port(8080).buildPattern(), "p0"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("http://localhost:8080/abc")));
		Assertions.assertEquals("abc1d2", link.resolve(new TestInput("http://localhost:8080/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/x/d/y")));

		link.disableRoute(new TestRoute("http://localhost:8080/abc"));
		link.disableRoute(new TestRoute("http://localhost:8080/abc/1/d/2"));

		Assertions.assertEquals("p0", link.resolve(new TestInput("http://localhost:8080/abc")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/x/d/y")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(5, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getURI() != null && route.getURI().equals("http://localhost:8080/abc")).findFirst().get().isDisabled());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getURI() != null && route.getURI().equals("http://localhost:8080/abc/1/d/2")).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute("http://localhost:8080/abc"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("http://localhost:8080/abc")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("http://localhost:8080/abc/x/d/y")));
	}

	private static class TestURIRoutingLink extends URIRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected String getNormalizedURI(TestInput input) {
			return input.getUri();
		}

		@Override
		protected void setURIParameters(TestInput input, Map<String, String> parameters) {
			input.setParameters(parameters);
		}
	}

	private static class TestRoute implements URIRoute<String> {

		private final String uri;
		private final URIPattern uriPattern;

		private final String resource;

		private boolean disabled;

		public TestRoute(String uri) {
			this(uri, null);
		}

		public TestRoute(String uri, String resource) {
			this.uri = uri;
			this.uriPattern = null;
			this.resource = resource;
		}

		public TestRoute(URIPattern uriPattern) {
			this(uriPattern, null);
		}

		public TestRoute(URIPattern uriPattern, String resource) {
			this.uri = null;
			this.uriPattern = uriPattern;
			this.resource = resource;
		}

		@Override
		public String getURI() {
			return this.uri;
		}

		@Override
		public URIPattern getURIPattern() {
			return this.uriPattern;
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
			return disabled == testRoute.disabled && Objects.equals(uri, testRoute.uri) && Objects.equals(uriPattern, testRoute.uriPattern) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, uriPattern, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements URIRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private String uri;

		private URIPattern uriPattern;

		private final Set<TestRoute> routes;

		private TestRouteExtractor() {
			this.routes = new HashSet<>();
		}


		@Override
		public TestRouteExtractor uri(String uri) {
			this.uri = uri;
			return this;
		}

		@Override
		public TestRouteExtractor uriPattern(URIPattern uriPattern) {
			this.uriPattern = uriPattern;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route;
			if(this.uri != null) {
				route = new TestRoute(this.uri, resource);
				this.uri = null;
			}
			else {
				route = new TestRoute(this.uriPattern, resource);
				this.uriPattern = null;
			}
			route.setDisabled(disabled);
			this.routes.add(route);
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final String uri;

		private Map<String, String> parameters;

		public TestInput(String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}

		public void setParameters(Map<String, String> parameters) {
			this.parameters = parameters;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}
	}
}

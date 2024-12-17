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
import io.inverno.mod.http.base.router.PathRoute;
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
public class PathRoutingLinkTest {

	@Test
	public void testResolve() {
		PathRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestPathRoutingLink();

		link.setRoute(new TestRoute("/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("/abc/1/d/2", "abc1d2"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("/abc")));
		Assertions.assertEquals("abc1d2", link.resolve(new TestInput("/abc/1/d/2")));
		Assertions.assertNull(link.resolve(new TestInput("/def")));

		TestInput input = new TestInput("/abc/x/d/y");
		Assertions.assertEquals("abcp1dp2", link.resolve(input));
		Assertions.assertEquals(Map.of("p1", "x", "p2", "y"), input.getParameters());
	}

	@Test
	public void testResolveAll() {
		PathRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestPathRoutingLink();

		link.setRoute(new TestRoute("/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "p0"));

		Assertions.assertEquals(List.of("abc1d2", "abcp1dp2"), new ArrayList<>(link.resolveAll(new TestInput("/abc/1/d/2"))));
		Assertions.assertEquals(List.of("abc", "p0"), new ArrayList<>(link.resolveAll(new TestInput("/abc"))));
		Assertions.assertEquals(List.of(), new ArrayList<>(link.resolveAll(new TestInput(null))));
	}

	@Test
	public void testRemoveRoute() {
		PathRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestPathRoutingLink();

		link.setRoute(new TestRoute("/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "p0"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("/abc")));
		Assertions.assertEquals("abc1d2", link.resolve(new TestInput("/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/x/d/y")));

		link.removeRoute(new TestRoute("/abc"));
		link.removeRoute(new TestRoute("/abc/1/d/2"));

		Assertions.assertEquals("p0", link.resolve(new TestInput("/abc")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/x/d/y")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(
			Set.of(
				new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "abcp1dp2"),
				new TestRoute("/x/y", "xy"),
				new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "p0")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		PathRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestPathRoutingLink();

		link.setRoute(new TestRoute("/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "p0"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(
			Set.of(
				new TestRoute("/abc", "abc"),
				new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "abcp1dp2"),
				new TestRoute("/abc/1/d/2", "abc1d2"),
				new TestRoute("/x/y", "xy"),
				new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "p0")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		PathRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestPathRoutingLink();

		link.setRoute(new TestRoute("/abc", "abc"));
		link.setRoute(new TestRoute(URIs.uri("/abc/{p1}/d/{p2}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "abcp1dp2"));
		link.setRoute(new TestRoute("/abc/1/d/2", "abc1d2"));
		link.setRoute(new TestRoute("/x/y", "xy"));
		link.setRoute(new TestRoute(URIs.uri("/{p0}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPathPattern(), "p0"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("/abc")));
		Assertions.assertEquals("abc1d2", link.resolve(new TestInput("/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/x/d/y")));

		link.disableRoute(new TestRoute("/abc"));
		link.disableRoute(new TestRoute("/abc/1/d/2"));

		Assertions.assertEquals("p0", link.resolve(new TestInput("/abc")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/x/d/y")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(5, extractor.getRoutes().size());

		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getPath() != null && route.getPath().equals("/abc")).findFirst().get().isDisabled());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getPath() != null && route.getPath().equals("/abc/1/d/2")).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute("/abc"));

		Assertions.assertEquals("abc", link.resolve(new TestInput("/abc")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/1/d/2")));
		Assertions.assertEquals("abcp1dp2", link.resolve(new TestInput("/abc/x/d/y")));
	}

	private static class TestPathRoutingLink extends PathRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected String getNormalizedPath(TestInput input) {
			return input.getPath();
		}

		@Override
		protected void setPathParameters(TestInput input, Map<String, String> parameters) {
			input.setParameters(parameters);
		}
	}

	private static class TestRoute implements PathRoute<String> {

		private final String path;
		private final URIPattern pathPattern;
		private final String resource;

		private boolean disabled;

		public TestRoute(String path) {
			this(path, null);
		}

		public TestRoute(String path, String resource) {
			this.path = path;
			this.pathPattern = null;
			this.resource = resource;
		}

		public TestRoute(URIPattern pathPattern) {
			this(pathPattern, null);
		}

		public TestRoute(URIPattern pathPattern, String resource) {
			this.path = null;
			this.pathPattern = pathPattern;
			this.resource = resource;
		}

		@Override
		public String getPath() {
			return this.path;
		}

		@Override
		public URIPattern getPathPattern() {
			return this.pathPattern;
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
			return disabled == testRoute.disabled && Objects.equals(path, testRoute.path) && Objects.equals(pathPattern, testRoute.pathPattern) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(path, pathPattern, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements PathRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private String path;

		private URIPattern pathPattern;

		private final Set<TestRoute> routes;

		private TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor path(String path) {
			this.path = path;
			return this;
		}

		@Override
		public TestRouteExtractor pathPattern(URIPattern pathPattern) {
			this.pathPattern = pathPattern;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route;
			if(this.path != null) {
				route = new TestRoute(this.path, resource);
				this.path = null;
			}
			else {
				route = new TestRoute(this.pathPattern, resource);
				this.pathPattern = null;
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

		private final String path;

		private Map<String, String> parameters;

		public TestInput(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}

		public void setParameters(Map<String, String> parameters) {
			this.parameters = parameters;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}
	}
}

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

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.router.MethodRoute;
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
public class MethodRoutingLinkTest {

	@Test
	public void testResolve() {
		MethodRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute(Method.GET, "get"));
		link.setRoute(new TestRoute(Method.POST, "post"));

		Assertions.assertEquals("get", link.resolve(new TestInput(Method.GET)));
		Assertions.assertEquals("post", link.resolve(new TestInput(Method.POST)));
		Assertions.assertEquals(Set.of(Method.GET, Method.POST), Assertions.assertThrows(MethodNotAllowedException.class, () -> link.resolve(new TestInput(Method.DELETE))).getAllowedMethods());
	}

	@Test
	public void testResolveAll() {
		MethodRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute(Method.GET, "get"));
		link.setRoute(new TestRoute(Method.POST, "post"));

		Assertions.assertEquals(List.of("get"), new ArrayList<>(link.resolveAll(new TestInput(Method.GET))));
		Assertions.assertEquals(List.of(), new ArrayList<>(link.resolveAll(new TestInput(null))));
	}

	@Test
	public void testRemoveRoute() {
		MethodRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute(Method.GET, "get"));
		link.setRoute(new TestRoute(Method.POST, "post"));

		Assertions.assertEquals("get", link.resolve(new TestInput(Method.GET)));
		Assertions.assertEquals("post", link.resolve(new TestInput(Method.POST)));

		link.removeRoute(new TestRoute(Method.POST));

		Assertions.assertEquals(Set.of(Method.GET), Assertions.assertThrows(MethodNotAllowedException.class, () -> link.resolve(new TestInput(Method.POST))).getAllowedMethods());

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(Set.of(new TestRoute(Method.GET, "get")), extractor.getRoutes());
	}

	@Test
	public void testExtractRoutes() {
		MethodRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute(Method.GET, "get"));
		link.setRoute(new TestRoute(Method.POST, "post"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(Set.of(new TestRoute(Method.GET, "get"), new TestRoute(Method.POST, "post")), extractor.getRoutes());
	}

	@Test
	public void testEnableDisableRoute() {
		MethodRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute(Method.GET, "get"));
		link.setRoute(new TestRoute(Method.POST, "post"));

		Assertions.assertEquals("get", link.resolve(new TestInput(Method.GET)));
		Assertions.assertEquals("post", link.resolve(new TestInput(Method.POST)));

		link.disableRoute(new TestRoute(Method.GET));

		Assertions.assertEquals(Set.of(Method.POST), Assertions.assertThrows(MethodNotAllowedException.class, () -> link.resolve(new TestInput(Method.GET))).getAllowedMethods());

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(2, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getMethod() == Method.GET).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute(Method.GET));

		Assertions.assertEquals("get", link.resolve(new TestInput(Method.GET)));
	}

	private static class TestMethodRoutingLink extends MethodRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected Method getMethod(TestInput input) {
			return input.getMethod();
		}
	}

	private static class TestRoute implements MethodRoute<String> {

		private final Method method;
		private final String resource;

		private boolean disabled;

		public TestRoute(Method method) {
			this(method, null);
		}

		public TestRoute(Method method, String resource) {
			this.method = method;
			this.resource = resource;
		}

		@Override
		public Method getMethod() {
			return this.method;
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
			return disabled == testRoute.disabled && method == testRoute.method && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(method, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements MethodRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private Method method;

		private TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor method(Method method) {
			this.method = method;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.method, resource);
			route.setDisabled(disabled);
			this.routes.add(route);

			this.method = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final Method method;

		public TestInput(Method method) {
			this.method = method;
		}

		public Method getMethod() {
			return method;
		}
	}
}

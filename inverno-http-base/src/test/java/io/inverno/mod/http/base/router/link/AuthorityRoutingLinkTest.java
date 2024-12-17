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

import io.inverno.mod.http.base.router.AuthorityRoute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class AuthorityRoutingLinkTest {

	@Test
	public void testResolve() {
		AuthorityRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute("localhost:8080", "localhost"));
		link.setRoute(new TestRoute("example.org", "example"));
		link.setRoute(new TestRoute("abc-5", "abc-5"));
		link.setRoute(new TestRoute(Pattern.compile("abc-\\d"), "abc-d"));

		Assertions.assertEquals("localhost", link.resolve(new TestInput("localhost:8080")));
		Assertions.assertEquals("example", link.resolve(new TestInput("example.org")));
		Assertions.assertEquals("abc-5", link.resolve(new TestInput("abc-5")));
		Assertions.assertEquals("abc-d", link.resolve(new TestInput("abc-4")));
		Assertions.assertNull(link.resolve(new TestInput("unknown")));
	}

	@Test
	public void testResolveAll() {
		AuthorityRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute("localhost:8080", "localhost"));
		link.setRoute(new TestRoute("example.org", "example"));
		link.setRoute(new TestRoute("abc-5", "abc-5"));
		link.setRoute(new TestRoute(Pattern.compile("abc-\\d"), "abc-d"));

		Assertions.assertEquals(List.of("localhost"), new ArrayList<>(link.resolveAll(new TestInput("localhost:8080"))));
		Assertions.assertEquals(List.of("abc-5", "abc-d"), new ArrayList<>(link.resolveAll(new TestInput("abc-5"))));
		Assertions.assertEquals(List.of(), new ArrayList<>(link.resolveAll(new TestInput(null))));
	}

	@Test
	public void testRemoveRoute() {
		AuthorityRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute("localhost:8080", "localhost"));
		link.setRoute(new TestRoute("example.org", "example"));
		link.setRoute(new TestRoute("abc-5", "abc-5"));
		link.setRoute(new TestRoute(Pattern.compile("abc-\\d"), "abc-d"));

		link.removeRoute(new TestRoute("example.org"));

		Assertions.assertEquals("localhost", link.resolve(new TestInput("localhost:8080")));
		Assertions.assertNull(link.resolve(new TestInput("example.org")));
		Assertions.assertEquals("abc-5", link.resolve(new TestInput("abc-5")));
		Assertions.assertEquals("abc-d", link.resolve(new TestInput("abc-4")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute("localhost:8080", "localhost"),
				new TestRoute("abc-5", "abc-5"),
				new TestRoute(Pattern.compile("abc-\\d"), "abc-d")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		AuthorityRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute("localhost:8080", "localhost"));
		link.setRoute(new TestRoute("example.org", "example"));
		link.setRoute(new TestRoute(Pattern.compile("abc-\\d"), "abc-d"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute("localhost:8080", "localhost"),
					new TestRoute("example.org", "example"),
				new TestRoute(Pattern.compile("abc-\\d"), "abc-d")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		AuthorityRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestMethodRoutingLink();

		link.setRoute(new TestRoute("localhost:8080", "localhost"));
		link.setRoute(new TestRoute("example.org", "example"));
		link.setRoute(new TestRoute("abc-5", "abc-5"));
		link.setRoute(new TestRoute(Pattern.compile("abc-\\d"), "abc-d"));

		Assertions.assertEquals("localhost", link.resolve(new TestInput("localhost:8080")));
		Assertions.assertEquals("example", link.resolve(new TestInput("example.org")));
		Assertions.assertEquals("abc-5", link.resolve(new TestInput("abc-5")));
		Assertions.assertEquals("abc-d", link.resolve(new TestInput("abc-4")));

		link.disableRoute(new TestRoute("abc-5"));

		Assertions.assertEquals("localhost", link.resolve(new TestInput("localhost:8080")));
		Assertions.assertEquals("example", link.resolve(new TestInput("example.org")));
		Assertions.assertEquals("abc-d", link.resolve(new TestInput("abc-5")));
		Assertions.assertEquals("abc-d", link.resolve(new TestInput("abc-4")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getAuthority() != null && route.getAuthority().equals("abc-5")).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute("abc-5"));
		link.disableRoute(new TestRoute(Pattern.compile("abc-\\d")));

		Assertions.assertEquals("localhost", link.resolve(new TestInput("localhost:8080")));
		Assertions.assertEquals("example", link.resolve(new TestInput("example.org")));
		Assertions.assertEquals("abc-5", link.resolve(new TestInput("abc-5")));
		Assertions.assertNull(link.resolve(new TestInput("abc-4")));
	}

	private static class TestMethodRoutingLink extends AuthorityRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected String getAuthority(TestInput input) {
			return input.getAuthority();
		}
	}

	private static class TestRoute implements AuthorityRoute<String> {

		private final String authority;
		private final Pattern authorityPattern;
		private final String resource;

		private boolean disabled;

		public TestRoute(String authority) {
			this(authority, null);
		}

		public TestRoute(String authority, String resource) {
			this.authority = authority;
			this.authorityPattern = null;
			this.resource = resource;
		}

		public TestRoute(Pattern authorityPattern) {
			this(authorityPattern, null);
		}

		public TestRoute(Pattern authorityPattern, String resource) {
			this.authority = null;
			this.authorityPattern = authorityPattern;
			this.resource = resource;
		}

		@Override
		public String getAuthority() {
			return this.authority;
		}

		@Override
		public Pattern getAuthorityPattern() {
			return this.authorityPattern;
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
			return disabled == testRoute.disabled && Objects.equals(authority, testRoute.authority) && Objects.equals(authorityPattern != null ? authorityPattern.pattern() : null, testRoute.authorityPattern != null ? testRoute.authorityPattern.pattern() : null) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(authority, authorityPattern != null ? authorityPattern.pattern() : null, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements AuthorityRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private String authority;
		private Pattern authorityPattern;

		public TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor authority(String authority) {
			this.authority = authority;
			return this;
		}

		@Override
		public TestRouteExtractor authorityPattern(Pattern authorityPattern) {
			this.authorityPattern = authorityPattern;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route;
			if(this.authority != null) {
				route = new TestRoute(this.authority, resource);
				this.authority = null;
			}
			else {
				route = new TestRoute(this.authorityPattern, resource);
				this.authorityPattern = null;
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

		private final String authority;

		public TestInput(String authority) {
			this.authority = authority;
		}

		public String getAuthority() {
			return authority;
		}
	}
}

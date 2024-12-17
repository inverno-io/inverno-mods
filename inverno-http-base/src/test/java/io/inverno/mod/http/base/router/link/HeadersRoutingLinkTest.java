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

import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.router.HeadersRoute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class HeadersRoutingLinkTest {

	@Test
	public void testResolve() {
		HeadersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"h2", new HeadersRoute.HeaderMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null),
				"h2", new HeadersRoute.HeaderMatcher(Set.of("b"), null),
				"h3", new HeadersRoute.HeaderMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("h1", List.of("a")))));
		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("h1", List.of("x", "y", "a")))));
		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b")))));

		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("_"), "h2", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("_"), "h2", List.of("b")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("5"), "h2", List.of("b")))));

		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b"), "h3", List.of("c")))));

		Assertions.assertNull(link.resolve(new TestInput(Map.of("h1", List.of("x"), "h2", List.of("b"), "h3", List.of("c")))));
		Assertions.assertNull(link.resolve(new TestInput(Map.of())));
	}

	@Test
	public void testResolveAll() {
		HeadersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"h2", new HeadersRoute.HeaderMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null),
				"h2", new HeadersRoute.HeaderMatcher(Set.of("b"), null),
				"h3", new HeadersRoute.HeaderMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals(List.of("r1"), new ArrayList<>(link.resolveAll(new TestInput(Map.of("h1", List.of("a"))))));
		Assertions.assertEquals(List.of("r3", "r1"), new ArrayList<>(link.resolveAll(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b"), "h3", List.of("c"))))));
	}

	@Test
	public void testRemoveRoute() {
		HeadersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"h2", new HeadersRoute.HeaderMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null),
				"h2", new HeadersRoute.HeaderMatcher(Set.of("b"), null),
				"h3", new HeadersRoute.HeaderMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("h1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("_"), "h2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b"), "h3", List.of("c")))));

		link.removeRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			)
		));

		Assertions.assertNull(link.resolve(new TestInput(Map.of("h1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("_"), "h2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b"), "h3", List.of("c")))));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute(
					Map.of(
						"h1", new HeadersRoute.HeaderMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
						"h2", new HeadersRoute.HeaderMatcher(null, Set.of(Pattern.compile("a|b")))
					),
					"r2"
				),
				new TestRoute(
					Map.of(
						"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null),
						"h2", new HeadersRoute.HeaderMatcher(Set.of("b"), null),
						"h3", new HeadersRoute.HeaderMatcher(Set.of("c"), null)
					),
					"r3"
				)
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		HeadersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"h2", new HeadersRoute.HeaderMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null),
				"h2", new HeadersRoute.HeaderMatcher(Set.of("b"), null),
				"h3", new HeadersRoute.HeaderMatcher(Set.of("c"), null)
			),
			"r3"
		));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute(
					Map.of(
						"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
					),
					"r1"
				),
				new TestRoute(
					Map.of(
						"h1", new HeadersRoute.HeaderMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
						"h2", new HeadersRoute.HeaderMatcher(null, Set.of(Pattern.compile("a|b")))
					),
					"r2"
				),
				new TestRoute(
					Map.of(
						"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null),
						"h2", new HeadersRoute.HeaderMatcher(Set.of("b"), null),
						"h3", new HeadersRoute.HeaderMatcher(Set.of("c"), null)
					),
					"r3"
				)
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		HeadersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"h2", new HeadersRoute.HeaderMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null),
				"h2", new HeadersRoute.HeaderMatcher(Set.of("b"), null),
				"h3", new HeadersRoute.HeaderMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("h1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("_"), "h2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b"), "h3", List.of("c")))));

		link.disableRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			)
		));

		Assertions.assertNull(link.resolve(new TestInput(Map.of("h1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("_"), "h2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b"), "h3", List.of("c")))));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(3, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getHeadersMatchers().keySet().equals(Set.of("h1"))).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute(
			Map.of(
				"h1", new HeadersRoute.HeaderMatcher(Set.of("a"), null)
			)
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("h1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("h1", List.of("_"), "h2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("h1", List.of("a"), "h2", List.of("b"), "h3", List.of("c")))));
	}

	private static class TestRoutingLink extends HeadersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected InboundHeaders getHeaders(TestInput input) {
			return input.getHeaders();
		}
	}

	private static class TestRoute implements HeadersRoute<String> {

		private final Map<String, HeaderMatcher> matchers;
		private final String resource;

		private boolean disabled;

		public TestRoute(Map<String, HeaderMatcher> headerMatchers) {
			this(headerMatchers, null);
		}

		public TestRoute(Map<String, HeaderMatcher> headerMatchers, String resource) {
			this.matchers = headerMatchers;
			this.resource = resource;
		}

		@Override
		public Map<String, HeaderMatcher> getHeadersMatchers() {
			return this.matchers;
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
			return disabled == testRoute.disabled && Objects.equals(matchers, testRoute.matchers) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(matchers, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements HeadersRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private Map<String, HeadersRoute.HeaderMatcher> headerMatchers;

		public TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor headersMatchers(Map<String, HeadersRoute.HeaderMatcher> headersMatchers) {
			this.headerMatchers = headersMatchers;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.headerMatchers, resource);
			route.setDisabled(disabled);
			this.routes.add(route);
			this.headerMatchers = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final Map<String, List<String>> headers;

		public TestInput(Map<String, List<String>> headers) {
			this.headers = headers;
		}

		public InboundHeaders getHeaders() {
			return new InboundHeaders() {
				@Override
				public boolean contains(CharSequence name) {
					return false;
				}

				@Override
				public boolean contains(CharSequence name, CharSequence value) {
					return false;
				}

				@Override
				public Set<String> getNames() {
					return Set.of();
				}

				@Override
				public Optional<String> get(CharSequence name) {
					return Optional.empty();
				}

				@Override
				public List<String> getAll(CharSequence name) {
					return TestInput.this.headers.getOrDefault(name, List.of());
				}

				@Override
				public List<Map.Entry<String, String>> getAll() {
					return List.of();
				}

				@Override
				public Optional<Parameter> getParameter(CharSequence name) {
					return Optional.empty();
				}

				@Override
				public List<Parameter> getAllParameter(CharSequence name) {
					return List.of();
				}

				@Override
				public List<Parameter> getAllParameter() {
					return List.of();
				}

				@Override
				public <T extends Header> Optional<T> getHeader(CharSequence name) {
					return Optional.empty();
				}

				@Override
				public <T extends Header> List<T> getAllHeader(CharSequence name) {
					return List.of();
				}

				@Override
				public List<Header> getAllHeader() {
					return List.of();
				}
			};
		}
	}
}

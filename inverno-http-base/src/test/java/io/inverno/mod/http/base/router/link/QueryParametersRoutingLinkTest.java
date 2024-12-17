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

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.router.QueryParametersRoute;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class QueryParametersRoutingLinkTest {

	@Test
	public void testResolve() {
		QueryParametersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"p2", new QueryParametersRoute.ParameterMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null),
				"p2", new QueryParametersRoute.ParameterMatcher(Set.of("b"), null),
				"p3", new QueryParametersRoute.ParameterMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("p1", List.of("a")))));
		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("p1", List.of("x", "y", "a")))));
		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b")))));

		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("_"), "p2", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("_"), "p2", List.of("b")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("5"), "p2", List.of("b")))));

		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b"), "p3", List.of("c")))));

		Assertions.assertNull(link.resolve(new TestInput(Map.of("p1", List.of("x"), "p2", List.of("b"), "p3", List.of("c")))));
		Assertions.assertNull(link.resolve(new TestInput(Map.of())));
	}

	@Test
	public void testResolveAll() {
		QueryParametersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"p2", new QueryParametersRoute.ParameterMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null),
				"p2", new QueryParametersRoute.ParameterMatcher(Set.of("b"), null),
				"p3", new QueryParametersRoute.ParameterMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals(List.of("r1"), new ArrayList<>(link.resolveAll(new TestInput(Map.of("p1", List.of("a"))))));
		Assertions.assertEquals(List.of("r3", "r1"), new ArrayList<>(link.resolveAll(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b"), "p3", List.of("c"))))));
	}

	@Test
	public void testRemoveRoute() {
		QueryParametersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"p2", new QueryParametersRoute.ParameterMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null),
				"p2", new QueryParametersRoute.ParameterMatcher(Set.of("b"), null),
				"p3", new QueryParametersRoute.ParameterMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("p1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("_"), "p2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b"), "p3", List.of("c")))));

		link.removeRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			)
		));

		Assertions.assertNull(link.resolve(new TestInput(Map.of("p1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("_"), "p2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b"), "p3", List.of("c")))));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute(
					Map.of(
						"p1", new QueryParametersRoute.ParameterMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
						"p2", new QueryParametersRoute.ParameterMatcher(null, Set.of(Pattern.compile("a|b")))
					),
					"r2"
				),
				new TestRoute(
					Map.of(
						"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null),
						"p2", new QueryParametersRoute.ParameterMatcher(Set.of("b"), null),
						"p3", new QueryParametersRoute.ParameterMatcher(Set.of("c"), null)
					),
					"r3"
				)
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		QueryParametersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"p2", new QueryParametersRoute.ParameterMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null),
				"p2", new QueryParametersRoute.ParameterMatcher(Set.of("b"), null),
				"p3", new QueryParametersRoute.ParameterMatcher(Set.of("c"), null)
			),
			"r3"
		));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute(
					Map.of(
						"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
					),
					"r1"
				),
				new TestRoute(
					Map.of(
						"p1", new QueryParametersRoute.ParameterMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
						"p2", new QueryParametersRoute.ParameterMatcher(null, Set.of(Pattern.compile("a|b")))
					),
					"r2"
				),
				new TestRoute(
					Map.of(
						"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null),
						"p2", new QueryParametersRoute.ParameterMatcher(Set.of("b"), null),
						"p3", new QueryParametersRoute.ParameterMatcher(Set.of("c"), null)
					),
					"r3"
				)
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		QueryParametersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			),
			"r1"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("_"), Set.of(Pattern.compile("\\d"))),
				"p2", new QueryParametersRoute.ParameterMatcher(null, Set.of(Pattern.compile("a|b")))
			),
			"r2"
		));
		link.setRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null),
				"p2", new QueryParametersRoute.ParameterMatcher(Set.of("b"), null),
				"p3", new QueryParametersRoute.ParameterMatcher(Set.of("c"), null)
			),
			"r3"
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("p1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("_"), "p2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b"), "p3", List.of("c")))));

		link.disableRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			)
		));

		Assertions.assertNull(link.resolve(new TestInput(Map.of("p1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("_"), "p2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b"), "p3", List.of("c")))));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(3, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getQueryParameterMatchers().keySet().equals(Set.of("p1"))).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute(
			Map.of(
				"p1", new QueryParametersRoute.ParameterMatcher(Set.of("a"), null)
			)
		));

		Assertions.assertEquals("r1", link.resolve(new TestInput(Map.of("p1", List.of("a")))));
		Assertions.assertEquals("r2", link.resolve(new TestInput(Map.of("p1", List.of("_"), "p2", List.of("a")))));
		Assertions.assertEquals("r3", link.resolve(new TestInput(Map.of("p1", List.of("a"), "p2", List.of("b"), "p3", List.of("c")))));
	}

	private static class TestRoutingLink extends QueryParametersRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected QueryParameters getQueryParameters(TestInput input) {
			return input.getQueryParameters();
		}
	}

	private static class TestRoute implements QueryParametersRoute<String> {

		private final Map<String, ParameterMatcher> matchers;
		private final String resource;

		private boolean disabled;

		public TestRoute(Map<String, ParameterMatcher> headerMatchers) {
			this(headerMatchers, null);
		}

		public TestRoute(Map<String, ParameterMatcher> headerMatchers, String resource) {
			this.matchers = headerMatchers;
			this.resource = resource;
		}

		@Override
		public Map<String, ParameterMatcher> getQueryParameterMatchers() {
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

	private static class TestRouteExtractor implements QueryParametersRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private Map<String, QueryParametersRoute.ParameterMatcher> matchers;

		public TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor queryParametersMatchers(Map<String, QueryParametersRoute.ParameterMatcher> queryParametersMatchers) {
			this.matchers = queryParametersMatchers;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.matchers, resource);
			route.setDisabled(disabled);
			this.routes.add(route);
			this.matchers = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final Map<String, List<String>> parameters;

		public TestInput(Map<String, List<String>> parameters) {
			this.parameters = parameters;
		}

		private QueryParameters getQueryParameters() {
			return new QueryParameters() {
				@Override
				public boolean contains(String name) {
					return false;
				}

				@Override
				public Set<String> getNames() {
					return Set.of();
				}

				@Override
				public Optional<Parameter> get(String name) {
					return Optional.empty();
				}

				@Override
				public List<Parameter> getAll(String name) {
					if(!TestInput.this.parameters.containsKey(name)) {
						return List.of();
					}
					return TestInput.this.parameters.get(name).stream()
						.map(value -> new Parameter() {
							@Override
							public String getName() {
								return name;
							}

							@Override
							public String getValue() {
								return value;
							}

							@Override
							public <T> T as(Class<T> type) {
								return null;
							}

							@Override
							public <T> T as(Type type) {
								return null;
							}

							@Override
							public <T> T[] asArrayOf(Class<T> type) {
								return null;
							}

							@Override
							public <T> T[] asArrayOf(Type type) {
								return null;
							}

							@Override
							public <T> List<T> asListOf(Class<T> type) {
								return List.of();
							}

							@Override
							public <T> List<T> asListOf(Type type) {
								return List.of();
							}

							@Override
							public <T> Set<T> asSetOf(Class<T> type) {
								return Set.of();
							}

							@Override
							public <T> Set<T> asSetOf(Type type) {
								return Set.of();
							}

							@Override
							public Byte asByte() {
								return 0;
							}

							@Override
							public Short asShort() {
								return 0;
							}

							@Override
							public Integer asInteger() {
								return 0;
							}

							@Override
							public Long asLong() {
								return 0l;
							}

							@Override
							public Float asFloat() {
								return 0f;
							}

							@Override
							public Double asDouble() {
								return 0.0;
							}

							@Override
							public Character asCharacter() {
								return null;
							}

							@Override
							public String asString() {
								return "";
							}

							@Override
							public Boolean asBoolean() {
								return null;
							}

							@Override
							public BigInteger asBigInteger() {
								return null;
							}

							@Override
							public BigDecimal asBigDecimal() {
								return null;
							}

							@Override
							public LocalDate asLocalDate() {
								return null;
							}

							@Override
							public LocalDateTime asLocalDateTime() {
								return null;
							}

							@Override
							public ZonedDateTime asZonedDateTime() {
								return null;
							}

							@Override
							public Currency asCurrency() {
								return null;
							}

							@Override
							public Locale asLocale() {
								return null;
							}

							@Override
							public File asFile() {
								return null;
							}

							@Override
							public Path asPath() {
								return null;
							}

							@Override
							public URI asURI() {
								return null;
							}

							@Override
							public URL asURL() {
								return null;
							}

							@Override
							public Pattern asPattern() {
								return null;
							}

							@Override
							public InetAddress asInetAddress() {
								return null;
							}

							@Override
							public Class<?> asClass() {
								return null;
							}
						})
						.collect(Collectors.toUnmodifiableList());
				}

				@Override
				public Map<String, List<Parameter>> getAll() {
					return Map.of();
				}
			};
		}
	}
}

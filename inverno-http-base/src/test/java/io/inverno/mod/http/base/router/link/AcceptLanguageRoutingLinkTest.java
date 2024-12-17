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

import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
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
public class AcceptLanguageRoutingLinkTest {

	private static final HeaderCodec<? extends Headers.AcceptLanguage> ACCEPT_LANGUAGE_CODEC = new AcceptLanguageCodec(true);

	@Test
	public void testResolve() {
		AcceptLanguageRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("fr-FR", "r1"));
		link.setRoute(new TestRoute("en-GB", "r2"));
		link.setRoute(new TestRoute("en-US", "r3"));
		link.setRoute(new TestRoute("en", "r4"));

		Assertions.assertNull(link.resolve(new TestInput("it-IT")));
		Assertions.assertEquals("r1", link.resolve(new TestInput("fr-FR")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("en-US")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU")));
		Assertions.assertEquals("r1", link.resolve(new TestInput("*")));
		Assertions.assertEquals("r1", link.resolve(new TestInput("fr")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("en-US,en-GB;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("en-AU,en-GB;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU,en-ZA;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("en-AU,en-GB;q=0.9,en;q=0.8,fr-FR;q=0.8,fr;q=0.7")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("fr-CA,fr;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r1", link.resolve(new TestInput("fr-CA,fr;q=0.9")));
		Assertions.assertNull(link.resolve(new TestInput("fr-CA")));

		link = new TestRoutingLink();
		link.setRoute(new TestRoute("fr-FR", "r1"));
		link.setRoute(new TestRoute("en-GB", "r2"));
		Assertions.assertEquals("r1", link.resolve(new TestInput("*")));

		link = new TestRoutingLink();
		link.setRoute(new TestRoute("en-GB", "r2"));
		link.setRoute(new TestRoute("fr-FR", "r1"));
		Assertions.assertEquals("r2", link.resolve(new TestInput("*")));
	}

	@Test
	public void testResolveAll() {
		AcceptLanguageRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("fr-FR", "r1"));
		link.setRoute(new TestRoute("en-GB", "r2"));
		link.setRoute(new TestRoute("en-US", "r3"));
		link.setRoute(new TestRoute("en", "r4"));

		Assertions.assertEquals(List.of("r1"), new ArrayList<>(link.resolveAll(new TestInput("fr"))));
		Assertions.assertEquals(List.of(), new ArrayList<>(link.resolveAll(new TestInput("fr-CA"))));
		Assertions.assertEquals(List.of("r1"), new ArrayList<>(link.resolveAll(new TestInput("fr-CA,fr;q=0.9"))));
		Assertions.assertEquals(List.of("r2", "r4", "r1", "r3"), new ArrayList<>(link.resolveAll(new TestInput("fr-CA,fr;q=0.9,en-GB;q=0.8,en;q=0.7"))));
		Assertions.assertEquals(List.of("r4", "r2", "r3"), new ArrayList<>(link.resolveAll(new TestInput("en"))));
	}

	@Test
	public void testRemoveRoute() {
		AcceptLanguageRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("fr-FR", "r1"));
		link.setRoute(new TestRoute("en-GB", "r2"));
		link.setRoute(new TestRoute("en-US", "r3"));
		link.setRoute(new TestRoute("en", "r4"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("fr-FR")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("en-AU,en-GB;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("en-US")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU")));

		link.removeRoute(new TestRoute("en-GB"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("fr-FR")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU,en-GB;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("en-US")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute("fr-FR", "r1"),
				new TestRoute("en-US", "r3"),
				new TestRoute("en", "r4")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		AcceptLanguageRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("fr-FR", "r1"));
		link.setRoute(new TestRoute("en-GB", "r2"));
		link.setRoute(new TestRoute("en-US", "r3"));
		link.setRoute(new TestRoute("en", "r4"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute("fr-FR", "r1"),
				new TestRoute("en-GB", "r2"),
				new TestRoute("en-US", "r3"),
				new TestRoute("en", "r4")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		AcceptLanguageRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute("fr-FR", "r1"));
		link.setRoute(new TestRoute("en-GB", "r2"));
		link.setRoute(new TestRoute("en-US", "r3"));
		link.setRoute(new TestRoute("en", "r4"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("fr-FR")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("en-AU,en-GB;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("en-US")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU")));

		link.disableRoute(new TestRoute("en-GB"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("fr-FR")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU,en-GB;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("en-US")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(4, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getLanguage().equals("en-GB")).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute("en-GB"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("fr-FR")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("en-AU,en-GB;q=0.9,en;q=0.8")));
		Assertions.assertEquals("r3", link.resolve(new TestInput("en-US")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("en-AU")));
	}

	private static class TestRoutingLink extends AcceptLanguageRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(TestInput input) {
			return input.getAllAcceptLanguageHeaders();
		}
	}

	private static class TestRoute implements AcceptLanguageRoute<String> {

		private final String language;
		private final String resource;

		private boolean disabled;

		public TestRoute(String language) {
			this(language, null);
		}

		public TestRoute(String language, String resource) {
			this.language = language;
			this.resource = resource;
		}

		@Override
		public String getLanguage() {
			return this.language;
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
			return disabled == testRoute.disabled && Objects.equals(language, testRoute.language) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(language, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements AcceptLanguageRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private String language;

		public TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor language(String language) {
			this.language = language;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.language, resource);
			route.setDisabled(disabled);
			this.routes.add(route);
			this.language = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final Headers.AcceptLanguage acceptLanguageHeader;

		public TestInput(String acceptLanguage) {
			this.acceptLanguageHeader = ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, acceptLanguage);
		}

		public List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders() {
			return List.of(acceptLanguageHeader);
		}
	}
}

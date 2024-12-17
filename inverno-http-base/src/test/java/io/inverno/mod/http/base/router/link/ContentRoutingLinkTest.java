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

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.router.ContentRoute;
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
public class ContentRoutingLinkTest {

	private static final HeaderCodec<? extends Headers.ContentType> CONTENT_TYPE_CODEC = new ContentTypeCodec();

	@Test
	public void testResolve() {
		ContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(MediaTypes.APPLICATION_JSON, "r1"));
		link.setRoute(new TestRoute(MediaTypes.TEXT_PLAIN, "r2"));

		Assertions.assertEquals("r1", link.resolve(new TestInput("application/json")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("text/plain")));
		Assertions.assertThrows(UnsupportedMediaTypeException.class, () -> link.resolve(new TestInput("application/xml")));

		link.setRoute(new TestRoute("text/plain;version=1", "r3"));
		link.setRoute(new TestRoute("text/plain;version=2", "r4"));
		link.setRoute(new TestRoute("text/plain;version=2;p=1", "r5"));

		Assertions.assertEquals("r3", link.resolve(new TestInput("text/plain;version=1")));
		Assertions.assertEquals("r4", link.resolve(new TestInput("text/plain;version=2")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("text/plain;version=3")));
		Assertions.assertEquals("r5", link.resolve(new TestInput("text/plain;version=2;p=1")));
		Assertions.assertEquals("r2", link.resolve(new TestInput("text/plain;p=1")));

		link.setRoute(new TestRoute("text/*;q=0.5", "r6"));
		link.setRoute(new TestRoute("*/html;q=0.5", "r7"));

		Assertions.assertEquals("r6", link.resolve(new TestInput("text/html")));
		Assertions.assertEquals("r7", link.resolve(new TestInput("application/html")));

		link.setRoute(new TestRoute("text/*;q=0.5", "r8"));
		link.setRoute(new TestRoute("*/xml;q=0.7", "r9"));

		Assertions.assertEquals("r8", link.resolve(new TestInput("text/html")));
		Assertions.assertEquals("r9", link.resolve(new TestInput("text/xml")));
	}

	@Test
	public void testResolveAll() {
		ContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(MediaTypes.APPLICATION_JSON, "r1"));
		link.setRoute(new TestRoute(MediaTypes.TEXT_PLAIN, "r2"));
		link.setRoute(new TestRoute("text/plain;version=1", "r3"));
		link.setRoute(new TestRoute("text/plain;version=2", "r4"));
		link.setRoute(new TestRoute("text/plain;version=2;p=1", "r5"));
		link.setRoute(new TestRoute("text/*;q=0.5", "r6"));
		link.setRoute(new TestRoute("*/html;q=0.5", "r7"));
		link.setRoute(new TestRoute("text/*;q=0.5", "r8"));
		link.setRoute(new TestRoute("*/xml;q=0.7", "r9"));

		Assertions.assertEquals(List.of("r1"), new ArrayList<>(link.resolveAll(new TestInput("application/json"))));
		Assertions.assertEquals(List.of("r2", "r8"), new ArrayList<>(link.resolveAll(new TestInput("text/plain"))));
		Assertions.assertEquals(List.of("r4", "r2", "r8"), new ArrayList<>(link.resolveAll(new TestInput("text/plain;version=2"))));
	}

	@Test
	public void testRemoveRoute() {
		ContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(MediaTypes.TEXT_PLAIN, "r2"));
		link.setRoute(new TestRoute("text/plain;version=1", "r3"));
		link.setRoute(new TestRoute("text/plain;version=2", "r4"));
		link.setRoute(new TestRoute("text/plain;version=2;p=1", "r5"));

		Assertions.assertEquals("r3", link.resolve(new TestInput("text/plain;version=1")));

		link.removeRoute(new TestRoute("text/plain;version=1"));

		Assertions.assertEquals("r2", link.resolve(new TestInput("text/plain;version=1")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute(MediaTypes.TEXT_PLAIN, "r2"),
				new TestRoute("text/plain;version=2", "r4"),
				new TestRoute("text/plain;version=2;p=1", "r5")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testExtractRoutes() {
		ContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(MediaTypes.TEXT_PLAIN, "r2"));
		link.setRoute(new TestRoute("text/plain;version=1", "r3"));
		link.setRoute(new TestRoute("text/plain;version=2", "r4"));
		link.setRoute(new TestRoute("text/plain;version=2;p=1", "r5"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);

		Assertions.assertEquals(
			Set.of(
				new TestRoute(MediaTypes.TEXT_PLAIN, "r2"),
				new TestRoute("text/plain;version=1", "r3"),
				new TestRoute("text/plain;version=2", "r4"),
				new TestRoute("text/plain;version=2;p=1", "r5")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		ContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(MediaTypes.TEXT_PLAIN, "r2"));
		link.setRoute(new TestRoute("text/plain;version=1", "r3"));
		link.setRoute(new TestRoute("text/plain;version=2", "r4"));
		link.setRoute(new TestRoute("text/plain;version=2;p=1", "r5"));

		Assertions.assertEquals("r3", link.resolve(new TestInput("text/plain;version=1")));

		link.disableRoute(new TestRoute("text/plain;version=1"));

		Assertions.assertEquals("r2", link.resolve(new TestInput("text/plain;version=1")));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(4, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getContentType().equals("text/plain;version=1")).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute("text/plain;version=1"));

		Assertions.assertEquals("r3", link.resolve(new TestInput("text/plain;version=1")));
	}

	private static class TestRoutingLink extends ContentRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected Headers.ContentType getContentTypeHeader(TestInput input) {
			return input.getContentTypeHeader();
		}
	}

	private static class TestRoute implements ContentRoute<String> {

		private final String contentType;
		private final String resource;

		private boolean disabled;

		public TestRoute(String contentType) {
			this(contentType, null);
		}

		public TestRoute(String contentType, String resource) {
			this.contentType = contentType;
			this.resource = resource;
		}

		@Override
		public String getContentType() {
			return this.contentType;
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
			return disabled == testRoute.disabled && Objects.equals(contentType, testRoute.contentType) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(contentType, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements ContentRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private String contentType;

		public TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor contentType(String contentType) {
			this.contentType = contentType;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.contentType, resource);
			route.setDisabled(disabled);
			this.routes.add(route);
			this.contentType = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final Headers.ContentType contentTypeHeader;

		public TestInput(String contentType) {
			this.contentTypeHeader = CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, contentType);
		}

		public Headers.ContentType getContentTypeHeader() {
			return contentTypeHeader;
		}
	}
}

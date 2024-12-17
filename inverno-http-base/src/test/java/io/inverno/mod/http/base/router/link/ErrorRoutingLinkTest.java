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

import io.inverno.mod.http.base.router.ErrorRoute;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
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
public class ErrorRoutingLinkTest {

	@Test
	public void testResolve() {
		ErrorRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(Exception.class, "exception"));
		link.setRoute(new TestRoute(IOException.class, "io"));
		link.setRoute(new TestRoute(FileNotFoundException.class, "file"));

		Assertions.assertEquals("file", link.resolve(new TestInput(new FileNotFoundException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new IOException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new EOFException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new RuntimeException())));
	}

	@Test
	public void testResolveAll() {
		ErrorRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(Exception.class, "exception"));
		link.setRoute(new TestRoute(IOException.class, "io"));
		link.setRoute(new TestRoute(FileNotFoundException.class, "file"));

		Assertions.assertEquals(List.of("file", "io", "exception"), new ArrayList<>(link.resolveAll(new TestInput(new FileNotFoundException()))));
		Assertions.assertEquals(List.of("io", "exception"), new ArrayList<>(link.resolveAll(new TestInput(new IOException()))));
		Assertions.assertEquals(List.of(), new ArrayList<>(link.resolveAll(new TestInput(null))));
	}

	@Test
	public void testRemoveRoute() {
		ErrorRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(Exception.class, "exception"));
		link.setRoute(new TestRoute(IOException.class, "io"));
		link.setRoute(new TestRoute(FileNotFoundException.class, "file"));

		Assertions.assertEquals("file", link.resolve(new TestInput(new FileNotFoundException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new IOException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new EOFException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new RuntimeException())));

		link.removeRoute(new TestRoute(IOException.class, "io"));

		Assertions.assertEquals("file", link.resolve(new TestInput(new FileNotFoundException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new IOException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new EOFException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new RuntimeException())));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(Set.of(new TestRoute(Exception.class, "exception"), new TestRoute(FileNotFoundException.class, "file")), extractor.getRoutes());
	}

	@Test
	public void testExtractRoutes() {
		ErrorRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(Exception.class, "exception"));
		link.setRoute(new TestRoute(IOException.class, "io"));
		link.setRoute(new TestRoute(FileNotFoundException.class, "file"));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(
			Set.of(
				new TestRoute(Exception.class, "exception"),
				new TestRoute(IOException.class, "io"),
				new TestRoute(FileNotFoundException.class, "file")
			),
			extractor.getRoutes()
		);
	}

	@Test
	public void testEnableDisableRoute() {
		ErrorRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = new TestRoutingLink();

		link.setRoute(new TestRoute(Exception.class, "exception"));
		link.setRoute(new TestRoute(IOException.class, "io"));
		link.setRoute(new TestRoute(FileNotFoundException.class, "file"));

		Assertions.assertEquals("file", link.resolve(new TestInput(new FileNotFoundException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new IOException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new EOFException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new RuntimeException())));

		link.disableRoute(new TestRoute(IOException.class, "io"));

		Assertions.assertEquals("file", link.resolve(new TestInput(new FileNotFoundException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new IOException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new EOFException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new RuntimeException())));

		TestRouteExtractor extractor = new TestRouteExtractor();
		link.extractRoutes(extractor);
		Assertions.assertEquals(3, extractor.getRoutes().size());
		Assertions.assertTrue(extractor.getRoutes().stream().filter(route -> route.getErrorType().equals(IOException.class)).findFirst().get().isDisabled());

		link.enableRoute(new TestRoute(IOException.class, "io"));

		Assertions.assertEquals("file", link.resolve(new TestInput(new FileNotFoundException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new IOException())));
		Assertions.assertEquals("io", link.resolve(new TestInput(new EOFException())));
		Assertions.assertEquals("exception", link.resolve(new TestInput(new RuntimeException())));
	}

	private static class TestRoutingLink extends ErrorRoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		@Override
		protected Throwable getError(TestInput input) {
			return input.getError();
		}
	}

	private static class TestRoute implements ErrorRoute<String> {

		private final Class<? extends Throwable> errorType;
		private final String resource;

		private boolean disabled;

		public TestRoute(Class<? extends Throwable> error) {
			this(error, null);
		}

		public TestRoute(Class<? extends Throwable> error, String resource) {
			this.errorType = error;
			this.resource = resource;
		}

		@Override
		public Class<? extends Throwable> getErrorType() {
			return this.errorType;
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
			return disabled == testRoute.disabled && Objects.equals(errorType, testRoute.errorType) && Objects.equals(resource, testRoute.resource);
		}

		@Override
		public int hashCode() {
			return Objects.hash(errorType, resource, disabled);
		}
	}

	private static class TestRouteExtractor implements ErrorRoute.Extractor<String, TestRoute, TestRouteExtractor> {

		private final Set<TestRoute> routes;

		private Class<? extends Throwable> errorType;

		public TestRouteExtractor() {
			this.routes = new HashSet<>();
		}

		@Override
		public TestRouteExtractor errorType(Class<? extends Throwable> errorType) {
			this.errorType = errorType;
			return this;
		}

		@Override
		public void set(String resource, boolean disabled) {
			TestRoute route = new TestRoute(this.errorType, resource);
			route.setDisabled(disabled);
			this.routes.add(route);
			this.errorType = null;
		}

		@Override
		public Set<TestRoute> getRoutes() {
			return this.routes;
		}
	}

	private static class TestInput {

		private final Throwable error;

		public TestInput(Throwable error) {
			this.error= error;
		}

		public Throwable getError() {
			return error;
		}
	}
}

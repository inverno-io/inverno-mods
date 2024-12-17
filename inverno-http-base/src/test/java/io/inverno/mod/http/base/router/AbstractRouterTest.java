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
package io.inverno.mod.http.base.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class AbstractRouterTest {

	@Test
	public void testSetRoute() {
		String[][] routes = new String[][] {
			{null, null, null, "resource1"},
			{"a", null, null, "resource2"},
			{null, "b", null, "resource3"},
			{null, null, "c", "resource4"},
			{"a", "b", null, "resource5"},
			{"a", null, "c", "resource6"},
			{null, "b", "c", "resource7"},
			{"a", "b", "c", "resource8"}
		};

		permutations(routes).forEach(rts -> {
			TestRouter router = new TestRouter();

			rts.forEach(route -> router.setRoute(new TestRoute(router, route[0], route[1], route[2], route[3])));

			Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, null)));
			Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", null)));
			Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "z")));
			Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", null)));
			Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, "z")));
			Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "z")));
			Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "z")));

			Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
			Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
			Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

			Assertions.assertEquals("resource3", router.resolve(new TestInput("x", "b", "z")));
			Assertions.assertEquals("resource3", router.resolve(new TestInput(null, "b", "z")));
			Assertions.assertEquals("resource3", router.resolve(new TestInput(null, "b", null)));

			Assertions.assertEquals("resource4", router.resolve(new TestInput("x", "y", "c")));
			Assertions.assertEquals("resource4", router.resolve(new TestInput(null, "y", "c")));
			Assertions.assertEquals("resource4", router.resolve(new TestInput(null, null, "c")));

			Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "z")));
			Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", null)));

			Assertions.assertEquals("resource6", router.resolve(new TestInput("a", "y", "c")));
			Assertions.assertEquals("resource6", router.resolve(new TestInput("a", null, "c")));

			Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
			Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

			Assertions.assertEquals("resource8", router.resolve(new TestInput("a", "b", "c")));
		});
	}

	@Test
	public void testResolve() {
		TestRouter router = new TestRouter();

		router.setRoute(new TestRoute(router, "a", null, null, "a"));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "b", "c")));
	}

	@Test
	public void testResolveAll() {
		// I need to resolve all resources that matches: /a/b/c is matched by /, /a, /a/b, /a//c, /a/b/c, //b, //b/c and ///c
		TestRouter router = new TestRouter();

		TestRoute[] routes = new TestRoute[] {
			new TestRoute(router, null, null, null, "resource1"),
			new TestRoute(router, "a", null, null, "resource2"),
			new TestRoute(router, null, "b", null, "resource3"),
			new TestRoute(router, null, null, "c", "resource4"),
			new TestRoute(router, "a", "b", null, "resource5"),
			new TestRoute(router, "a", null, "c", "resource6"),
			new TestRoute(router, null, "b", "c", "resource7"),
			new TestRoute(router, "a", "b", "c", "resource8")
		};

		Arrays.stream(routes).forEach(router::setRoute);

		Assertions.assertEquals(
			List.of(
				// Ordered from the most precise to the most general, previous link takes precedence over next link (i.e. A > B > C)
				"resource8", // /a/b/c
				"resource5", // /a/b/_
				"resource6", // /a/_/c
				"resource2", // /a/_/_
				"resource7", // /_/b/c
				"resource3", // /_/b/_
				"resource4", // /_/_/c
				"resource1"  // /_/_/_
			),
			new ArrayList<>(router.resolveAll(new TestInput("a", "b", "c")))
		);

		router.removeRoute(new TestRoute(router, null, "b", null));
		router.removeRoute(new TestRoute(router, null, null, "c"));
		router.removeRoute(new TestRoute(router, "a", null, "c"));

		router.setRoute(new TestRoute(router, "a", "y", null, "resource9"));
		router.setRoute(new TestRoute(router, null, "y", "c", "resource10"));
		router.setRoute(new TestRoute(router, null, "b", "z", "resource11"));
		router.setRoute(new TestRoute(router, "a", "b", "z", "resource12"));
		router.setRoute(new TestRoute(router, "x", "b", "c", "resource13"));
		router.setRoute(new TestRoute(router, "a", null, "z", "resource14"));

		Assertions.assertEquals(
			List.of(
				"resource8", // /a/b/c
				"resource5", // /a/b/_
				"resource2", // /a/_/_
				"resource7", // /_/b/c
				"resource1"  // /_/_/_
			),
			new ArrayList<>(router.resolveAll(new TestInput("a", "b", "c")))
		);

		String b = router.resolve(new TestInput(null, "b", null));

		Assertions.assertEquals(
			List.of(
				"resource8",  // /a/b/c
				"resource12",  // /a/b/z
				"resource5",  // /a/b/_
				"resource14", // /a/_/z
				"resource2",  // /a/_/_
				"resource13", // /x/b/c
				"resource7",  // /_/b/c
				"resource11", // /_/b/z
				"resource1"   // /_/_/_
			),
			new ArrayList<>(router.resolveAll(new TestInput(null, "b", null)))
		);
	}

	@Test
	public void testRemoveRoute() {
		TestRouter router = new TestRouter();

		TestRoute[] routes = new TestRoute[] {
			new TestRoute(router, null, null, null, "resource1"),
			new TestRoute(router, "a", null, null, "resource2"),
			new TestRoute(router, null, "b", null, "resource3"),
			new TestRoute(router, null, null, "c", "resource4"),
			new TestRoute(router, "a", "b", null, "resource5"),
			new TestRoute(router, "a", null, "c", "resource6"),
			new TestRoute(router, null, "b", "c", "resource7"),
			new TestRoute(router, "a", "b", "c", "resource8")
		};

		Arrays.stream(routes).forEach(router::setRoute);

		router.removeRoute(new TestRoute(router, "x", "y", "z", "missing"));
		router.removeRoute(new TestRoute(router, null, "y", "z", "missing"));
		router.removeRoute(new TestRoute(router, null, null, "z", "missing"));
		router.removeRoute(new TestRoute(router, "x", "y", null, "missing"));
		router.removeRoute(new TestRoute(router, "x", null, "z", "missing"));
		router.removeRoute(new TestRoute(router, "x", null, null, "missing"));
		router.removeRoute(new TestRoute(router, null, "y", null, "missing"));

		// when it doesn't match it removes all...

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

		Assertions.assertEquals("resource3", router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertEquals("resource3", router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertEquals("resource3", router.resolve(new TestInput(null, "b", null)));

		Assertions.assertEquals("resource4", router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertEquals("resource4", router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertEquals("resource4", router.resolve(new TestInput(null, null, "c")));

		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", null)));

		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource8", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, "a", "b", "c"));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

		Assertions.assertEquals("resource3", router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertEquals("resource3", router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertEquals("resource3", router.resolve(new TestInput(null, "b", null)));

		Assertions.assertEquals("resource4", router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertEquals("resource4", router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertEquals("resource4", router.resolve(new TestInput(null, null, "c")));

		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", null)));

		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, null, "b", null));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", null)));

		Assertions.assertEquals("resource4", router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertEquals("resource4", router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertEquals("resource4", router.resolve(new TestInput(null, null, "c")));

		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", null)));

		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, null, null, "c"));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", null)));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "c")));

		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", null)));

		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource5", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router,"a", "b", null));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", null)));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "c")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", null)));

		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource6", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, "a", null, "c"));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", null)));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", null, "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "b", null)));

		Assertions.assertEquals("resource1", router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertEquals("resource1", router.resolve(new TestInput(null, null, "c")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", null)));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, null, null, null));

		Assertions.assertNull(router.resolve(new TestInput("x", null, null)));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", null)));
		Assertions.assertNull(router.resolve(new TestInput(null, null, "z")));
		Assertions.assertNull(router.resolve(new TestInput("x", "y", null)));
		Assertions.assertNull(router.resolve(new TestInput("x", null, "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertNull(router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, null)));

		Assertions.assertNull(router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "b", null)));

		Assertions.assertNull(router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput(null, null, "c")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", null)));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource2", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, "a", null, null));

		Assertions.assertNull(router.resolve(new TestInput("x", null, null)));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", null)));
		Assertions.assertNull(router.resolve(new TestInput(null, null, "z")));
		Assertions.assertNull(router.resolve(new TestInput("x", "y", null)));
		Assertions.assertNull(router.resolve(new TestInput("x", null, "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertNull(router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertNull(router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertNull(router.resolve(new TestInput("a", "y", null)));
		Assertions.assertNull(router.resolve(new TestInput("a", null, null)));

		Assertions.assertNull(router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "b", null)));

		Assertions.assertNull(router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput(null, null, "c")));

		Assertions.assertNull(router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput("a", "b", null)));

		Assertions.assertNull(router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput("a", null, "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertEquals("resource7", router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertEquals("resource7", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, null, "b", "c"));

		Assertions.assertNull(router.resolve(new TestInput("x", null, null)));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", null)));
		Assertions.assertNull(router.resolve(new TestInput(null, null, "z")));
		Assertions.assertNull(router.resolve(new TestInput("x", "y", null)));
		Assertions.assertNull(router.resolve(new TestInput("x", null, "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", "z")));
		Assertions.assertNull(router.resolve(new TestInput("x", "y", "z")));

		Assertions.assertNull(router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertNull(router.resolve(new TestInput("a", "y", null)));
		Assertions.assertNull(router.resolve(new TestInput("a", null, null)));

		Assertions.assertNull(router.resolve(new TestInput("x", "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput(null, "b", null)));

		Assertions.assertNull(router.resolve(new TestInput("x", "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput(null, "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput(null, null, "c")));

		Assertions.assertNull(router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertNull(router.resolve(new TestInput("a", "b", null)));

		Assertions.assertNull(router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertNull(router.resolve(new TestInput("a", null, "c")));

		Assertions.assertNull(router.resolve(new TestInput("x", "b", "c")));
		Assertions.assertNull(router.resolve(new TestInput(null, "b", "c")));

		Assertions.assertNull(router.resolve(new TestInput("a", "b", "c")));
	}

	@Test
	public void testRemoveRoute_exactRoute() {
		TestRouter router = new TestRouter();

		TestRoute[] routes = new TestRoute[] {
			new TestRoute(router, "a", null, null, "a"),
			new TestRoute(router, "a", "b", null, "ab"),
			new TestRoute(router, "a", null, "c", "ac"),
			new TestRoute(router, "a", "b", "c", "abc")
		};

		Arrays.stream(routes).forEach(router::setRoute);

		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "z")));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("ac", router.resolve(new TestInput("a", null, "c")));
		Assertions.assertEquals("ac", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("abc", router.resolve(new TestInput("a", "b", "c")));

		router.removeRoute(new TestRoute(router, "a", null, null));

		Assertions.assertNull(router.resolve(new TestInput("a", null, null)));
		Assertions.assertNull(router.resolve(new TestInput("a", "y", "z")));
		Assertions.assertNull(router.resolve(new TestInput("a", null, "z")));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "z")));
		Assertions.assertEquals("ac", router.resolve(new TestInput("a", null, "c")));
		Assertions.assertEquals("ac", router.resolve(new TestInput("a", "y", "c")));
		Assertions.assertEquals("abc", router.resolve(new TestInput("a", "b", "c")));
	}

	@Test
	public void testEnableDisableRoute() {
		TestRouter router = new TestRouter();

		router.setRoute(new TestRoute(router, "a", null, null, "a"));
		router.setRoute(new TestRoute(router, "a", "b", null, "ab"));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", null)));

		// Disable the exact route
		router.disableRoute(new TestRoute(router, "a", null, null));

		Assertions.assertNull(router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertNull(router.resolve(new TestInput("a", null, "y")));
		Assertions.assertNull(router.resolve(new TestInput("a", "x", null)));
		Assertions.assertNull(router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", null)));

		router.enableRoute(new TestRoute(router, "a", null, null));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", null)));

		router.disableRoute(new TestRoute(router, "a", "b", null));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "b", null)));

		router.enableRoute(new TestRoute(router, "a", "b", null));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", null)));

		router.disableRoute(new TestRoute(router, "a", "b", null));
		router.setRoute(new TestRoute(router, "a", "b", null, "cd"));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "b", null)));

		router.enableRoute(new TestRoute(router, "a", "b", null));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("cd", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("cd", router.resolve(new TestInput("a", "b", null)));

		router.disableRoute(new TestRoute(router, "a", null, null));
		router.setRoute(new TestRoute(router, null, null, null, "def"));

		Assertions.assertEquals("def", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("def", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("def", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("def", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("cd", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("cd", router.resolve(new TestInput("a", "b", null)));

		router.enableRoute(new TestRoute(router, "a", null, null));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, "y")));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "x", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", null, null)));
		Assertions.assertEquals("cd", router.resolve(new TestInput("a", "b", "y")));
		Assertions.assertEquals("cd", router.resolve(new TestInput("a", "b", null)));
	}

	@Test
	public void testGetRoutes() {
		TestRouter router = new TestRouter();

		router.setRoute(new TestRoute(router, "a", null, null, "a"));
		router.setRoute(new TestRoute(router, "a", "b", null, "ab"));

		Set<TestRoute> routes = router.getRoutes();
		Assertions.assertEquals(Set.of(new TestRoute(router, "a", null, null, "a"), new TestRoute(router, "a", "b", null, "ab")), routes);

		router.disableRoute(new TestRoute(router, "a", null, null));

		routes = router.getRoutes();
		Assertions.assertEquals(Set.of(new TestRoute(router, "a", null, null, "a"), new TestRoute(router, "a", "b", null, "ab")), routes);
		// TODO check /a is disabled
		routes.forEach(r -> {
			if(r.get().equals("a")) {
				Assertions.assertTrue(r.isDisabled());
			}
			else {
				Assertions.assertFalse(r.isDisabled());
			}
		});

		router.setRoute(new TestRoute(router, "a", "b", "c", "abc"));

		routes = router.getRoutes();
		Assertions.assertEquals(Set.of(new TestRoute(router, "a", null, null, "a"), new TestRoute(router, "a", "b", null, "ab"),  new TestRoute(router, "a", "b", "c", "abc")), routes);
	}

	@Test
	public void testRouteRemove() {
		TestRouter router = new TestRouter();

		router.setRoute(new TestRoute(router, "a", null, null, "a"));
		router.setRoute(new TestRoute(router, "a", "b", null, "ab"));

		router.getRoutes().stream().filter(r -> r.get().equals("a")).findAny().get().remove();

		Set<TestRoute> routes = router.getRoutes();
		Assertions.assertEquals(Set.of(new TestRoute(router, "a", "b", null, "ab")), routes);

		router.setRoute(new TestRoute(router, "a", null, null, "x"));

		router.getRoutes().stream().filter(r -> r.get().equals("ab")).findAny().get().remove();

		routes = router.getRoutes();
		Assertions.assertEquals(Set.of(new TestRoute(router, "a", null, null, "x")), routes);
	}

	@Test
	public void testRouteEnableDisable() {
		TestRouter router = new TestRouter();

		router.setRoute(new TestRoute(router, "a", null, null, "a"));
		router.setRoute(new TestRoute(router, "a", "b", null, "ab"));

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "z")));

		TestRoute disabledRoute = router.getRoutes().stream().filter(r -> r.get().equals("a")).findAny().get();

		Assertions.assertFalse(disabledRoute.isDisabled());
		disabledRoute.disable();
		Assertions.assertTrue(disabledRoute.isDisabled());

		Assertions.assertNull(router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "z")));

		router.enableRoute(new TestRoute(router, "a", null, null));

		Assertions.assertFalse(router.getRoutes().stream().filter(r -> r.get().equals("a")).findAny().get().isDisabled());

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("ab", router.resolve(new TestInput("a", "b", "z")));

		disabledRoute = router.getRoutes().stream().filter(r -> r.get().equals("ab")).findAny().get();

		Assertions.assertFalse(disabledRoute.isDisabled());
		disabledRoute.disable();
		Assertions.assertTrue(disabledRoute.isDisabled());

		Assertions.assertEquals("a", router.resolve(new TestInput("a", "y", null)));
		Assertions.assertEquals("a", router.resolve(new TestInput("a", "b", "z")));

		disabledRoute.enable();
		Assertions.assertFalse(disabledRoute.isDisabled());

		Set<TestRoute> routes = router.getRoutes();
		Assertions.assertEquals(Set.of(new TestRoute(router, "a", null, null, "a"), new TestRoute(router, "a", "b", null, "ab")), routes);
		Assertions.assertTrue(routes.stream().allMatch(r -> !r.isDisabled()));
	}

	@Test
	public void testRoute() {
		TestRouter router = new TestRouter();

		router
			.route()
				.a("a")
				.b("b")
				.c("c")
				.set("abc")
			.route()
				.a("a2")
				.a("a3")
				.a("a4")
				.b("b")
				.set("multi_ab");

		Assertions.assertEquals("abc", router.resolve(new TestInput("a", "b", "c")));
		Assertions.assertEquals("multi_ab", router.resolve(new TestInput("a2", "b", "x")));
		Assertions.assertEquals("multi_ab", router.resolve(new TestInput("a3", "b", "y")));
		Assertions.assertEquals("multi_ab", router.resolve(new TestInput("a4", "b", "z")));

		Set<TestRoute> routes = router.getRoutes();

		Assertions.assertEquals(
			Set.of(
				new TestRoute(router, "a", "b", "c", "abc"),
				new TestRoute(router, "a2", "b", null, "multi_ab"),
				new TestRoute(router, "a3", "b", null, "multi_ab"),
				new TestRoute(router, "a4", "b", null, "multi_ab")
			),
			routes
		);
	}

	@Test
	void testFindRoutes() {
		TestRouter router = new TestRouter();

		router.setRoute(new TestRoute(router, "a", null, null, "r1"));
		router.setRoute(new TestRoute(router, "a", null, "c", "r2"));
		router.setRoute(new TestRoute(router, "a", "b", null, "r3"));
		router.setRoute(new TestRoute(router, "x", null, null, "r4"));
		router.setRoute(new TestRoute(router, null, "u", null, "r5"));
		router.setRoute(new TestRoute(router, null, "v", "z", "r6"));

		Assertions.assertEquals(
			Set.of(
				new TestRoute(router, "a", null, null, "r1"),
				new TestRoute(router, "a", null, "c", "r2"),
				new TestRoute(router, "a", "b", null, "r3")
			),
			router.route().a("a").findRoutes()
		);

		Assertions.assertEquals(
			Set.of(
				new TestRoute(router, "x", null, null, "r4")
			),
			router.route().a("x").findRoutes()
		);

		Set<TestRoute> routes1 = router.route().b("u").b("v").findRoutes();

		Assertions.assertEquals(
			Set.of(
				new TestRoute(router, null, "u", null, "r5"),
				new TestRoute(router, null, "v", "z", "r6")
			),
			router.route().b("u").b("v").findRoutes()
		);
	}

	private static <T> Stream<List<T>> permutations(T[] arr) {
		final long size = IntStream.rangeClosed(2, arr.length).reduce((x, y) -> x * y).getAsInt();
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<List<T>>(size, Spliterator.SIZED) {

			int[] indexes = new int[arr.length];
			int i = 0;

			@Override
			public boolean tryAdvance(Consumer<? super List<T>> action) {
				if (indexes[i] < i) {
					int a = i % 2 == 0 ?  0: indexes[i];
					int b = i;

					T tmp = arr[a];
					arr[a] = arr[b];
					arr[b] = tmp;

					action.accept(List.of(arr));
					indexes[i]++;
					i = 0;
				}
				else {
					indexes[i] = 0;
					i++;
				}
				return i < arr.length;
			}
		}, false);
	}

	private static class TestInput {

		private final String a;
		private final String b;
		private final String c;

		public TestInput(String a, String b, String c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public String getA() {
			return a;
		}

		public String getB() {
			return b;
		}

		public String getC() {
			return c;
		}
	}

	private static class TestRouter extends AbstractRouter<String, TestInput, TestRoute, TestRouteManager, TestRouter, TestRouteExtractor> {

		public TestRouter() {
			super(RoutingLink
				.link(ARoutingLink::new)
				.link(BRoutingLink::new)
				.link(ign -> new CRoutingLink())
			);
		}

		@Override
		public void setRoute(TestRoute route) {
			super.setRoute(route);
		}

		@Override
		public void removeRoute(TestRoute route) {
			super.removeRoute(route);
		}

		@Override
		public void enableRoute(TestRoute route) {
			super.enableRoute(route);
		}

		@Override
		public void disableRoute(TestRoute route) {
			super.disableRoute(route);
		}

		@Override
		protected TestRoute createRoute(String resource, boolean disabled) {
			return new TestRoute(this, resource, disabled);
		}

		@Override
		protected TestRouteExtractor createRouteExtractor() {
			return new TestRouteExtractor(this);
		}

		@Override
		protected TestRouteManager createRouteManager() {
			return new TestRouteManager(this);
		}
	}

	private static class TestRouteManager extends AbstractRouteManager<String, TestInput, TestRoute, TestRouteManager, TestRouter, TestRouteExtractor> {

		private Set<String> as;
		private Set<String> bs;
		private Set<String> cs;

		public TestRouteManager(TestRouter router) {
			super(router);
		}

		public TestRouteManager a(String a) {
			Objects.requireNonNull(a);
			if(this.as == null) {
				this.as = new HashSet<>();
			}
			this.as.add(a);
			return this;
		}

		public TestRouteManager b(String b) {
			Objects.requireNonNull(b);
			if(this.bs == null) {
				this.bs = new HashSet<>();
			}
			this.bs.add(b);
			return this;
		}

		public TestRouteManager c(String c) {
			Objects.requireNonNull(c);
			if(this.cs == null) {
				this.cs = new HashSet<>();
			}
			this.cs.add(c);
			return this;
		}

		protected final boolean matchesA(TestRoute route) {
			if(this.as != null && !this.as.isEmpty()) {
				if(route.getA() == null || !this.as.contains(route.getA())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesB(TestRoute route) {
			if(this.bs != null && !this.bs.isEmpty()) {
				if(route.getB() == null || !this.bs.contains(route.getB())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesC(TestRoute route) {
			if(this.cs != null && !this.cs.isEmpty()) {
				if(route.getC() == null || !this.cs.contains(route.getC())) {
					return false;
				}
			}
			return true;
		}

		@Override
		protected Predicate<TestRoute> routeMatcher() {
			return ((Predicate<TestRoute>)this::matchesA).and(this::matchesB).and(this::matchesC);
		}

		protected final Consumer<TestRouteExtractor> aRouteExtractor(Consumer<TestRouteExtractor> next) {
			return extractor -> {
				if(this.as != null && !this.as.isEmpty()) {
					for(String a : this.as) {
						next.accept(extractor.a(a));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		protected final Consumer<TestRouteExtractor> bRouteExtractor(Consumer<TestRouteExtractor> next) {
			return extractor -> {
				if(this.bs != null && !this.bs.isEmpty()) {
					for(String b : this.bs) {
						next.accept(extractor.b(b));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		protected final Consumer<TestRouteExtractor> cRouteExtractor(Consumer<TestRouteExtractor> next) {
			return extractor -> {
				if(this.cs != null && !this.cs.isEmpty()) {
					for(String c : this.cs) {
						next.accept(extractor.c(c));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		@Override
		protected Function<Consumer<TestRouteExtractor>, Consumer<TestRouteExtractor>> routeExtractor() {
			return ((Function<Consumer<TestRouteExtractor>, Consumer<TestRouteExtractor>>)this::aRouteExtractor).compose(this::bRouteExtractor).compose(this::cRouteExtractor);
		}
	}

	private static class TestRouteExtractor extends AbstractRouteExtractor<String, TestInput, TestRoute, TestRouteManager, TestRouter, TestRouteExtractor> {

		private String a;
		private String b;
		private String c;

		public TestRouteExtractor(TestRouter router) {
			super(router);
		}

		protected TestRouteExtractor(TestRouteExtractor parent) {
			super(parent);
		}

		public TestRouteExtractor a(String a) {
			TestRouteExtractor childExtractor = new TestRouteExtractor(this);
			childExtractor.a = a;
			return childExtractor;
		}

		public String getA() {
			if(this.a != null) {
				return this.a;
			}
			return this.parent != null ? this.parent.getA() : null;
		}

		public TestRouteExtractor b(String b) {
			TestRouteExtractor childExtractor = new TestRouteExtractor(this);
			childExtractor.b = b;
			return childExtractor;
		}

		public String getB() {
			if(this.b != null) {
				return this.b;
			}
			return this.parent != null ? this.parent.getB() : null;
		}

		public TestRouteExtractor c(String c) {
			TestRouteExtractor childExtractor = new TestRouteExtractor(this);
			childExtractor.c = c;
			return childExtractor;
		}

		public String getC() {
			if(this.c != null) {
				return this.c;
			}
			return this.parent != null ? this.parent.getC() : null;
		}

		@Override
		protected void populateRoute(TestRoute route) {
			route.setA(this.getA());
			route.setB(this.getB());
			route.setC(this.getC());
		}
	}

	private static class TestRoute extends AbstractRoute<String, TestInput, TestRoute, TestRouteManager, TestRouter, TestRouteExtractor> {

		private String a;
		private String b;
		private String c;

		public TestRoute(TestRouter router, String resource, boolean disabled) {
			this(router,null, null,null, resource, disabled);
		}

		public TestRoute(TestRouter router, String a, String b, String c) {
			this(router, a, b, c, null, false);
		}

		public TestRoute(TestRouter router, String a, String b, String c, String resource) {
			this(router, a, b, c, resource, false);
		}

		public TestRoute(TestRouter router, String a, String b, String c, String resource, boolean disabled) {
			super(router, resource, disabled);
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public void setA(String a) {
			this.a = a;
		}

		public String getA() {
			return a;
		}

		public void setB(String b) {
			this.b = b;
		}

		public String getB() {
			return b;
		}

		public void setC(String c) {
			this.c = c;
		}

		public String getC() {
			return c;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			TestRoute testRoute = (TestRoute) o;
			return Objects.equals(a, testRoute.a) && Objects.equals(b, testRoute.b) && Objects.equals(c, testRoute.c);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), a, b, c);
		}
	}

	private static class ARoutingLink extends RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		private final Map<String, RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> links;

		public ARoutingLink(Supplier<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> nextLinkFactory) {
			super(nextLinkFactory);
			this.links = new HashMap<>();
		}

		@Override
		protected boolean canLink(TestRoute route) {
			return route.getA() != null;
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> getLink(TestRoute route) {
			return this.links.get(route.getA());
		}

		@Override
		protected Collection<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> getLinks() {
			return this.links.values();
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> getOrSetLink(TestRoute route) {
			return this.links.computeIfAbsent(route.getA(), a -> this.createLink());
		}

		@Override
		protected void removeLink(TestRoute route) {
			this.links.remove(route.getA());
		}

		@Override
		protected void refreshEnabled() {

		}

		@Override
		protected void extractLinks(TestRouteExtractor routeExtractor) {
			this.links.entrySet().forEach(e -> e.getValue().extractRoutes(routeExtractor.a(e.getKey())));
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> resolveLink(TestInput input) {
			return this.links.get(input.getA());
		}

		@Override
		protected List<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> resolveAllLink(TestInput input) {
			if(input.getA() != null) {
				RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = this.links.get(input.getA());
				return link != null ? List.of(link) : List.of();
			}
			else {
				return new ArrayList<>(this.links.values());
			}
		}
	}

	private static class BRoutingLink extends RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		private final Map<String, RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> links;

		public BRoutingLink(Supplier<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> nextLinkFactory) {
			super(nextLinkFactory);
			this.links = new HashMap<>();
		}

		@Override
		protected boolean canLink(TestRoute route) {
			return route.getB() != null;
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> getLink(TestRoute route) {
			return this.links.get(route.getB());
		}

		@Override
		protected Collection<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> getLinks() {
			return this.links.values();
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> getOrSetLink(TestRoute route) {
			return this.links.computeIfAbsent(route.getB(), a -> this.createLink());
		}

		@Override
		protected void removeLink(TestRoute route) {
			this.links.remove(route.getB());
		}

		@Override
		protected void refreshEnabled() {

		}

		@Override
		protected void extractLinks(TestRouteExtractor routeExtractor) {
			this.links.entrySet().forEach(e -> {
				e.getValue().extractRoutes(routeExtractor.b(e.getKey()));
			});
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> resolveLink(TestInput input) {
			return this.links.get(input.getB());
		}

		@Override
		protected List<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> resolveAllLink(TestInput input) {
			if(input.getB() != null) {
				RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = this.links.get(input.getB());
				return link != null ? List.of(link) : List.of();
			}
			else {
				return new ArrayList<>(this.links.values());
			}
		}
	}

	private static class CRoutingLink extends RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> {

		private final Map<String, RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> links;

		public CRoutingLink() {
			super();
			this.links = new HashMap<>();
		}

		@Override
		protected boolean canLink(TestRoute route) {
			return route.getC() != null;
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> getLink(TestRoute route) {
			return this.links.get(route.getC());
		}

		@Override
		protected Collection<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> getLinks() {
			return this.links.values();
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> getOrSetLink(TestRoute route) {
			return this.links.computeIfAbsent(route.getC(), a -> this.createLink());
		}

		@Override
		protected void removeLink(TestRoute route) {
			this.links.remove(route.getC());
		}

		@Override
		protected void refreshEnabled() {

		}

		@Override
		protected void extractLinks(TestRouteExtractor routeExtractor) {
			this.links.entrySet().forEach(e -> {
				e.getValue().extractRoutes(routeExtractor.c(e.getKey()));
			});
		}

		@Override
		protected RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> resolveLink(TestInput input) {
			return this.links.get(input.getC());
		}

		@Override
		protected List<RoutingLink<String, TestInput, TestRoute, TestRouteExtractor>> resolveAllLink(TestInput input) {
			if(input.getC() != null) {
				RoutingLink<String, TestInput, TestRoute, TestRouteExtractor> link = this.links.get(input.getC());
				return link != null ? List.of(link) : List.of();
			}
			else {
				return new ArrayList<>(this.links.values());
			}
		}
	}
}

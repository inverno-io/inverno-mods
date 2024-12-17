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

import java.util.Set;

/**
 * <p>
 * a route extractor is used internally in an {@link AbstractRouter} to extract routes defined in the routing chain.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the resource type
 * @param <B> the route type
 */
public interface RouteExtractor<A, B extends Route<A>> {

	/**
	 * <p>
	 * Sets the resource extracted from the routing chain and stores the extracted routes.
	 * </p>
	 *
	 * @param resource a resource
	 * @param disabled true if the corresponding route is disabled, false otherwise
	 */
	void set(A resource, boolean disabled);

	/**
	 * <p>
	 * Returns the set of extracted routes.
	 * </p>
	 *
	 * @return a set of routes
	 */
	Set<B> getRoutes();
}
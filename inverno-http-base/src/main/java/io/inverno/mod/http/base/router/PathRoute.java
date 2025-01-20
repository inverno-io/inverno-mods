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

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.base.net.URIs;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A path route.
 * </p>
 *
 * <p>
 * This is used to define route based on the path specified in an input. For instance, in order to resolve a handler for an HTTP request targeting resource {@code /path/to/resource}, a path route must
 * be defined with {@code /path/to/resource} path targeting a specific resource handler.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.PathRoutingLink
 *
 * @param <A> the resource type
 */
public interface PathRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns an absolute normalized path.
	 * </p>
	 *
	 * @return an absolute normalized path or null
	 */
	String getPath();

	/**
	 * <p>
	 * Returns an absolute URI pattern.
	 * </p>
	 *
	 * @return an absolute URI pattern or null
	 */
	URIPattern getPathPattern();

	/**
	 * <p>
	 * A path route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the path route type
	 * @param <C> the path route extractor
	 */
	interface Extractor<A, B extends PathRoute<A>, C extends PathRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified absolute path.
		 * </p>
		 *
		 * @param path an absolute path
		 *
		 * @return a route extractor
		 */
		C path(String path);

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified absolute path pattern.
		 * </p>
		 *
		 * @param pathPattern an absolute path pattern
		 *
		 * @return a route extractor
		 */
		C pathPattern(URIPattern pathPattern);
	}

	/**
	 * <p>
	 * A path route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the path route type
	 * @param <D> the path route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends PathRoute<A>, D extends PathRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the path or path pattern matching the path in an input.
		 * </p>
		 *
		 * <p>
		 * This method determines whether the specified path is a static path or a pattern and then delegates to {@link #path(String)} or {@link #pathPattern(URIPattern)}.
		 * </p>
		 *
		 * @param path an absolute path or path pattern
		 * @param matchTrailingSlash true to match trailing slash, false otherwise
		 *
		 * @return the route manager
		 *
		 * @throws IllegalArgumentException if the specified path is not absolute
		 */
		@SuppressWarnings("unchecked")
		default D resolvePath(String path, boolean matchTrailingSlash) throws IllegalArgumentException {
			Objects.requireNonNull(path);
			if(!path.startsWith("/")) {
				throw new IllegalArgumentException("Path must be absolute");
			}

			URIBuilder pathBuilder = URIs.uri(path, URIs.RequestTargetForm.PATH, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);

			List<String> pathParameterNames = pathBuilder.getParameterNames();
			if(pathParameterNames.isEmpty()) {
				// Static path
				String rawPath = pathBuilder.buildRawPath();
				this.path(rawPath);
				if (matchTrailingSlash) {
					if (rawPath.endsWith("/")) {
						this.path(rawPath.substring(0, rawPath.length() - 1));
					}
					else {
						this.path(rawPath + "/");
					}
				}
			}
			else {
				// PathPattern
				this.pathPattern(pathBuilder.buildPathPattern(matchTrailingSlash));
			}
			return (D)this;
		}

		/**
		 * <p>
		 * Specifies the absolute path matching the path in an input.
		 * </p>
		 *
		 * @param path an absolute path
		 *
		 * @return the route manager
		 */
		D path(String path);

		/**
		 * <p>
		 * Specifies the absolute path pattern matching the path in an input.
		 * </p>
		 *
		 * @param pathPattern an absolute path pattern
		 *
		 * @return the route manager
		 */
		D pathPattern(URIPattern pathPattern);
	}
}

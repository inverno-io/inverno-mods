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

import io.inverno.mod.base.net.URIPattern;

/**
 * <p>
 * A URI route.
 * </p>
 *
 * <p>
 * This is used to define route based on the URI specified in an input. For instance, in order to resolve the socket address targeted by the URI {@code http://sampleHost}, a URI route must be defined
 * with {@code http://sampleHost} URI targeting a resolved socket address.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.URIRoutingLink
 *
 * @param <A> the resource type
 */
public interface URIRoute<A> extends Route<A> {

	String getURI();

	URIPattern getURIPattern();

	interface Extractor<A, B extends URIRoute<A>, C extends URIRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		C uri(String uri);

		C uriPattern(URIPattern uriPattern);
	}

	interface Manager<A, B, C extends URIRoute<A>, D extends URIRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		D uri(String uri);

		D uriPattern(URIPattern uriPattern);
	}
}
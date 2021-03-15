/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.internal;

import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.web.AbstractRoute;
import io.winterframework.mod.web.ContentAwareRoute;

/**
 * <p>
 * A route extractor to extract {@link ContentAwareRoute} routes.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of exchange handled by the route
 * @param <B> the route type
 * @param <C> the route extractor type
 */
interface ContentAwareRouteExtractor<A extends Exchange, B extends AbstractRoute<A>, C extends ContentAwareRouteExtractor<A, B, C>> extends RouteExtractor<A, B> {

	/**
	 * <p>
	 * Sets the extractor to extract routes which consume the specified media range.
	 * </p>
	 * 
	 * @param mediaRange the media range matching the type of the routes to extract
	 * 
	 * @return the route extractor
	 */
	C consumes(String mediaRange);
}

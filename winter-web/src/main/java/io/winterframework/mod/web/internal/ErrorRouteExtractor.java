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

import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.web.ErrorRoute;

/**
 * <p>
 * A route extractor to extract {@link ErrorRoute} routes.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
interface ErrorRouteExtractor extends 
	AcceptAwareRouteExtractor<ErrorExchange<Throwable>, ErrorRoute, ErrorRouteExtractor>,
	ErrorAwareRouteExtractor<ErrorExchange<Throwable>, ErrorRoute, ErrorRouteExtractor>, 
	RouteExtractor<ErrorExchange<Throwable>, ErrorRoute> {

}

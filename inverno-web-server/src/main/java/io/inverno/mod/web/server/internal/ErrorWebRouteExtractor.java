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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.ErrorWebRoute;

/**
 * <p>
 * A route extractor to extract {@link ErrorWebRoute} routes.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the type of the exchange context
 */
interface ErrorWebRouteExtractor<A extends ExchangeContext> extends
	PathAwareRouteExtractor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteExtractor<A>>,
	AcceptAwareRouteExtractor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteExtractor<A>>,
	ErrorAwareRouteExtractor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteExtractor<A>>, 
	InterceptableRouteExtractor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ErrorWebRouteExtractor<A>> {

}

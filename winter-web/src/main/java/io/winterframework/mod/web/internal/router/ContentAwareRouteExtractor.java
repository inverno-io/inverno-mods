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
package io.winterframework.mod.web.internal.router;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.router.Route;

/**
 * @author jkuhn
 *
 */
public interface ContentAwareRouteExtractor<A, B, C extends Exchange<A, B>, D extends Route<A,B,C>, E extends ContentAwareRouteExtractor<A, B, C, D, E>> extends RouteExtractor<A, B, C, D> {

	E consumes(String mediaType);
}

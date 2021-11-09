/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.WebInterceptedRouteManager;

/**
 * <p>
 * Generic {@link WebInterceptedRouteManager} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
class GenericWebInterceptedRouteManager extends AbstractWebRouteManager<GenericWebInterceptedRouter, GenericWebInterceptedRouteManager> implements WebInterceptedRouteManager<ExchangeContext> {

	/**
	 * <p>
	 * Creates a generic web route manager.
	 * </p>
	 * 
	 * @param router a generic web intercepted router
	 */
	public GenericWebInterceptedRouteManager(GenericWebInterceptedRouter router) {
		super(router);
	}
}

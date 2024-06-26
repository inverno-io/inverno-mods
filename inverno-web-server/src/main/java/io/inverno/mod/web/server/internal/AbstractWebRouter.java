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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.server.WebRoute;

/**
 * <p>
 * Base Web router class.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
abstract class AbstractWebRouter extends AbstractRouter {
	
	/**
	 * <p>
	 * Sets the specified web route in the router.
	 * </p>
	 * 
	 * @param route a web route
	 */
	abstract void setRoute(WebRoute<ExchangeContext> route);
	
	/**
	 * <p>
	 * Enables the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to enable
	 */
	abstract void enableRoute(WebRoute<ExchangeContext> route);
	
	/**
	 * <p>
	 * Disables the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to disable
	 */
	abstract void disableRoute(WebRoute<ExchangeContext> route);

	/**
	 * <p>
	 * Removes the specified web route if it exists.
	 * </p>
	 * 
	 * @param route the web route to remove
	 */
	abstract void removeRoute(WebRoute<ExchangeContext> route);
}

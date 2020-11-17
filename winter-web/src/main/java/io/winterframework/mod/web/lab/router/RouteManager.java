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
package io.winterframework.mod.web.lab.router;

import java.util.List;

import io.winterframework.mod.web.RequestHandler;

/**
 * @author jkuhn
 *
 */
public interface RouteManager<A, B, C, D extends Router<A, B, C, D, E, F>, E extends RouteManager<A, B, C, D, E, F>, F extends Route<A, B, C>> {

	D handler(RequestHandler<A, B, C> handler);
	
	// TODO These can be tricky because a manager can actually target sets of routes
	// - is it always possible to remove/disable routes which are part of a wider set of routes?
	D enable();
	
	D disable();
	
	D remove();
	
	List<F> findRoutes();
}

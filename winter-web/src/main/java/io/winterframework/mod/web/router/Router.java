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
package io.winterframework.mod.web.router;

import java.util.Set;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.router.GenericErrorRouter;
import io.winterframework.mod.web.internal.router.GenericWebRouter;

/**
 * @author jkuhn
 *
 */
public interface Router<A, B, C extends Exchange<A, B>, D extends Router<A, B, C, D, E, F, G, H, I>, E extends RouteManager<A, B, C, D, E, F, G, H, I>, F extends Route<A, B, C>, G, H, I extends Exchange<G, H>> extends ExchangeHandler<G, H, I> {
	
	static WebRouter<RequestBody, ResponseBody, WebExchange<RequestBody, ResponseBody>> web() {
		return new GenericWebRouter();
	}
	
	static ErrorRouter error() {
		return new GenericErrorRouter();
	}
	
	E route();
	
	Set<F> getRoutes();
	
	// interceptors?
}

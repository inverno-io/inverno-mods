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

import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;

/**
 * @author jkuhn
 *
 */
public interface WebRouteManager<A, B, C extends WebContext> extends RouteManager<A, B, C, WebRouter<A, B, C>, WebRouteManager<A, B, C>, WebRoute<A, B, C>, RequestBody, ResponseBody, Void> {
	
	WebRouteManager<A, B, C> path(String path) throws IllegalArgumentException;
		
	WebRouteManager<A, B, C> path(String path, boolean matchTrailingslash) throws IllegalArgumentException;
	
	WebRouteManager<A, B, C> method(Method method);
	
	WebRouteManager<A, B, C> consumes(String mediaType);
	
	WebRouteManager<A, B, C> produces(String mediaType);
	
	WebRouteManager<A, B, C> language(String language);
}

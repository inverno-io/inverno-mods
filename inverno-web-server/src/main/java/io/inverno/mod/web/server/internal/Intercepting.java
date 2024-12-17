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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.server.internal.router.InternalErrorWebRouteInterceptorRouter;
import io.inverno.mod.web.server.internal.router.InternalWebRouteInterceptorRouter;

/**
 * <p>
 * Identifies a Web server that can intercept Web routes and error Web routes.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
interface Intercepting<A extends ExchangeContext> {

	/**
	 * <p>
	 * Returns the internal Web route interceptor router holding and resolving Web route interceptors.
	 * </p>
	 *
	 * @return the internal Web route interceptor router
	 */
	InternalWebRouteInterceptorRouter<A> getWebRouteInterceptorRouter();

	/**
	 * <p>
	 * Sets the internal Web route interceptor router holding and resolving Web route interceptors.
	 * </p>
	 *
	 * @param interceptorRouter the internal Web route interceptor router
	 */
	void setWebRouteInterceptorRouter(InternalWebRouteInterceptorRouter<A> interceptorRouter);

	/**
	 * <p>
	 * Returns the internal error Web route interceptor router holding and resolving error Web route interceptors.
	 * </p>
	 *
	 * @return the internal error Web route interceptor router
	 */
	InternalErrorWebRouteInterceptorRouter<A> getErrorWebRouteInterceptorRouter();

	/**
	 * <p>
	 * Sets the internal error Web route interceptor router holding and resolving error Web route interceptors.
	 * </p>
	 *
	 * @param errorInterceptorRouter the internal error Web route interceptor router
	 */
	void setErrorWebRouteInterceptorRouter(InternalErrorWebRouteInterceptorRouter<A> errorInterceptorRouter);
}

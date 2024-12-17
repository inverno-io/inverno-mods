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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.ExchangeContext;
import java.util.List;
import java.util.function.Function;

/**
 * <p>
 * Entry point for configuring the error Web exchange interceptors to apply when creating error Web routes in an intercepted Web server.
 * </p>
 *
 * <p>
 * It is implemented by the {@link WebServer}. Interceptors are defined using an {@link ErrorWebRouteInterceptorManager} which allows to specify the criteria an error Web route must match for the
 * error Web exchange interceptor to be executed.
 * </p>
 *
 * <p>
 * When defining an interceptor, the {@link ErrorWebRouteInterceptorManager} shall return a new intercepted Web server instance containing the interceptor definition. It can then be used to define
 * more interceptors resulting in the creation of hierarchies of Web servers with different interceptor definitions.
 * </p>
 *
 * <p>
 * Error Web routes that are expected to be intercepted must then be defined after defining interceptors on the resulting intercepted Web server.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebServer
 *
 * @param <A> the exchange context type
 */
public interface ErrorWebRouteInterceptor<A extends ExchangeContext> {

	/**
	 * <p>
	 * Returns a new interceptor manager for defining an error Web route interceptor.
	 * </p>
	 *
	 * @return a new error Web route interceptor manager
	 */
	ErrorWebRouteInterceptorManager<A, ? extends ErrorWebRouteInterceptor<A>> interceptError();

	/**
	 * <p>
	 * Configures an error Web route interceptor.
	 * </p>
	 *
	 * <p>
	 * The result returned by the specified configurer function must be the error Web route interceptor returned by the input interceptor manager after defining the interceptor and which contains the
	 * configured interceptor. If that requirement is not met, invoking this method will most likely result in a no-op operation since no route could be defined on an error Web route interceptor
	 * containing the configured interceptor.
	 * </p>
	 *
	 * @param configurer an error Web route interceptor configurer function
	 *
	 * @return the error Web route interceptor containing the configured interceptor
	 */
	default ErrorWebRouteInterceptor<A> interceptError(Function<ErrorWebRouteInterceptorManager<A, ? extends ErrorWebRouteInterceptor<A>>, ? extends ErrorWebRouteInterceptor<A>> configurer) {
		return configurer.apply(this.interceptError());
	}

	/**
	 * <p>
	 * Configures multiple error Web route interceptors.
	 * </p>
	 *
	 * @param configurer an error Web route interceptor configurer
	 *
	 * @return the error Web route interceptor containing the configured interceptors
	 */
	ErrorWebRouteInterceptor<A> configureErrorInterceptors(ErrorWebRouteInterceptor.Configurer<? super A> configurer);

	/**
	 * <p>
	 * Configures multiple error Web route interceptors.
	 * </p>
	 *
	 * @param configurers a list of error Web route interceptor configurers
	 *
	 * @return the error Web route interceptor containing the configured interceptors
	 */
	ErrorWebRouteInterceptor<A> configureErrorInterceptors(List<Configurer<? super A>> configurers);

	/**
	 * <p>
	 * A configurer used to configure error Web route interceptors in a Web server.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	@FunctionalInterface
	interface Configurer<A extends ExchangeContext> {

		/**
		 * <p>
		 * Configures error Web route interceptors.
		 * </p>
		 *
		 * <p>
		 * The returned error Web route interceptor must be the instance returned by the interceptor manager used to define the last interceptor. If that requirement is not met, some interceptor
		 * definitions might be missing when defining routes afterwards.
		 * </p>
		 *
		 * @param errorInterceptors the error Web route interceptor to use to define error Web route interceptors
		 *
		 * @return the error Web route interceptor containing the configured interceptors
		 */
		ErrorWebRouteInterceptor<A> configure(ErrorWebRouteInterceptor<A> errorInterceptors);
	}
}

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
package io.inverno.mod.web.client;

import io.inverno.mod.http.base.ExchangeContext;
import java.util.List;
import java.util.function.Function;

/**
 * <p>
 * Entry point for configuring the Web exchange interceptors to apply when processing Web exchange in an intercepted Web client.
 * </p>
 *
 * <p>
 * It is implemented by the {@link WebClient}. Interceptors are defined using a {@link WebRouteInterceptorManager} which allows to specify the criteria a Web exchange to be intercepted by an
 * exchange interceptor.
 * </p>
 *
 * <p>
 * When defining an interceptor on a Web client, a new intercepted Web client instance shall be created containing the interceptor definition. It can then be used to define more interceptors resulting
 * in the creation of hierarchies of intercepted Web clients with different interceptor definitions with the root Web client at the top.
 * </p>
 *
 * <p>
 * Web exchanges that are expected to be intercepted must then be processed by the intercepted Web client chain containing the desired interceptor definitions.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public interface WebRouteInterceptor<A extends ExchangeContext> {

	/**
	 * <p>
	 * Returns a new interceptor manager for defining a Web route interceptor.
	 * </p>
	 *
	 * @return a new Web route interceptor manager
	 */
	WebRouteInterceptorManager<A, ? extends WebRouteInterceptor<A>> intercept();

	/**
	 * <p>
	 * Configures a Web route interceptor.
	 * </p>
	 *
	 * <p>
	 * The result returned by the specified configurer function must be the Web route interceptor returned by the input interceptor manager after defining the interceptor and which contains the
	 * configured interceptor. If that requirement is not met, invoking this method will most likely result in a no-op operation since the resulting intercepted Web client instance would be lost.
	 * </p>
	 *
	 * @param configurer a Web route interceptor configurer function
	 *
	 * @return the Web route interceptor containing the configured interceptor
	 */
	default WebRouteInterceptor<A> intercept(Function<WebRouteInterceptorManager<A, ? extends WebRouteInterceptor<A>>, ? extends WebRouteInterceptor<A>> configurer) {
		return configurer.apply(this.intercept());
	}

	/**
	 * <p>
	 * Configures multiple Web route interceptors.
	 * </p>
	 *
	 * @param configurer a Web route interceptor configurer
	 *
	 * @return the Web route interceptor containing the configured interceptors
	 */
	WebRouteInterceptor<A> configureInterceptors(WebRouteInterceptor.Configurer<? super A> configurer);

	/**
	 * <p>
	 * Configures multiple Web route interceptors.
	 * </p>
	 *
	 * @param configurers a list of Web route interceptor configurers
	 *
	 * @return the Web route interceptor containing the configured interceptors
	 */
	WebRouteInterceptor<A> configureInterceptors(List<Configurer<? super A>> configurers);

	/**
	 * <p>
	 * A configurer used to configure Web route interceptors in a Web client.
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
		 * The returned Web route interceptor must be the instance returned by the interceptor manager used to define the last interceptor. If that requirement is not met, some interceptor definitions
		 * will be missing when processing Web exchanges on the resulting Web client or when defining subsequent route interceptors.
		 * </p>
		 *
		 * @param interceptors the Web route interceptor to use to define Web route interceptors
		 *
		 * @return the Web route interceptor containing the configured interceptors
		 */
		WebRouteInterceptor<A> configure(WebRouteInterceptor<A> interceptors);
	}
}

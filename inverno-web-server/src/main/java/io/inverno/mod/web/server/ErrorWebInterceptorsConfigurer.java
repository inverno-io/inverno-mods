/*
 * Copyright 2022 Jeremy KUHN
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

/**
 * <p>
 * A configurer used to configure interceptors in an error web router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see ErrorWebRouter
 * 
 * @param <A> the type of the exchange context
 */
public interface ErrorWebInterceptorsConfigurer<A extends ExchangeContext> {

	/**
	 * <p>
	 * Configures error web interceptors.
	 * </p>
	 *
	 * @param interceptors the interceptable to configure
	 */
	void configure(ErrorWebInterceptable<A, ?> interceptors);
}

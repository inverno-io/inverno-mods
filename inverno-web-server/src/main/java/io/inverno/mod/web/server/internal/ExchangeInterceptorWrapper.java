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
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;

/**
 * <p>
 * An exchange interceptor wrapper.
 * </p>
 * 
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
abstract class ExchangeInterceptorWrapper<A extends ExchangeContext, B extends Exchange<A>> implements ExchangeInterceptor<A, B> {
	
	protected final ExchangeInterceptor<A, B> wrappedInterceptor;
	
	public ExchangeInterceptorWrapper(ExchangeInterceptor<A, B> interceptor) {
		this.wrappedInterceptor = interceptor;
	}
		
	public ExchangeInterceptor<A, B> unwrap() {
		return this.wrappedInterceptor;
	}
}

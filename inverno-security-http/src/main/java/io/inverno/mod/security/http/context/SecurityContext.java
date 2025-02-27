/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.security.http.context;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.identity.Identity;

/**
 * <p>
 * The security exchange context which exposes {@link Authentication}, {@link Identity} and {@link AccessController} in the exchange context.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 * @param <B> the access controller type
 */
public interface SecurityContext<A extends Identity, B extends AccessController> extends io.inverno.mod.security.context.SecurityContext<A, B>, ExchangeContext {

	/**
	 * <p>
	 * An intercepted security exchange context used by security interceptors to populate the security context.
	 * </p>
	 *
	 * <p>
	 * It should be only considered when configuring security interceptors and handlers which must be the only one allowed to set the security context, applicative interceptors and handlers should always
	 * use the {@link io.inverno.mod.security.context.SecurityContext} instead.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the identity type
	 * @param <B> the access controller type
	 */
	interface Intercepted<A extends Identity, B extends AccessController> extends DelegatingSecurityContext<A, B> {

		/**
		 * <p>
		 * Sets the security context in the security exchange context.
		 * </p>
		 *
		 * @param securityContext the security context
		 */
		void setSecurityContext(io.inverno.mod.security.context.SecurityContext<? extends A, ? extends B> securityContext);
	}
}

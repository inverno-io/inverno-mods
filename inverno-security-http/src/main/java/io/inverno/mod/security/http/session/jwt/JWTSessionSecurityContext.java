/*
 * Copyright 2025 Jeremy KUHN
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
package io.inverno.mod.security.http.session.jwt;

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.session.http.context.jwt.JWTSessionContext;

/**
 * <p>
 * The JWT session security exchange context.
 * </p>
 *
 * <p>
 * This context extends both {@link SecurityContext} and {@link JWTSessionContext}, and as such expose both session and security contexts in the application. It shall be used to support JWT session
 * authentication.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the authentication type
 * @param <B> the identity type
 * @param <C> the access controller type
 * @param <D> the session data type
 */
public interface JWTSessionSecurityContext<A extends Authentication, B extends Identity, C extends AccessController, D> extends SecurityContext<B, C>, JWTSessionContext<D, A> {

	/**
	 * <p>
	 * An intercepted JWT session security exchange context used by session and security interceptors to populate the session context and the security context.
	 * </p>
	 *
	 * <p>
	 * It should only be considered when configuring JWT session authentication.
	 * </p>
	 *
	 * @param <A> the authentication type
	 * @param <B> the identity type
	 * @param <C> the access controller type
	 * @param <D> the session data type
	 */
	interface Intercepted<A extends Authentication, B extends Identity, C extends AccessController, D> extends JWTSessionSecurityContext<A, B, C, D>, SecurityContext.Intercepted<B, C>, JWTSessionContext.Intercepted<D, A> {

	}
}

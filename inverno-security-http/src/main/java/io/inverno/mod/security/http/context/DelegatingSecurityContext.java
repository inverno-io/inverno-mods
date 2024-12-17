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

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.identity.Identity;
import java.util.Optional;

/**
 * <p>
 * A security exchange context that delegates to a regular security context to return authentication, identity and access controller.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the identity type
 * @param <B> the access controller type
 */
interface DelegatingSecurityContext<A extends Identity, B extends AccessController> extends SecurityContext<A, B> {

	@Override
	default Authentication getAuthentication() {
		var ctx = this.getSecurityContext();
		if(ctx != null) {
            return ctx.getAuthentication();
        }
		return SecurityContext.super.getAuthentication();
	}

    @Override
	@SuppressWarnings("unchecked")
    default Optional<A> getIdentity() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return (Optional<A>) ctx.getIdentity();
        }
        return SecurityContext.super.getIdentity();
    }

    @Override
	@SuppressWarnings("unchecked")
    default Optional<B> getAccessController() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return (Optional<B>) ctx.getAccessController();
        }
        return SecurityContext.super.getAccessController();
    }

	/**
	 * <p>
	 * Returns the underlying security context.
	 * </p>
	 * 
	 * @return a regular security context
	 */
    io.inverno.mod.security.context.SecurityContext<? extends A, ? extends B> getSecurityContext();
}

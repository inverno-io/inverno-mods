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
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
interface DelegatingSecurityContext extends SecurityContext {

	@Override
	public default Authentication getAuthentication() {
		var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getAuthentication();
        }
		return SecurityContext.super.getAuthentication();
	}

    @Override
    default Optional<Identity> getIdentity() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getIdentity();
        }
        return SecurityContext.super.getIdentity();
    }

    @Override
    default Optional<AccessController> getAccessController() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getAccessController();
        }
        return SecurityContext.super.getAccessController();
    }

    io.inverno.mod.security.context.SecurityContext getSecurityContext();
}

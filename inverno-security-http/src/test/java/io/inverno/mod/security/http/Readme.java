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

package io.inverno.mod.security.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {

	public void doc() {
		CredentialsExtractor<Credentials> credentialsExtractor = null;
		
		Authenticator<Credentials, Authentication> authenticator = null;
		IdentityResolver<Authentication, Identity> identityResolver = null;
		AccessControllerResolver<Authentication, AccessController> accessControllerResolver = null;
		
		SecurityInterceptor<Credentials, Identity, AccessController, InterceptingSecurityContext<Identity, AccessController>, Exchange<InterceptingSecurityContext<Identity, AccessController>>> securityInterceptor = SecurityInterceptor.of(credentialsExtractor, authenticator, identityResolver, accessControllerResolver);
	}
}

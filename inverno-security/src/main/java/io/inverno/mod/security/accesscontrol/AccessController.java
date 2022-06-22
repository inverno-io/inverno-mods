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
package io.inverno.mod.security.accesscontrol;

import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;

/**
 * <p>
 * An access controller is used to control the access to services and resources of an authenticated entity.
 * </p>
 *
 * <p>
 * This represents the base access controller interface which should be extended and/or implemented to support actual access control processing such as Role Based Access Control, Permission Based
 * Access Control, Access Control List...
 * </p>
 *
 * <p>
 * The access controller is one of the components that make up the {@link SecurityContext} along with {@link Authentication} and {@link Identity}. An access controller is usually resolved from an
 * {@link Authentication} using an {@link AccessControllerResolver}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see SecurityContext
 * @see Authentication
 * @see Identity
 */
public interface AccessController {

}

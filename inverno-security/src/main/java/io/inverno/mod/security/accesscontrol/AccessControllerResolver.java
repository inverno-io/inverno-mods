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
import reactor.core.publisher.Mono;

/**
 * <p>
 * An access controller resolver is used to resolve an access controller from an {@link Authentication}.
 * </p>
 * 
 * <p>
 * An authentication basically identifies an authenticated entity in an application, an access controller resolver resolves the access controller that shall be used to determine whether the
 * authenticated entity can access a service or a resource.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of authenthentication
 * @param <B> the type of access controller
 */
@FunctionalInterface
public interface AccessControllerResolver<A extends Authentication, B extends AccessController> {

	/**
	 * <p>
	 * Resolves the access controller used to control access to services and resources for the specified authentication.
	 * </p>
	 *
	 * @param authentication an authentication
	 *
	 * @return a mono emitting the access controller or an empty mono if no access controller could have been resolved
	 *
	 * @throws AccessControlException if there was an error resolving the access controller
	 */
	Mono<B> resolveAccessController(A authentication) throws AccessControlException;
}

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
package io.inverno.mod.security.authentication;

import reactor.core.publisher.Mono;

/**
 * <p>
 * An authentication releaser is used to invalidate or release any resources involved when terminating an authentication.
 * </p>
 * 
 * <p>
 * Typical use cases might be the deletion of a session or the invalidation of a token.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the authentication
 */
@FunctionalInterface
public interface AuthenticationReleaser<A extends Authentication> {
	
	/**
	 * <p>
	 * Releases the specified authentication.
	 * </p>
	 * 
	 * @param authentication the authentication to release
	 * 
	 * @return a mono which completes once the authentication has been released
	 */
	Mono<Void> release(A authentication);
}

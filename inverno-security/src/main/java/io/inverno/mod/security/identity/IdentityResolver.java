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
package io.inverno.mod.security.identity;

import io.inverno.mod.security.authentication.Authentication;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An identity resolver is used to resolve the identity of an authenticated entity from an {@link Authentication}.
 * </p>
 * 
 * <p>
 * An authentication basically identifies an authenticated entity in an application, it basically proves that the credentials of an entity have been authenticated, an identity resolver resolves the
 * identity of the authenticated user.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of authentication
 * @param <B> the type of identity
 */
@FunctionalInterface
public interface IdentityResolver<A extends Authentication, B extends Identity> {

	/**
	 * <p>
	 * Resolves the identity of the authenticated entity from the specified authentication.
	 * </p>
	 * 
	 * @param authentication an authentication
	 * 
	 * @return a mono emitting the resolved identity or an empty mono if no identity could have been resolved
	 * 
	 * @throws IdentityException of there was an error resolving the identity
	 */
	Mono<B> resolveIdentity(A authentication) throws IdentityException;
	
	/**
	 * <p>
	 * Invokes this identity resolver and then transforms the resulting identity publisher.
	 * </p>
	 * 
	 * @param <T> the type of the resulting identity
	 * @param mapper the function to transform the identity publisher
	 * 
	 * @return a transformed identity resolver
	 */
	default <T extends Identity> IdentityResolver<A, T> flatMap(Function<? super B, ? extends Mono<? extends T>> mapper) {
		return identity -> this.resolveIdentity(identity).flatMap(mapper);
	}
	
		/**
	 * <p>
	 * Invokes this identity resolver and then transforms the resulting identity.
	 * </p>
	 * 
	 * @param <T> the type of the resulting identity
	 * @param mapper the function to transform the identity
	 * 
	 * @return a transformed identity resolver
	 */
	default <T extends Identity> IdentityResolver<A, T> map(Function<? super B, ? extends T> mapper) {
		return identity -> this.resolveIdentity(identity).map(mapper);
	}
}

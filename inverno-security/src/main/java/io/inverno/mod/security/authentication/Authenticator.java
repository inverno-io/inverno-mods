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

import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An authenticator is used to authenticate the {@link Credentials} of an entity that wants to access protected services or resources.
 * </p>
 *
 * <p>
 * The {@link Authentication} returned by the {@link #authenticate(io.inverno.mod.security.authentication.Credentials) } method represents a proof of authentication, namely that the entity credentials
 * have been validated.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of credentials
 * @param <B> the type of authentication
 */
@FunctionalInterface
public interface Authenticator<A extends Credentials, B extends Authentication> {

	/**
	 * <p>
	 * Authenticates the specified credentials and returns an authentication when valid.
	 * </p>
	 *
	 * <p>
	 * Implementations must return an empty mono to indicate that they were unable to authenticate the credentials. This does not mean credentials are invalid, this simply mean that a particular
	 * authenticator does not manage them and therefore can's possibly determine whether they are valid. For example, when considering login credentials composed of a user and a password, an
	 * authenticator which does not manage that particular user must return an empty mono.
	 * </p>
	 * 
	 * <p>
	 * Implementations must throw an {@link AuthenticationException} when they were able to authenticate the credentials which turned out to be invalid. For example, an authenticator must throw such
	 * exception when it does manage a particular and that the password was invalid.
	 * </p>
	 * 
	 * @param credentials the credentials to authenticate
	 *
	 * @return a mono emitting an authentication or an empty mono if the authenticator could not authenticate the credentials
	 *
	 * @throws AuthenticationException if credentials are invalid
	 */
	Mono<B> authenticate(A credentials) throws AuthenticationException;

	/**
	 * <p>
	 * Returns a composed authenticator which first invokes this authenticator and, if the credentials could not be authenticated, invokes the specified authenticator.
	 * </p>
	 *
	 * @param other the authenticator to invoke in case this authenticator was not able to authenticate credentials
	 *
	 * @return a composed authenticator
	 */
	default Authenticator<A, B> or(Authenticator<? super A, ? extends B> other) {
		return credentials -> {
			return this.authenticate(credentials).switchIfEmpty(other.authenticate(credentials));
		};
	}

	/**
	 * <p>
	 * Invokes this authenticator and then transforms the resulting authentication publisher.
	 * </p>
	 * 
	 * @param <T> the type of the resulting authentication
	 * @param mapper the function to transform the authentication publisher
	 * 
	 * @return a transformed authentiator
	 */
	default <T extends Authentication> Authenticator<A, T> flatMap(Function<? super B, ? extends Mono<? extends T>> mapper) {
		return credentials -> {
			return this.authenticate(credentials).flatMap(mapper);
		};
	}

	/**
	 * <p>
	 * Invokes this authenticator and then transforms the resulting authentication.
	 * </p>
	 * 
	 * @param <T> the type of the resulting authentication
	 * @param mapper the function to transform the authentication
	 * 
	 * @return a transformed authentiator
	 */
	default <T extends Authentication> Authenticator<A, T> map(Function<? super B, ? extends T> mapper) {
		return credentials -> {
			return this.authenticate(credentials).map(mapper);
		};
	}
}

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
	 * Authenticates the specified credentials and returns an authentication.
	 * </p>
	 *
	 * <p>
	 * Implementations can return an empty mono to indicate that they were unable to authenticate the credentials. This does not mean credentials are invalid, this simply mean that a particular
	 * authenticator does not manage them and therefore can's possibly determine whether they are valid. For example, when considering login credentials composed of a user and a password, an
	 * authenticator which does not manage that particular user can return an empty mono.
	 * </p>
	 * 
	 * <p>
	 * Implementations must return denied authentications with {@link AuthenticationException} when they were able to authenticate credentials which turned out to be invalid. For example, a login
	 * credentials authenticator must return a denied authentication exception when it does manage a particular username but the provided password was invalid.
	 * </p>
	 * 
	 * <p>
	 * A denied authentication can also bre reported by throwing an {@link AuthenticationException} when returning an actual authentication instance is not practical.
	 * </p>
	 * 
	 * @param credentials the credentials to authenticate
	 *
	 * @return a mono emitting an authentication, an error mono or an empty mono if the authenticator could not authenticate the credentials
	 * 
	 * @throws AuthenticationException if credentials were invalid
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
		return credentials -> this.authenticate(credentials).switchIfEmpty(other.authenticate(credentials));
	}

	/**
	 * <p>
	 * Invokes this authenticator and then transforms the resulting authentication publisher.
	 * </p>
	 * 
	 * @param <T> the type of the resulting authentication
	 * @param mapper the function to transform the authentication publisher
	 * 
	 * @return a transformed authenticator
	 */
	default <T extends Authentication> Authenticator<A, T> flatMap(Function<? super B, ? extends Mono<? extends T>> mapper) {
		return credentials -> this.authenticate(credentials).flatMap(mapper);
	}

	/**
	 * <p>
	 * Invokes this authenticator and then transforms the resulting authentication.
	 * </p>
	 * 
	 * @param <T> the type of the resulting authentication
	 * @param mapper the function to transform the authentication
	 * 
	 * @return a transformed authenticator
	 */
	default <T extends Authentication> Authenticator<A, T> map(Function<? super B, ? extends T> mapper) {
		return credentials -> this.authenticate(credentials).map(mapper);
	}
	
	/**
	 * <p>
	 * Transforms the authenticator so it fails on denied authentications.
	 * </p>
	 * 
	 * <p>
	 * An authenticator is supposed to return a denied authentication in case of failed authentication, however this might not always be possible or convenient, especially when transforming
	 * authentication output using {@link #map(java.util.function.Function) } or {@link #flatMap(java.util.function.Function) } operators. As consequence, it might be desirable to actually propagate
	 * the original authentication error when a denied authentication is returned by the authenticator.
	 * </p>
	 * 
	 * @return an authenticator that returns an error mono on denied authentications
	 */
	default Authenticator<A, B> failOnDenied() {
		return credentials -> this.authenticate(credentials)
			.doOnNext(authentication -> authentication.getCause().ifPresent(e -> {
				throw e;
			}));
	}
	
	/**
	 * <p>
	 * Transforms the authenticator so it fails on denied and anonymous authentications.
	 * </p>
	 * 
	 * <p>
	 * As for {@link #failOnDenied() }, an authenticator can return a denied or an anonymous authentication, this operator allows to throw a corresponding {@link AuthenticationException} to stop a
	 * subsequent authentication transformation chain instead of dealing with denied and anonymous authentication when mapping the authentication output.
	 * </p>
	 * 
	 * @return an authenticator that returns an error mono on denied and anonymous authentications
	 */
	default Authenticator<A, B> failOnDeniedAndAnonymous() {
		return credentials -> this.authenticate(credentials)
			.doOnNext(authentication -> authentication.getCause().ifPresentOrElse(
				e -> {
					throw e;
				},
				() -> {
					if(!authentication.isAuthenticated()) {
						throw new AuthenticationException("Anonymous authentication not allowed");
					}
				}
			));
	}
}

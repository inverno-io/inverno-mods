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
package io.inverno.mod.security.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.security.SecurityManager;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.http.internal.GenericSecurityInterceptor;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;

/**
 * <p>
 * The security interceptor extracts the credentials send by a requester, authenticates them and creates the security context in the exchange.
 * </p>
 * 
 * <p>
 * This is the main security interceptor that must be used on protectected resources. It authenticates the request and creates the {@link SecurityContext} in the exchange which can later be used in a
 * {@link AccessControlInterceptor} or directly in the exchange handler to control the access to the resource.
 * </p>
 * 
 * <p>
 * Just like a {@link SecurityManager}, it relies on an {@link Authenticator} to authenticate credentials extracted from the request by a {@link CredentialsExtractor}, an {@link IdentityResolver} to
 * resolve the identity of the authenticated entity and an {@link AccessControllerResolver} to get the access controller for the authenticated entity in order to create the {@link SecurityContext}
 * which is set into the context of the exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see SecurityManager
 * @see AccessControllerResolver
 * 
 * @param <A> the credentials type
 * @param <B> the identity type
 * @param <C> the access controller type
 * @param <D> the intercepting security context type
 * @param <E> the exchange type
 */
public interface SecurityInterceptor<A extends Credentials, B extends Identity, C extends AccessController, D extends InterceptingSecurityContext<B, C>, E extends Exchange<D>> extends ExchangeInterceptor<D, E> {
	
	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor and authenticator.
	 * </p>
	 *
	 * @param <A>                  the credentials type
	 * @param <B>                  the authentication type
	 * @param <C>                  the identity type
	 * @param <D>                  the access controller type
	 * @param <E>                  the intercepting security context type
	 * @param <F>                  the exchange type
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * 
	 * @return a new security interceptor
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController, E extends InterceptingSecurityContext<C, D>, F extends Exchange<E>> SecurityInterceptor<A, C, D, E, F> of(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator) {
		return new GenericSecurityInterceptor<>(credentialsExtractor, authenticator);
	}
	
	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor, authenticator and identity resolver.
	 * </p>
	 *
	 * @param <A>                  the credentials type
	 * @param <B>                  the authentication type
	 * @param <C>                  the identity type
	 * @param <D>                  the access controller type
	 * @param <E>                  the intercepting security context type
	 * @param <F>                  the exchange type
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * @param identityResolver     an identity resolver
	 * 
	 * @return a new security interceptor
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController, E extends InterceptingSecurityContext<C, D>, F extends Exchange<E>> SecurityInterceptor<A, C, D, E, F> of(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver) {
		return new GenericSecurityInterceptor<>(credentialsExtractor, authenticator, identityResolver);
	}
	
	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor, authenticator and access controller resolver.
	 * </p>
	 *
	 * @param <A>                  the credentials type
	 * @param <B>                  the authentication type
	 * @param <C>                  the identity type
	 * @param <D>                  the access controller type
	 * @param <E>                  the intercepting security context type
	 * @param <F>                  the exchange type
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * @param accessControllerResolver     an access controller resolver
	 * 
	 * @return a new security interceptor
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController, E extends InterceptingSecurityContext<C, D>, F extends Exchange<E>> SecurityInterceptor<A, C, D, E, F> of(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		return new GenericSecurityInterceptor<>(credentialsExtractor, authenticator, accessControllerResolver);
	}
	
	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor, authenticator, identity resolver and access controller resolver.
	 * </p>
	 *
	 * @param <A>                  the credentials type
	 * @param <B>                  the authentication type
	 * @param <C>                  the identity type
	 * @param <D>                  the access controller type
	 * @param <E>                  the intercepting security context type
	 * @param <F>                  the exchange type
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * @param identityResolver     an identity resolver
	 * @param accessControllerResolver     an access controller resolver
	 * 
	 * @return a new security interceptor
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController, E extends InterceptingSecurityContext<C, D>, F extends Exchange<E>> SecurityInterceptor<A, C, D, E, F> of(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		return new GenericSecurityInterceptor<>(credentialsExtractor, authenticator, identityResolver, accessControllerResolver);
	}
}

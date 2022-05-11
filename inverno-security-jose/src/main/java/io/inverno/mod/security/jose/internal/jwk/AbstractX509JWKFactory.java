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
package io.inverno.mod.security.jose.internal.jwk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.X509JWKFactory;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * <p>
 * Base X.509 JSON Web Key factory implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the X.509 JWK type
 * @param <D> the X.509 JWK builder type
 * @param <E> the X.509 JWK generator type
 */
public abstract class AbstractX509JWKFactory<A extends PublicKey, B extends PrivateKey, C extends AbstractX509JWK<A, B>, D extends AbstractX509JWKBuilder<A, B, C, D>, E extends AbstractX509JWKGenerator<A, B, C, E>> extends AbstractJWKFactory<C, D, E> implements X509JWKFactory<A, B, C, D ,E> {

	/**
	 * The JWK URL resolver.
	 */
	protected final JWKURLResolver urlResolver;
	
	/**
	 * The X.509 certificate path validator.
	 */
	protected final X509JWKCertPathValidator certPathValidator;
	
	/**
	 * <p>
	 * Creates an X.509 JWK factory.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param mapper            an object mapper
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public AbstractX509JWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		super(configuration, jwkStore, keyResolver, mapper);
		this.urlResolver = urlResolver;
		this.certPathValidator = certPathValidator;
	}
}

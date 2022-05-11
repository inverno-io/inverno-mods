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

import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.X509JWK;
import io.inverno.mod.security.jose.jwk.X509JWKGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

/**
 * <p>
 * Base X.509 JSON Web Key generator implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the X.509 JWK type
 * @param <D> the X.509 JWK generator type
 */
public abstract class AbstractX509JWKGenerator<A extends PublicKey, B extends PrivateKey, C extends X509JWK<A, B>, D extends AbstractX509JWKGenerator<A, B, C, D>> extends AbstractJWKGenerator<C, D> implements X509JWKGenerator<A, B, C, D> {

	/**
	 * <p>
	 * Creates an X.509 JWK generator.
	 * </p>
	 */
	public AbstractX509JWKGenerator() {
		super();
	}

	/**
	 * <p>
	 * Creates an X.509 JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public AbstractX509JWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
		super(parameters);
	}
}

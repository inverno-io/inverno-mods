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
package io.inverno.mod.security.jose.internal.jwk.okp;

import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKGenerator;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.okp.OKPJWKGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

/**
 * <p>
 * Base Octet Key Pair JSON Web Key generator implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the Octet Key Pair JWK type
 * @param <D> the Octet Key Pair JWK generator type
 * 
 */
public abstract class AbstractOKPJWKGenerator<A extends PublicKey, B extends PrivateKey, C extends AbstractOKPJWK<A, B>, D extends AbstractOKPJWKGenerator<A, B, C, D>> extends AbstractX509JWKGenerator<A, B, C, D> implements OKPJWKGenerator<A, B, C, D> {
	
	/**
	 * The Curve parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3">RFC8037 Section 3</a>.
	 */
	protected OKPCurve curve;
	
	/**
	 * <p>
	 * Creates an OKP JWK generator.
	 * </p>
	 */
	public AbstractOKPJWKGenerator() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates an OKP JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public AbstractOKPJWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
		super(parameters);
	}
	
	@Override
	protected void set(String field, Object value) throws JWKGenerateException {
		switch(field) {
			case "crv": {
				this.curve((String)value);
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public D curve(String crv) {
		this.curve = OKPCurve.fromCurve(crv);
		return (D)this;
	}
}

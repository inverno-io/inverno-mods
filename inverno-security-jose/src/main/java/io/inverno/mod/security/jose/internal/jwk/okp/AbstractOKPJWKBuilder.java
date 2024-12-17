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

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKBuilder;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.okp.OKPJWK;
import io.inverno.mod.security.jose.jwk.okp.OKPJWKBuilder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base Octet Key Pair JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the Octet Key Pair JWK type
 * @param <D> the Octet Key Pair JWK builder type
 */
public abstract class AbstractOKPJWKBuilder<A extends PublicKey, B extends PrivateKey, C extends OKPJWK<A, B>, D extends AbstractOKPJWKBuilder<A, B, C, D>> extends AbstractX509JWKBuilder<A, B, C, D> implements OKPJWKBuilder<A, B, C, D> {
	
	/**
	 * The Curve parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3">RFC8037 Section 3</a>.
	 */
	protected OKPCurve curve;
	
	/**
	 * The public key parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3">RFC8037 Section 3</a>.
	 */
	protected String x;
	
	/**
	 * The private key parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3">RFC8037 Section 3</a>.
	 */
	protected String d;
	
	/**
	 * <p>
	 * Creates a generic OKP JWK builder.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public AbstractOKPJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		this(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, null);
	}
	
	/**
	 * <p>
	 * Creates a generic OKP JWK builder initialized with the specified parameters map.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 * @param parameters        a parameters map used to initialize the builder
	 * 
	 * @throws JWKReadException if there was an error reading the parameters map
	 */
	public AbstractOKPJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, urlResolver, certPathValidator, parameters);
	}
	
	@Override
	protected void set(String field, Object value) throws JWKReadException {
		switch(field) {
			case "crv": {
				this.curve((String)value);
				break;
			}
			case "x": {
				this.publicKey((String)value);
				break;
			}
			case "d": {
				this.privateKey((String)value);
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

	@Override
	@SuppressWarnings("unchecked")
	public D publicKey(String x) {
		this.x = x;
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D privateKey(String d) {
		this.d = d;
		return (D)this;
	}
	
	@Override
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		String thumbprint = AbstractOKPJWK.toJWKThumbprint(JWK.DEFAULT_THUMBPRINT_DIGEST, this.curve != null ? this.curve.getCurve() : null, OKPJWK.KEY_TYPE, this.x);
		return super.resolveFromJWKStore().switchIfEmpty(Mono.defer(() -> this.jwkStore.getByJWKThumbprint(thumbprint))).doOnNext(jwk -> {
			if(!(jwk instanceof OKPJWK)) {
				throw new JWKResolveException("Stored JWK is not of expected type: " + OKPJWK.class);
			}
			OKPJWK<?, ?> okpJWK = (OKPJWK<?, ?>)jwk;
			if((this.curve != null && !this.curve.getCurve().equals(okpJWK.getCurve())) || 
				(this.x != null && !this.x.equals(okpJWK.getPublicKey())) || 
				(this.d != null && !this.d.equals(okpJWK.getPrivateKey())) 
			) {
				throw new JWKResolveException("JWK parameters does not match stored JWK");
			}
		});
	}
	
	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve().then(Mono.fromRunnable(() -> {
			if(this.curve == null) {
				throw new JWKBuildException("Curve is null");
			}

			if(StringUtils.isBlank(this.x)) {
				throw new JWKBuildException("Public key is blank");
			}
		}));
	}
}

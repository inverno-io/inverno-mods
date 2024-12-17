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
package io.inverno.mod.security.jose.internal.jwk.pbes2;

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWKBuilder;
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWK;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWKBuilder;
import java.security.Key;
import java.util.Map;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Password-based JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericPBES2JWKBuilder extends AbstractJWKBuilder<GenericPBES2JWK, GenericPBES2JWKBuilder> implements PBES2JWKBuilder<GenericPBES2JWK, GenericPBES2JWKBuilder> {

	private PBES2Algorithm pbes2Alg;
	
	private String p;
	
	/**
	 * <p>
	 * Creates a generic PBES2 JWK builder.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 */
	public GenericPBES2JWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver) {
		super(configuration, jwkStore, keyResolver);
	}

	/**
	 * <p>
	 * Creates a generic PBES2 JWK builder initialized with the specified parameters map.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param parameters        a parameters map used to initialize the builder
	 * 
	 * @throws JWKReadException if there was an error reading the parameters map
	 */
	public GenericPBES2JWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, parameters);
	}

	@Override
	protected void set(String field, Object value) throws JWKReadException {
		if(field.equals("p")) {
			this.password((String) value);
		}
		else {
			super.set(field, value);
		}
	}
	
	@Override
	public GenericPBES2JWKBuilder algorithm(String alg) {
		this.pbes2Alg = alg != null ? PBES2Algorithm.fromAlgorithm(alg) : null;
		return super.algorithm(alg);
	}

	@Override
	public GenericPBES2JWKBuilder password(String password) {
		this.p = password;
		return this;
	}
	
	@Override
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		String thumbprint = GenericPBES2JWK.toJWKThumbprint(JWK.DEFAULT_THUMBPRINT_DIGEST, this.p, OCTJWK.KEY_TYPE);
		return super.resolveFromJWKStore().switchIfEmpty(Mono.defer(() -> this.jwkStore.getByJWKThumbprint(thumbprint))).doOnNext(jwk -> {
			if(!(jwk instanceof PBES2JWK)) {
				throw new JWKResolveException("Stored JWK is not of expected type: " + PBES2JWK.class);
			}
			PBES2JWK pbes2JWK = (PBES2JWK)jwk;
			if(this.p != null && !this.p.equals(pbes2JWK.getPassword())) {
				throw new JWKResolveException("JWK parameters does not match stored JWK");
			}
		});
	}
	
	@Override
	protected Mono<Void> resolveKey(Key key) throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.justOrEmpty(key)
			.flatMap(tmpKey -> {
				if(!(tmpKey instanceof SecretKey)) {
					throw new JWKBuildException("Key is not a secret key");
				}
				SecretKey passwordKey = (SecretKey)tmpKey;
				
				if(this.p == null) {
					this.p = new String(passwordKey.getEncoded());
				}
				else if(!new String(passwordKey.getEncoded()).equals(this.p)) {
					throw new JWKBuildException("Resolved password key does not match JWK parameters");
				}
				return Mono.empty();
			});
	}

	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve().then(Mono.fromRunnable(() -> {
			if(StringUtils.isBlank(this.p)) {
				throw new JWKBuildException("Password is blank");
			}
		}));
	}
	
	@Override
	protected Mono<GenericPBES2JWK> doBuild() throws JWKBuildException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			GenericPBES2JWK jwk = new GenericPBES2JWK(this.p, (SecretKey)this.key, this.key != null);
			jwk.setPublicKeyUse(this.use);
			jwk.setKeyOperations(this.key_ops);
			jwk.setAlgorithm(this.pbes2Alg);
			jwk.setKeyId(this.kid);
			
			return jwk;
		});
	}
}

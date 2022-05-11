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
package io.inverno.mod.security.jose.internal.jwk.oct;

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWKBuilder;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWKBuilder;
import java.math.BigInteger;
import java.security.Key;
import java.util.Base64;
import java.util.Map;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Octet JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericOCTJWKBuilder extends AbstractJWKBuilder<GenericOCTJWK, GenericOCTJWKBuilder> implements OCTJWKBuilder<GenericOCTJWK, GenericOCTJWKBuilder> {

	private OCTAlgorithm octAlg;
	
	private String k;

	/**
	 * <p>
	 * Creates a generic OCT JWK builder.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 */
	public GenericOCTJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver) {
		this(configuration, jwkStore, keyResolver, null);
	}
	
	/**
	 * <p>
	 * Creates a generic OCT JWK builder initialized with the specified parameters map.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param parameters        a parameters map used to initialize the builder
	 * 
	 * @throws JWKReadException if there was an error reading the parameters map
	 */
	public GenericOCTJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, Map<String, Object> parameters) throws JWKReadException {
		super(configuration, jwkStore, keyResolver, parameters);
	}
	
	@Override
	protected void set(String field, Object value) throws JWKReadException {
		switch(field) {
			case "k" : {
				this.keyValue((String)value);
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}
	
	@Override
	public GenericOCTJWKBuilder algorithm(String alg) {
		this.octAlg = alg != null ? OCTAlgorithm.fromAlgorithm(alg) : null;
		return super.algorithm(alg);
	}
	
	@Override
	public GenericOCTJWKBuilder keyValue(String k) {
		this.k = k;
		return this;
	}
	
	@Override
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		String thumbprint = GenericOCTJWK.toJWKThumbprint(JWK.DEFAULT_THUMBPRINT_DIGEST, this.k, OCTJWK.KEY_TYPE);
		return super.resolveFromJWKStore().switchIfEmpty(Mono.defer(() -> this.jwkStore.getByJWKThumbprint(thumbprint))).doOnNext(jwk -> {
			if(!(jwk instanceof OCTJWK)) {
				throw new JWKResolveException("Stored JWK is not of expected type: " + OCTJWK.class);
			}
			OCTJWK octJWK = (OCTJWK)jwk;
			if(this.k != null && !this.k.equals(octJWK.getKeyValue())) {
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
				SecretKey secretKey = (SecretKey)tmpKey;
				
				if(this.k == null) {
					this.k = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(secretKey.getEncoded());
				}
				else if(!new BigInteger(secretKey.getEncoded()).equals(new BigInteger(Base64.getUrlDecoder().decode(this.k)))) {
					throw new JWKBuildException("Resolved secret key does not match JWK parameters");
				}
				
				return Mono.empty();
			});
	}
	
	@Override
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return super.resolve().then(Mono.fromRunnable(() -> {
			if(StringUtils.isBlank(this.k)) {
				throw new JWKBuildException("Key value is blank");
			}
		}));
	}

	@Override
	public Mono<GenericOCTJWK> doBuild() throws JWKBuildException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			GenericOCTJWK jwk = new GenericOCTJWK(this.k, (SecretKey)this.key, this.key != null);
			jwk.setPublicKeyUse(this.use);
			jwk.setKeyOperations(this.key_ops);
			jwk.setAlgorithm(this.octAlg);
			jwk.setKeyId(this.kid);
			
			return jwk;
		});
	}
}

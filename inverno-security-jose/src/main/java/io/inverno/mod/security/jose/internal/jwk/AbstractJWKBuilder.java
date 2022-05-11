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

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKBuilder;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base JSON Web Key builder implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type
 * @param <B> the JWK builder type
 */
public abstract class AbstractJWKBuilder<A extends JWK, B extends AbstractJWKBuilder<A,B>> implements JWKBuilder<A, B>, Cloneable {
	
	private static final Set<String> SIG_OPERATIONS = Set.of(JWK.KEY_OP_SIGN, JWK.KEY_OP_VERIFY);
	private static final Set<String> ENC_OPERATIONS = Set.of(JWK.KEY_OP_ENCRYPT, JWK.KEY_OP_DECRYPT, JWK.KEY_OP_WRAP_KEY, JWK.KEY_OP_UNWRAP_KEY);

	/**
	 * The JOSE module configuration.
	 */
	protected final JOSEConfiguration configuration;
	
	/**
	 * The JWK Key store.
	 */
	protected final JWKStore jwkStore;
	
	/**
	 * The JWK Key resolver.
	 */
	protected final JWKKeyResolver keyResolver;
	
	/**
	 * The Public Key Use parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.2">RFC7517 Section 4.2</a>.
	 */
	protected String use;
	
	/**
	 * The Key Operations parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.3">RFC7517 Section 4.3</a>.
	 */
	protected Set<String> key_ops;
	
	/**
	 * The Algorithm parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.4">RFC7517 Section 4.4</a>.
	 */
	protected String alg;
	
	/**
	 * The Key id parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7517#section-4.5">RFC7517 Section 4.5</a>.
	 */
	protected String kid;
	
	/**
	 * The underlying key.
	 */
	protected volatile Key key;

	/**
	 * <p>
	 * Creates a JWK builder.
	 * </p>
	 *
	 * @param configuration the JOSE module configuration
	 * @param jwkStore      a JWK store
	 * @param keyResolver   a JWK key resolver
	 */
	public AbstractJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver) {
		this(configuration, jwkStore, keyResolver, null);
	}
	
	/**
	 * <p>
	 * Creates a JWK builder initialized with the specified parameters map.
	 * </p>
	 *
	 * @param configuration the JOSE module configuration
	 * @param jwkStore      a JWK store
	 * @param keyResolver   a JWK key resolver
	 * @param parameters    a parameters map used to initialize the builder
	 *
	 * @throws JWKReadException if there was an error reading the parameters map
	 */
	public AbstractJWKBuilder(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, Map<String, Object> parameters) throws JWKReadException {
		this.configuration = configuration;
		this.jwkStore = jwkStore;
		this.keyResolver = keyResolver;
		
		if(parameters != null) {
			parameters.forEach((k, v) -> {
				if(v != null) {
					this.set(k, v);
				}
			});
		}
	}
	
	/**
	 * <p>
	 * Sets the specified parameter into the builder.
	 * </p>
	 * 
	 * <p>
	 * Unsupported parameters are ignored.
	 * </p>
	 *
	 * @param name  the parameter name
	 * @param value the parameter value
	 *
	 * @throws JWKReadException if there was an error reading the value
	 */
	@SuppressWarnings("unchecked")
	protected void set(String name, Object value) throws JWKReadException {
		switch(name) {
			case "alg": {
				this.algorithm((String)value);
				break;
			}
			case "kid": {
				this.keyId((String)value);
				break;
			}
			case "use": {
				this.publicKeyUse((String)value);
				break;
			}
			case "key_ops": {
				if(value instanceof String[]) {
					this.keyOperations((String[])value);
				}
				else if(value instanceof Collection) {
					this.keyOperations(((Collection<String>)value).toArray(String[]::new));
				}
				else {
					throw new JWKReadException(value.getClass() + " can't be converted to String[]");
				}
				break;
			}
			default: {
				break;
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public B publicKeyUse(String use) {
		this.use = use;
		return (B)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public B keyOperations(String... key_ops) {
		if(key_ops == null || key_ops.length == 0) {
			this.key_ops = null;
		}
		else {
			this.key_ops = Arrays.stream(key_ops).filter(Objects::nonNull).collect(Collectors.toSet());
		}
		return (B)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public B algorithm(String alg) {
		this.alg = alg;
		return (B)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public B keyId(String kid) {
		this.kid = kid;
		return (B)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<A> build() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return ((Mono<A>)this.resolveFromJWKStore()).switchIfEmpty(Mono.defer(() -> {
			try {
				AbstractJWKBuilder<A,B> clone = ((AbstractJWKBuilder<A,B>)this.clone());
				return clone.resolve().then(clone.doBuild());
			} 
			catch(CloneNotSupportedException e) {
				throw new JWKBuildException("Error cloning JWK builder", e);
			}
		}));
	}
	
	/**
	 * <p>
	 * Tries to resolve the JWK from the JWK store.
	 * </p>
	 * 
	 * @return a single JWK publisher or an empty publisher if there's no JWK corresponding to the builder's parameters in the JWK store
	 * 
	 * @throws JWKResolveException if there was an error accessing the JWK store
	 */
	protected Mono<JWK> resolveFromJWKStore() throws JWKResolveException {
		return this.jwkStore.getByKeyId(this.kid);
	}
	
	/**
	 * <p>
	 * Resolves the JWK to build.
	 * </p>
	 * 
	 * <p>
	 * This method basically resolves resources such as keys or certificates and verifies that the builder's parameters are consistent.
	 * </p>
	 * 
	 * @return an empty single publisher which completes in error if the key is not consistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the JWK
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	protected Mono<Void> resolve() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return Mono.fromRunnable(() -> {
				if(this.use != null && this.key_ops != null && !this.key_ops.isEmpty() && !(this.use.equals(JWK.USE_SIG) && SIG_OPERATIONS.containsAll(this.key_ops)) && !(this.use.equals(JWK.USE_ENC) && ENC_OPERATIONS.containsAll(this.key_ops)) ) {
					throw new JWKBuildException("Key operations [" + this.key_ops.stream().collect(Collectors.joining(", ")) +"] are inconsistent with public key use " + this.use);
				}
			})
			.then(this.resolveKid());
	}
	
	/**
	 * <p>
	 * Resolves the specified key into the builder.
	 * </p>
	 *
	 * <p>
	 * This method basically verifies that the key is valid and consistent with the builder's parameters and eventually populates the builder with the key.
	 * </p>
	 *
	 * @param key a key
	 *
	 * @return an empty single publisher which completes in error if the key is not consistent with the builder's parameters
	 *
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the key
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	protected abstract Mono<Void> resolveKey(Key key) throws JWKBuildException, JWKResolveException, JWKProcessingException;
	
	/**
	 * <p>
	 * Tries to resolve the key identified by the key id using the JWK key resolver.
	 * </p>
	 * 
	 * <p>
	 * This method queries the key resolver and delegates the implementation specific key resolution to {@link #resolveKey(java.security.Key)}.
	 * </p>
	 * 
	 * @return an empty single publisher which completes in error if the resolved key is invalid or inconsistent with the builder's parameters
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKResolveException    if there was an error resolving the key
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	private Mono<Void> resolveKid() throws JWKBuildException, JWKResolveException, JWKProcessingException {
		return this.keyResolver.resolveKeyFromKeyId(this.kid)
			.flatMap(tmpKey -> this.resolveKey(tmpKey).doOnSuccess(ign -> this.key = tmpKey));
	}
	
	/**
	 * <p>
	 * Builds the JWK after all checks and processing have terminated successfully.
	 * </p>
	 * 
	 * @return a single JWK publisher
	 * 
	 * @throws JWKBuildException      if there was an error building the JWK
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	protected abstract Mono<A> doBuild() throws JWKBuildException, JWKProcessingException;
}

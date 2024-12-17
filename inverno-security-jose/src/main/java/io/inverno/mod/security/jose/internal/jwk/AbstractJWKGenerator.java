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

import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKGenerator;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base JSON Web Key generator implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type
 * @param <B> the JWK generator type
 */
public abstract class AbstractJWKGenerator<A extends JWK, B extends AbstractJWKGenerator<A,B>> implements JWKGenerator<A, B> {

	private static final Set<String> SIG_OPERATIONS = Set.of(JWK.KEY_OP_SIGN, JWK.KEY_OP_VERIFY);
	private static final Set<String> ENC_OPERATIONS = Set.of(JWK.KEY_OP_ENCRYPT, JWK.KEY_OP_DECRYPT, JWK.KEY_OP_WRAP_KEY, JWK.KEY_OP_UNWRAP_KEY);
	
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
	 * <p>
	 * Creates JWK generator.
	 * </p>
	 */
	public AbstractJWKGenerator() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public AbstractJWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
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
	 * Sets the specified parameter into the generator.
	 * </p>
	 * 
	 * <p>
	 * Unsupported parameters are ignored.
	 * </p>
	 *
	 * @param name  the parameter name
	 * @param value the parameter value
	 *
	 * @throws JWKGenerateException if there was an error reading the value
	 */
	@SuppressWarnings("unchecked")
	protected void set(String name, Object value) throws JWKGenerateException {
		switch(name) {
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
					throw new JWKGenerateException(value.getClass() + " can't be converted to String[]");
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
			return (B)this;
		}
		else {
			this.key_ops = new HashSet<>();
			for(int i=0;i<key_ops.length;i++) {
				final int current = i;
				Objects.requireNonNull(key_ops[i], () -> "Null key_op at index " + current);
				this.key_ops.add(key_ops[i]);
			}
		}
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
	public B algorithm(String alg) {
		this.alg = alg;
		return (B)this;
	}

	@Override
	public Mono<A> generate() throws JWKGenerateException, JWKProcessingException {
		return this.verify().then(this.doGenerate());
	}
	
	/**
	 * <p>
	 * Verifies that the generator's parameters are consistent and that we can proceed with the generation of the key.
	 * </p>
	 *
	 * @return an empty single publisher which completes in error if generator's parameters are invalid or inconsistent
	 *
	 * @throws JWKGenerateException   if there was an error generating the JWK
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	protected Mono<Void> verify() throws JWKGenerateException, JWKProcessingException {
		return Mono.fromRunnable(() -> {
			if(this.use != null && this.key_ops != null && !this.key_ops.isEmpty() && !(this.use.equals(JWK.USE_SIG) && SIG_OPERATIONS.containsAll(this.key_ops)) && !(this.use.equals(JWK.USE_ENC) && ENC_OPERATIONS.containsAll(this.key_ops)) ) {
				throw new JWKGenerateException("Key operations [" + String.join(", ", this.key_ops) +"] are inconsistent with public key use " + this.use);
			}
		});
	}
	
	/**
	 * <p>
	 * Generates the JWK after all checks and processing have terminated successfully.
	 * </p>
	 *
	 * @return a single JWK publisher
	 *
	 * @throws JWKGenerateException   if there was an error generating the JWK
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	protected abstract Mono<A> doGenerate() throws JWKGenerateException, JWKProcessingException;
}

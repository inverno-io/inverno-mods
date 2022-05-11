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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKFactory;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base JSON Web Key factory implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type
 * @param <B> the JWK builder type
 * @param <C> the JWK generator type
 */
public abstract class AbstractJWKFactory<A extends AbstractJWK, B extends AbstractJWKBuilder<A, B>, C extends AbstractJWKGenerator<A, C>> implements JWKFactory<A, B, C> {

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
	 * The object mapper.
	 */
	protected final ObjectMapper mapper;
	
	/**
	 * <p>
	 * Creates a JWK factory
	 * </p>
	 *
	 * @param configuration the JOSE module configuration
	 * @param jwkStore      a JWK store
	 * @param keyResolver   a JWK key resolver
	 * @param mapper        an object mapper
	 */
	public AbstractJWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper) {
		this.configuration = configuration;
		this.jwkStore = jwkStore;
		this.keyResolver = keyResolver;
		this.mapper = mapper;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Mono<A> read(String jwk) throws JWKReadException, JWKBuildException, JWKResolveException, JWKProcessingException {
		try {
			return this.read((Map<String, Object>)this.mapper.readerFor(Object.class).readValue(jwk));
		} 
		catch(JsonProcessingException e) {
			throw new JWKReadException("Error reading JWK", e);
		}
		catch(ClassCastException e) {
			throw new JWKReadException("Invalid JWK string", e);
		}
	}

	@Override
	public Mono<A> read(Map<String, Object> jwk) throws JWKReadException, JWKBuildException, JWKResolveException, JWKProcessingException {
		return this.builder(jwk).build();
	}
	
	@Override
	public Mono<A> generate(String alg, Map<String, Object> parameters) throws JWKGenerateException, JWKProcessingException {
		return this.generator(alg, parameters).generate();
	}
	
	/**
	 * <p>
	 * Returns a new JWK builder initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map
	 * 
	 * @return a new JWK builder
	 */
	public abstract B builder(Map<String, Object> parameters);
	
	/**
	 * <p>
	 * Returns a new JWK generator initialized with the specified parameters map.
	 * </p>
	 *
	 * @param alg        a JWK algorithm
	 * @param parameters a parameters map
	 *
	 * @return a new JWK generator
	 */
	public abstract C generator(String alg, Map<String, Object> parameters);
}

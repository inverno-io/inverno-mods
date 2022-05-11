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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWKFactory;
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWKFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic Password-based JSON Web Key factory implementation.
 * </p>
 *
 * <p>
 * It supports the {@code OCT} key type and the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>PBES2-HS256+A128KW</li>
 * <li>PBES2-HS384+A192KW</li>
 * <li>PBES2-HS512+A256KW</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class GenericPBES2JWKFactory extends AbstractJWKFactory<GenericPBES2JWK, GenericPBES2JWKBuilder, GenericPBES2JWKGenerator> implements PBES2JWKFactory<GenericPBES2JWK, GenericPBES2JWKBuilder, GenericPBES2JWKGenerator> {

	private static final Set<String> SUPPORTED_ALGORITHMS = Arrays.stream(PBES2Algorithm.values()).map(PBES2Algorithm::getAlgorithm).collect(Collectors.toSet());
	
	/**
	 * <p>
	 * Creates a generic PBES2 JWK factory.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param mapper            an object mapper
	 */
	public GenericPBES2JWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper) {
		super(configuration, jwkStore, keyResolver, mapper);
	}

	@Override
	public boolean supports(String kty) {
		return GenericPBES2JWK.KEY_TYPE.equals(kty);
	}
	
	@Override
	public boolean supportsAlgorithm(String alg) {
		return SUPPORTED_ALGORITHMS.contains(alg);
	}

	@Override
	public GenericPBES2JWKBuilder builder() {
		return new GenericPBES2JWKBuilder(this.configuration, this.jwkStore, this.keyResolver);
	}
	
	@Override
	public GenericPBES2JWKBuilder builder(Map<String, Object> parameters) throws JWKReadException {
		return new GenericPBES2JWKBuilder(this.configuration, this.jwkStore, this.keyResolver, parameters);
	}

	@Override
	public GenericPBES2JWKGenerator generator() {
		return new GenericPBES2JWKGenerator();
	}
	
	@Override
	public GenericPBES2JWKGenerator generator(String alg, Map<String, Object> parameters) throws JWKGenerateException {
		return new GenericPBES2JWKGenerator(parameters);
	}
}

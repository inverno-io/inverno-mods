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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWKFactory;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWKFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic Octet JSON Web Key factory implementation.
 * </p>
 *
 * <p>
 * It supports the {@code OCT} key type and the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>HS256</li>
 * <li>HS384</li>
 * <li>HS512</li>
 * <li>A128KW</li>
 * <li>A256KW</li>
 * <li>A512KW</li>
 * <li>A128GCMKW</li>
 * <li>A192GCMKW</li>
 * <li>A256GCMKW</li>
 * <li>A128GCM</li>
 * <li>A192GCM</li>
 * <li>A256GCM</li>
 * <li>A128CBC-HS256</li>
 * <li>A192CBC-HS384</li>
 * <li>A256CBC-HS512</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class GenericOCTJWKFactory extends AbstractJWKFactory<GenericOCTJWK, GenericOCTJWKBuilder, GenericOCTJWKGenerator> implements OCTJWKFactory<GenericOCTJWK, GenericOCTJWKBuilder, GenericOCTJWKGenerator> {

	private static final Set<String> SUPPORTED_ALGORITHMS = Arrays.stream(OCTAlgorithm.values()).map(OCTAlgorithm::getAlgorithm).collect(Collectors.toSet());
	
	/**
	 * <p>
	 * Creates a generic OCT JWK factory.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param mapper            an object mapper
	 */
	public GenericOCTJWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper) {
		super(configuration, jwkStore, keyResolver, mapper);
	}

	@Override
	public boolean supports(String kty) {
		return OCTJWK.KEY_TYPE.equals(kty);
	}

	@Override
	public boolean supportsAlgorithm(String alg) {
		return SUPPORTED_ALGORITHMS.contains(alg);
	}
	
	@Override
	public GenericOCTJWKBuilder builder() {
		return new GenericOCTJWKBuilder(this.configuration, this.jwkStore, this.keyResolver);
	}
	
	@Override
	public GenericOCTJWKBuilder builder(Map<String, Object> parameters) throws JWKReadException {
		return new GenericOCTJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, parameters);
	}

	@Override
	public GenericOCTJWKGenerator generator() {
		return new GenericOCTJWKGenerator();
	}
	
	@Override
	public GenericOCTJWKGenerator generator(String alg, Map<String, Object> parameters) throws JWKGenerateException {
		return new GenericOCTJWKGenerator(parameters).algorithm(alg);
	}
}

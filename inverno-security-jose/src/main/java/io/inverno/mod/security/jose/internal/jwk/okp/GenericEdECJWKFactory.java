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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.okp.EdECJWKFactory;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic Edward-Curve JWK factory implementation.
 * </p>
 *
 * <p>
 * It supports the {@code OKP} key type and the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>EdDSA with elliptic curve Ed25519 and Ed448.</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class GenericEdECJWKFactory extends AbstractOKPJWKFactory<EdECPublicKey, EdECPrivateKey, GenericEdECJWK, GenericEdECJWKBuilder, GenericEdECJWKGenerator> implements EdECJWKFactory<GenericEdECJWK, GenericEdECJWKBuilder, GenericEdECJWKGenerator> {

	private static final Set<String> SUPPORTED_ALGORITHMS = Arrays.stream(EdECAlgorithm.values()).map(EdECAlgorithm::getAlgorithm).collect(Collectors.toSet());
	
	/**
	 * <p>
	 * Creates a generic EdEC JWK factory.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param mapper            an object mapper
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericEdECJWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper, JWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		super(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
	}
	
	@Override
	public boolean supportsAlgorithm(String alg) {
		return SUPPORTED_ALGORITHMS.contains(alg);
	}

	@Override
	public GenericEdECJWKBuilder builder() {
		return new GenericEdECJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator);
	}
	
	@Override
	public GenericEdECJWKBuilder builder(Map<String, Object> parameters) throws JWKReadException {
		return new GenericEdECJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator, parameters);
	}
	
	@Override
	public GenericEdECJWKGenerator generator() {
		return new GenericEdECJWKGenerator();
	}

	@Override
	public GenericEdECJWKGenerator generator(String alg, Map<String, Object> parameters)  throws JWKGenerateException {
		return new GenericEdECJWKGenerator(parameters).algorithm(alg);
	}
}

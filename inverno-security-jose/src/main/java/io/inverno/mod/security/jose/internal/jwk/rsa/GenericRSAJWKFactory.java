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
package io.inverno.mod.security.jose.internal.jwk.rsa;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKFactory;
import io.inverno.mod.security.jose.internal.jwk.SwitchableJWKURLResolver;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWKFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic RSA JSON Web Key factory implementation.
 * </p>
 * 
 * <p>
 * It supports the {@code RSA} key type and the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>RS1</li>
 * <li>RS256</li>
 * <li>RS384</li>
 * <li>RS512</li>
 * <li>PS256</li>
 * <li>PS384</li>
 * <li>PS512</li>
 * <li>RSA1_5</li>
 * <li>RSA-OAEP</li>
 * <li>RSA-OAEP-256</li>
 * <li>RSA-OAEP-384</li>
 * <li>RSA-OAEP-512</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class GenericRSAJWKFactory extends AbstractX509JWKFactory<RSAPublicKey, RSAPrivateKey, GenericRSAJWK, GenericRSAJWKBuilder, GenericRSAJWKGenerator> implements RSAJWKFactory<GenericRSAJWK, GenericRSAJWKBuilder, GenericRSAJWKGenerator> {

	private static final Set<String> SUPPORTED_ALGORITHMS = Arrays.stream(RSAAlgorithm.values()).map(RSAAlgorithm::getAlgorithm).collect(Collectors.toSet());
	
	/**
	 * <p>
	 * Creates a generic RSA JWK factory.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param mapper            an object mapper
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericRSAJWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper, SwitchableJWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		super(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
	}

	@Override
	public boolean supports(String kty) {
		return RSAJWK.KEY_TYPE.equals(kty);
	}

	@Override
	public boolean supportsAlgorithm(String alg) {
		return SUPPORTED_ALGORITHMS.contains(alg);
	}
	
	@Override
	public GenericRSAJWKBuilder builder() {
		return new GenericRSAJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator);
	}
	
	@Override
	public GenericRSAJWKBuilder builder(Map<String, Object> parameters) throws JWKReadException {
		return new GenericRSAJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator, parameters);
	}

	@Override
	public GenericRSAJWKGenerator generator() {
		return new GenericRSAJWKGenerator();
	}
	
	@Override
	public GenericRSAJWKGenerator generator(String alg, Map<String, Object> parameters) throws JWKGenerateException {
		return new GenericRSAJWKGenerator(parameters).algorithm(alg);
	}
}

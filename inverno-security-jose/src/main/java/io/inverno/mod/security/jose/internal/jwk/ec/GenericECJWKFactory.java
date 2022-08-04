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
package io.inverno.mod.security.jose.internal.jwk.ec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKFactory;
import io.inverno.mod.security.jose.internal.jwk.SwitchableJWKURLResolver;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import io.inverno.mod.security.jose.jwk.ec.ECJWKFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic Elliptic Curve JSON Web Key factory implementation.
 * </p>
 * 
 * <p>
 * It supports the {@code EC} key type and the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>ES256</li>
 * <li>ES384</li>
 * <li>ES512</li>
 * <li>ES256K (deprecated)</li>
 * <li>ECDH-ES with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A128KW with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A192KW with elliptic curve P-256, P-384 or P-521</li>
 * <li>ECDH-ES+A256KW with elliptic curve P-256, P-384 or P-521</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class GenericECJWKFactory extends AbstractX509JWKFactory<ECPublicKey, ECPrivateKey, GenericECJWK, GenericECJWKBuilder, GenericECJWKGenerator> implements ECJWKFactory<GenericECJWK, GenericECJWKBuilder, GenericECJWKGenerator> {

	private static final Set<String> SUPPORTED_ALGORITHMS = Arrays.stream(ECAlgorithm.values()).map(ECAlgorithm::getAlgorithm).collect(Collectors.toSet());
	
	/**
	 * <p>
	 * Creates a generic EC JWK factory.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param mapper            an object mapper
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericECJWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper, SwitchableJWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		super(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
	}

	@Override
	public boolean supports(String kty) {
		return ECJWK.KEY_TYPE.equals(kty);
	}

	@Override
	public boolean supportsAlgorithm(String alg) {
		return SUPPORTED_ALGORITHMS.contains(alg);
	}

	@Override
	public GenericECJWKBuilder builder() {
		return new GenericECJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator);
	}
	
	@Override
	public GenericECJWKBuilder builder(Map<String, Object> parameters) throws JWKReadException {
		return new GenericECJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator, parameters);
	}

	@Override
	public GenericECJWKGenerator generator() {
		return new GenericECJWKGenerator();
	}
	
	@Override
	public GenericECJWKGenerator generator(String alg, Map<String, Object> parameters) throws JWKGenerateException {
		return new GenericECJWKGenerator(parameters).algorithm(alg);
	}
}

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
import io.inverno.mod.security.jose.internal.jwk.SwitchableJWKURLResolver;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKKeyResolver;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.okp.XECJWKFactory;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic Extended Elliptic Curve JSON Web Key factory implementation.
 * </p>
 * 
 * <p>
 * It supports the {@code OKP} key type and the following algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH-ES with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A128KW with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A192KW with extended elliptic curve X25519 or X448</li>
 * <li>ECDH-ES+A256KW with extended elliptic curve X25519 or X448</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class GenericXECJWKFactory extends AbstractOKPJWKFactory<XECPublicKey, XECPrivateKey, GenericXECJWK, GenericXECJWKBuilder, GenericXECJWKGenerator> implements XECJWKFactory<GenericXECJWK, GenericXECJWKBuilder, GenericXECJWKGenerator> {
	
	private static final Set<String> SUPPORTED_ALGORITHMS = Arrays.stream(XECAlgorithm.values()).map(XECAlgorithm::getAlgorithm).collect(Collectors.toSet());
	
	/**
	 * <p>
	 * Creates a generic XEC JWK factory.
	 * </p>
	 *
	 * @param configuration     the JOSE module configuration
	 * @param jwkStore          a JWK store
	 * @param keyResolver       a JWK key resolver
	 * @param mapper            an object mapper
	 * @param urlResolver       a JWK URL resolver
	 * @param certPathValidator an X.509 certificate path validator
	 */
	public GenericXECJWKFactory(JOSEConfiguration configuration, JWKStore jwkStore, JWKKeyResolver keyResolver, ObjectMapper mapper, SwitchableJWKURLResolver urlResolver, X509JWKCertPathValidator certPathValidator) {
		super(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
	}

	@Override
	public boolean supportsAlgorithm(String alg) {
		return SUPPORTED_ALGORITHMS.contains(alg);
	}
	
	@Override
	public GenericXECJWKBuilder builder() {
		return new GenericXECJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator);
	}
	
	@Override
	public GenericXECJWKBuilder builder(Map<String, Object> parameters) throws JWKReadException {
		return new GenericXECJWKBuilder(this.configuration, this.jwkStore, this.keyResolver, this.urlResolver, this.certPathValidator, parameters);
	}
	
	@Override
	public GenericXECJWKGenerator generator() {
		return new GenericXECJWKGenerator();
	}

	@Override
	public GenericXECJWKGenerator generator(String alg, Map<String, Object> parameters) throws JWKGenerateException {
		return new GenericXECJWKGenerator(parameters).algorithm(alg);
	}
}

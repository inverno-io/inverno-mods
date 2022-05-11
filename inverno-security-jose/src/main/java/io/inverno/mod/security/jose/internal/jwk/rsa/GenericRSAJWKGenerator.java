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

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKGenerator;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWKGenerator;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic RSA JSON Web Key generator implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericRSAJWKGenerator extends AbstractX509JWKGenerator<RSAPublicKey, RSAPrivateKey, GenericRSAJWK, GenericRSAJWKGenerator> implements RSAJWKGenerator<GenericRSAJWK, GenericRSAJWKGenerator> {
	
	/**
	 * The minimum key size.
	 */
	public static final int MINIMUM_KEY_SIZE = 2048;
	
	/**
	 * The default key size.
	 */
	public static final int DEFAULT_KEY_SIZE = MINIMUM_KEY_SIZE;
	
	private RSAAlgorithm rsaAlg;
	
	private Integer keySize;
	
	/**
	 * <p>
	 * Creates a generic RSA JWK generator.
	 * </p>
	 */
	public GenericRSAJWKGenerator() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates a generic RSA JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public GenericRSAJWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
		super(parameters);
		if(this.keySize == null) {
			this.keySize = DEFAULT_KEY_SIZE;
		}
	}
	
	@Override
	protected void set(String field, Object value) throws JWKGenerateException {
		switch(field) {
			case "keySize": {
				this.keySize((Integer)value);
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}
	
	@Override
	public GenericRSAJWKGenerator algorithm(String alg) {
		this.rsaAlg = alg != null ? RSAAlgorithm.fromAlgorithm(alg) : null;
		return super.algorithm(alg);
	}
	
	@Override
	public GenericRSAJWKGenerator keySize(int keySize) {
		this.keySize = keySize;
		return this;
	}

	@Override
	protected Mono<Void> verify() throws JWKGenerateException, JWKProcessingException {
		return super.verify().then(Mono.fromRunnable(() -> {
			if(this.keySize < MINIMUM_KEY_SIZE) {
				throw new JWKGenerateException("Key size must be at least " + MINIMUM_KEY_SIZE);
			}
		}));
	}

	@Override
	protected Mono<GenericRSAJWK> doGenerate() throws JWKGenerateException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			try {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
				keyPairGenerator.initialize(this.keySize);
				KeyPair keyPair = keyPairGenerator.generateKeyPair();

				RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();
				RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();

				GenericRSAJWK jwk = new GenericRSAJWK(
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(publicKey.getModulus())),
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(publicKey.getPublicExponent())),
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toUnsignedBytes(privateKey.getPrivateExponent()))
				);

				jwk.setPublicKeyUse(this.use);
				jwk.setKeyOperations(this.key_ops);
				jwk.setKeyId(this.kid);
				jwk.setAlgorithm(this.rsaAlg);

				return jwk;
			} 
			catch (NoSuchAlgorithmException e) {
				throw new JWKGenerateException("Error generating RSA JWK", e);
			}
		});
	}
}

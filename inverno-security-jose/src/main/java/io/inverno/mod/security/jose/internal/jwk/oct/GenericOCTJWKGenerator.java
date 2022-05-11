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

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractJWKGenerator;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.oct.OCTJWKGenerator;
import java.security.SecureRandom;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Octet JSON Web Key generator implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericOCTJWKGenerator extends AbstractJWKGenerator<GenericOCTJWK, GenericOCTJWKGenerator> implements OCTJWKGenerator<GenericOCTJWK, GenericOCTJWKGenerator> {
	
	/**
	 * The minimum key size.
	 */
	public static final int MINIMUM_KEY_SIZE = 16;
	
	/**
	 * The default key size.
	 */
	public static final int DEFAULT_KEY_SIZE = 32;

	private OCTAlgorithm octAlg;
	
	private Integer keySize;
	
	private SecureRandom secureRandom;
	
	/**
	 * <p>
	 * Creates a generic OCT JWK generator.
	 * </p>
	 */
	public GenericOCTJWKGenerator() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates a generic OCT JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public GenericOCTJWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
		super(parameters);
		if(this.secureRandom == null) {
			this.secureRandom = JOSEUtils.DEFAULT_SECURE_RANDOM;
		}
	}
	
	@Override
	protected void set(String field, Object value) throws JWKGenerateException {
		switch(field) {
			case "keySize": {
				this.keySize((Integer)value);
				break;
			}
			case "secureRandom": {
				this.secureRandom((SecureRandom)value);
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}
	
	@Override
	public GenericOCTJWKGenerator algorithm(String alg) {
		this.octAlg = alg != null ? OCTAlgorithm.fromAlgorithm(alg) : null;
		if(this.octAlg != null) {
			this.keySize = this.octAlg.getEncryptionKeyLength() + (this.octAlg.getMacKeyLength() != null ? this.octAlg.getMacKeyLength() : 0);
		}
		return super.algorithm(alg);
	}
	
	@Override
	public GenericOCTJWKGenerator keySize(int keySize) {
		this.keySize = keySize;
		return this;
	}
	
	@Override
	public GenericOCTJWKGenerator secureRandom(SecureRandom secureRandom) {
		this.secureRandom = secureRandom != null ? secureRandom : JOSEUtils.DEFAULT_SECURE_RANDOM;
		return this;
	}

	@Override
	protected Mono<Void> verify() throws JWKGenerateException, JWKProcessingException {
		return super.verify().then(Mono.fromRunnable(() -> {
			if(this.keySize != null) {
				if(this.keySize < MINIMUM_KEY_SIZE) {
					throw new JWKGenerateException("Key size must be at least " + MINIMUM_KEY_SIZE);
				}
				int macKeyLength = this.octAlg.getMacKeyLength() != null ? this.octAlg.getMacKeyLength() : 0;
				if(this.octAlg != null && this.keySize != this.octAlg.getEncryptionKeyLength() + macKeyLength) {
					throw new JWKGenerateException("Key size " + this.keySize + " is inconsistent with algorithm " + this.octAlg.getAlgorithm() + " which requires " + (this.octAlg.getEncryptionKeyLength() + macKeyLength));
				}
			}
		}));
	}
	
	@Override
	public Mono<GenericOCTJWK> doGenerate() throws JWKGenerateException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			byte[] keyBytes = new byte[this.keySize != null ? this.keySize : DEFAULT_KEY_SIZE];
			this.secureRandom.nextBytes(keyBytes);

			GenericOCTJWK jwk = new GenericOCTJWK(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(keyBytes));
			jwk.setPublicKeyUse(this.use);
			jwk.setKeyOperations(this.key_ops);
			jwk.setKeyId(this.kid);
			jwk.setAlgorithm(this.octAlg);

			return jwk;
		});
	}
}

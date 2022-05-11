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

import io.inverno.mod.security.jose.internal.jwk.AbstractJWKGenerator;
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWKGenerator;
import java.util.Map;
import org.apache.commons.text.RandomStringGenerator;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Password-based JSON Web Key generator implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericPBES2JWKGenerator extends AbstractJWKGenerator<GenericPBES2JWK, GenericPBES2JWKGenerator> implements PBES2JWKGenerator<GenericPBES2JWK, GenericPBES2JWKGenerator> {

	private static final RandomStringGenerator DEFAULT_RANDOM_STRING_GENERATOR = new RandomStringGenerator.Builder().build();
	
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-8.8">RFC7518 Section 8.8</a>
	 */
	private static final int MAXIMUM_LENGTH = 128;
	
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-8.8">RFC7518 Section 8.8</a>
	 */
	private static final int DEFAULT_LENGTH = PBES2Algorithm.PBES2_HS256_A128KW.getEncryptionKeyLength();
	
	private PBES2Algorithm pbes2Alg;
	
	private Integer length;
	
	private RandomStringGenerator randomStringGenerator;
	
	/**
	 * <p>
	 * Creates a generic PBES2 JWK generator.
	 * </p>
	 */
	public GenericPBES2JWKGenerator() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates a generic PBES2 JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public GenericPBES2JWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
		super(parameters);
		if(this.length == null) {
			this.length = DEFAULT_LENGTH;
		}
		if(this.randomStringGenerator == null) {
			this.randomStringGenerator = DEFAULT_RANDOM_STRING_GENERATOR;
		}
	}
	
	@Override
	protected void set(String field, Object value) throws JWKGenerateException {
		switch(field) {
			case "length": {
				this.length((Integer)value);
				break;
			}
			case "randomStringGenerator": {
				this.randomStringGenerator((RandomStringGenerator)value);
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}

	@Override
	public GenericPBES2JWKGenerator algorithm(String alg) {
		this.pbes2Alg = alg != null ? PBES2Algorithm.fromAlgorithm(alg) : null;
		if(this.pbes2Alg != null) {
			this.length = this.pbes2Alg.getEncryptionKeyLength();
		}
		return super.algorithm(alg);
	}

	@Override
	public GenericPBES2JWKGenerator length(int length) {
		this.length = length;
		return this;
	}

	@Override
	public GenericPBES2JWKGenerator randomStringGenerator(RandomStringGenerator randomStringGenerator) {
		this.randomStringGenerator = randomStringGenerator != null ? randomStringGenerator : DEFAULT_RANDOM_STRING_GENERATOR;
		return this;
	}

	@Override
	protected Mono<Void> verify() throws JWKGenerateException, JWKProcessingException {
		return super.verify().then(Mono.fromRunnable(() -> {
			int minimumLength = this.pbes2Alg != null ? this.pbes2Alg.getEncryptionKeyLength() : DEFAULT_LENGTH;
			if(this.length < minimumLength) {
				throw new JWKGenerateException("Password length must be at least " + minimumLength);
			}
			else if(this.length > MAXIMUM_LENGTH) {
				throw new JWKGenerateException("Password length must no longer than " + MAXIMUM_LENGTH);
			}
		}));
	}
	
	@Override
	protected Mono<GenericPBES2JWK> doGenerate() throws JWKGenerateException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			String generatedPassword = this.randomStringGenerator.generate(this.length);
			
			GenericPBES2JWK jwk = new GenericPBES2JWK(generatedPassword);
			jwk.setAlgorithm(this.pbes2Alg);
			jwk.setKeyId(this.kid);
			jwk.setKeyOperations(this.key_ops);
			jwk.setPublicKeyUse(this.use);
			
			return jwk;
		});
	}
}

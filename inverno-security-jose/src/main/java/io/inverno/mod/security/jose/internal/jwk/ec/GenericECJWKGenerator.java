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

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWKGenerator;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.ec.ECJWKGenerator;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Elliptic Curve JSON Web Key generator implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericECJWKGenerator extends AbstractX509JWKGenerator<ECPublicKey, ECPrivateKey, GenericECJWK, GenericECJWKGenerator> implements ECJWKGenerator<GenericECJWK, GenericECJWKGenerator> {

	/**
	 * The default Elliptic curve.
	 */
	public static final ECCurve DEFAULT_CURVE = ECCurve.P_256;
	
	private ECAlgorithm ecAlg;
	
	private ECCurve curve;

	/**
	 * <p>
	 * Creates a generic EC JWK generator.
	 * </p>
	 */
	public GenericECJWKGenerator() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates a generic EC JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public GenericECJWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
		super(parameters);
		if(this.curve == null) {
			this.curve = DEFAULT_CURVE;
		}
	}
	
	@Override
	protected void set(String field, Object value) throws JWKGenerateException {
		switch(field) {
			case "crv": {
				this.curve((String)value);
				break;
			}
			default: {
				super.set(field, value);
				break;
			}
		}
	}
	
	@Override
	public GenericECJWKGenerator algorithm(String alg) {
		this.ecAlg = alg != null ? ECAlgorithm.fromAlgorithm(alg) : null;
		return super.algorithm(alg);
	}
	
	@Override
	public GenericECJWKGenerator curve(String crv) {
		this.curve = crv != null ? ECCurve.fromCurve(crv) : DEFAULT_CURVE;
		return this;
	}
	
	@Override
	protected Mono<Void> verify() throws JWKGenerateException, JWKProcessingException {
		return super.verify().then(Mono.fromRunnable(() -> {
			if(this.ecAlg != null && this.ecAlg.getCurve() != null && !this.curve.equals(this.ecAlg.getCurve())) {
				throw new JWKGenerateException("JWK with curve " + this.curve.getCurve() + " does not support algorithm " + this.ecAlg.getAlgorithm());
			}
		}));
	}

	@Override
	protected Mono<GenericECJWK> doGenerate() throws JWKGenerateException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			try {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
				
				keyPairGenerator.initialize(new ECGenParameterSpec(this.curve.getJCAName()));
				KeyPair keyPair = keyPairGenerator.generateKeyPair();

				ECPublicKey publicKey = (ECPublicKey)keyPair.getPublic();
				ECPrivateKey privateKey = (ECPrivateKey)keyPair.getPrivate();

				GenericECJWK jwk = new GenericECJWK(
					this.curve, 
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(publicKey.getW().getAffineX(), this.curve.getKeyLength())), 
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(publicKey.getW().getAffineY(), this.curve.getKeyLength())), 
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(JOSEUtils.toPaddedUnsignedBytes(privateKey.getS(), this.curve.getKeyLength()))
				);
				jwk.setPublicKeyUse(this.use);
				jwk.setKeyOperations(this.key_ops);
				jwk.setKeyId(this.kid);
				jwk.setAlgorithm(this.alg);

				return jwk;
			} 
			catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
				throw new JWKGenerateException("Error generating EC JWK", e);
			}
		});
	}
}

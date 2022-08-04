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
package io.inverno.mod.security.jose.internal.jwa;

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWK;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.AsymmetricJWK;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.crypto.KeyAgreement;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Base Elliptic Curve Diffie-Hellman Ephemeral Static direct key manager implementation.
 * </p>
 * 
 * <p>
 * It processes the following parameters:
 * </p>
 * 
 * <ul>
 * <li>{@code epk}: ephemeral public key</li>
 * <li>{@code apu}: Agreement PartyUInfo</li>
 * <li>{@code apv}: Agreement PartyVInfo</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the asymmetric JWK type
 * @param <D> the JWA algorithm type
 */
public abstract class AbstractECDH_ESKeyManager<A extends PublicKey, B extends PrivateKey, C extends AsymmetricJWK<A, B>, D extends JWAAlgorithm<C>> extends AbstractDirectJWAKeyManager<C, D> {

	/**
	 * The set of parameters processed by the key manager.
	 */
	public static final Set<String> PROCESSED_PARAMETERS = Set.of("epk", "apu", "apv");
	
	/**
	 * <p>
	 * Creates an ECDH-ES direct key manager.
	 * </p>
	 *
	 * @param jwk       a JWK
	 * @param algorithm a JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AbstractECDH_ESKeyManager(C jwk, D algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
	}

	/**
	 * <p>
	 * Creates an ECDH-ES direct key manager.
	 * </p>
	 * 
	 * @param jwk a JWK
	 */
	public AbstractECDH_ESKeyManager(C jwk) {
		super(jwk);
	}
	
	/**
	 * <p>
	 * Returns the JCA key agreement algorithm to use to derive the key.
	 * </p>
	 * 
	 * @return a JCA key agreement algorithm
	 */
	protected abstract String getKeyAgreementAlgorithm();
	
	/**
	 * <p>
	 * Extracts the Ephemeral key from the specified parameters map or generates a new one.
	 * </p>
	 * 
	 * <p>
	 * Parameters are typically custom parameters in a JOSE header. When {@code epk}, {@code apv} and {@code apu} are missing, it is assumed that we are a producer and a key derivation and then a new
	 * private ephemeral key must be generated.
	 * </p>
	 * 
	 * @param parameters the parameters map
	 * 
	 * @return the extracted EPK or a new ephemeral private key
	 * 
	 * @throws JWAProcessingException if there was an error extracting or generating the ephemeral key
	 */
	protected abstract C getEPK(Map<String, Object> parameters) throws JWAProcessingException;

	@Override
	public Set<String> getProcessedParameters() {
		return PROCESSED_PARAMETERS;
	}
	
	@Override
	public DirectCEK doDeriveCEK(OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException {
		try {
			// 1. If missing, generate the EPK, otherwise use the EPK
			C epk = this.getEPK(parameters);

			// 2. Derive the key 
			KeyAgreement ka = KeyAgreement.getInstance(this.getKeyAgreementAlgorithm());
		
			Optional<B> epkPrivateKey = epk.toPrivateKey();
			if(epkPrivateKey.isPresent()) {
				// We have generated an ephemeral key, we are a producer, the JWK is the public key of the consumer
				ka.init(epkPrivateKey.get());
				ka.doPhase(this.jwk.toPublicKey(), true);
			}
			else {
				// We have received a JWE with an ephemeral public key, we are a consumer, the JWK is the private key of the consumer
				ka.init(this.jwk.toPrivateKey().orElseThrow(() -> new JWAKeyManagerException("JWK is missing private key")));
				ka.doPhase(epk.toPublicKey(), true);
			}

			byte[] z = ka.generateSecret();

			// 3. Concat KDF, hash (SHA-256) and take what we need
			// https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-56Ar2.pdf Section 5.8 and Section 6.2.2.2
			byte[] deriveSharedKey = deriveSharedKey(z, octEnc, parameters);

			GenericOCTJWK cek = new GenericOCTJWK(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(deriveSharedKey));
			cek.setAlgorithm(octEnc);

			return new GenericDirectCEK(
				cek, 
				Map.of(
					"epk", epk.toPublicJWK().minify()
				)
			);
		} 
		catch(NoSuchAlgorithmException | InvalidKeyException | IOException e) {
			throw new JWAKeyManagerException(e);
		}
	}
	
	/**
	 * <p>
	 * Derives the shared key as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.6.2">RFC7518 Section 4.6.2</a>.
	 * </p>
	 *
	 * @param z          the shared secret
	 * @param parameters the parameters map
	 *
	 * @return the derived key
	 */
	private static byte[] deriveSharedKey(byte[] z, OCTAlgorithm encryptionAlgorithm, Map<String, Object> parameters) throws NoSuchAlgorithmException, IOException {
		ByteArrayOutputStream dkmOutput = new ByteArrayOutputStream();
		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		byte[] otherInfo = computeOtherInfo(encryptionAlgorithm.getAlgorithm(), encryptionAlgorithm.getEncryptionKeyLength(), parameters);

		// keyLen / hashLen
		int reps = (encryptionAlgorithm.getEncryptionKeyLength() + digest.getDigestLength() - 1) / digest.getDigestLength();
		for(int i = 1;i <= reps;i++) {
			digest.update(JOSEUtils.toUnsignedBytes(i));
			digest.update(z);
			digest.update(otherInfo);
			
			dkmOutput.write(digest.digest());
		}

		byte[] dkm = dkmOutput.toByteArray();

		if(dkm.length == encryptionAlgorithm.getEncryptionKeyLength()) {
			return dkm;
		}
		else {
			byte[] sharedKey = new byte[encryptionAlgorithm.getEncryptionKeyLength()];
			System.arraycopy(dkm, 0, sharedKey, 0, sharedKey.length);
			return sharedKey;
		}
	}
	
	/**
	 * <p>
	 * Computes other info as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.6.2">RFC7518 Section 4.6.2</a>.
	 * </p>
	 *
	 * @param enc        the target encryption algorithm
	 * @param keydatalen the key data length
	 * @param parameters the parameters map
	 *
	 * @return other info
	 *
	 * @throws JWAKeyManagerException if there was an error computing other info
	 */
	private static byte[] computeOtherInfo(String enc, int keydatalen, Map<String, Object> parameters) throws JWAKeyManagerException{
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream(bout);

			// AlgorithmID
			appendOtherInfo(enc.getBytes(), dout);

			if(parameters != null) {
				// PartyUInfo
				String apu = (String)parameters.get("apu");
				if(!StringUtils.isBlank(apu)) {
					appendOtherInfo(Base64.getUrlDecoder().decode(apu), dout);
				}
				else {
					dout.writeInt(0);
				}

				// PartyVInfo
				String apv = (String)parameters.get("apv");
				if(!StringUtils.isBlank(apv)) {
					appendOtherInfo(Base64.getUrlDecoder().decode(apv), dout);
				}
				else {
					dout.writeInt(0);
				}
			}
			else {
				// PartyUInfo
				dout.writeInt(0);
				// PartyVInfo
				dout.writeInt(0);
			}

			// SuppPubInfo
			dout.writeInt(keydatalen * 8);

			return bout.toByteArray();
		}
		catch(IOException e) {
			throw new JWAKeyManagerException("Error computing other info");
		}
	}
	
	/**
	 * <p>
	 * Appends the specified other info to the specified data output stream.
	 * </p>
	 *
	 * @param otherInfo the other info to append
	 * @param output    the data output stream
	 *
	 * @throws IOException if there was an error appending the other info
	 */
	private static void appendOtherInfo(byte[] otherInfo, DataOutputStream output) throws IOException {
		output.writeInt(otherInfo.length);
		output.write(otherInfo);
	}
}

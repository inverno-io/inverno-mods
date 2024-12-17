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

import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.JWASignatureException;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

/**
 * <p>
 * Elliptic Cuvre signer implementation.
 * </p>
 * 
 * <p>
 * It supports the following signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>ES256 with elliptic curve P-256</li>
 * <li>ES384 with elliptic curve P-384</li>
 * <li>ES512 with elliptic curve P-521</li>
 * <li>ES256K with elliptic curve secp256k1</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class ECSigner extends AbstractJWASigner<ECJWK, ECAlgorithm> {

	/**
	 * <p>
	 * Creates an EC signer.
	 * </p>
	 *
	 * @param jwk       an elliptic curve JWK
	 * @param algorithm an elliptic curve JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public ECSigner(ECJWK jwk, ECAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!jwk.getCurve().equals(algorithm.getCurve().getCurve())) {
			throw new JWAProcessingException("JWK with curve " + jwk.getCurve() + " does not support algorithm " + algorithm);
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an EC signer.
	 * </p>
	 *
	 * @param jwk an elliptic curve JWK
	 */
	protected ECSigner(ECJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
	
	}
	
	@Override
	protected byte[] doSign(byte[] data) throws JWASignatureException {
		return this.jwk.toPrivateKey()
			.map(privateKey -> {
				try {
					Signature sig = Signature.getInstance(this.algorithm.getJcaAlgorithm());
					sig.initSign(privateKey);
					sig.update(data);
					return signatureFromDer(sig.sign());
				} 
				catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
					throw new JWASignatureException(e);
				}
			})
			.orElseThrow(() -> new JWASignatureException("JWK is missing EC private key"));
	}
	
	@Override
	protected boolean doVerify(byte[] data, byte[] signature) throws JWASignatureException {
		try {
			Signature sig = Signature.getInstance(this.algorithm.getJcaAlgorithm());
			sig.initVerify(this.jwk.toPublicKey());
			sig.update(data);
			return sig.verify(signatureToDER(signature));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new JWASignatureException(e);
		}
	}
	
	/**
	 * <p>
	 * Decodes the specified ASN1./DER signature to the expected JOSE object signature format.
	 * </p>
	 *
	 * @param derSignature an ASN1./DER-encoded
	 *
	 * @return The JOSE object signature
	 * 
	 * @throws JWASignatureException if the specified signature is invalid
	 */
    public byte[] signatureFromDer(final byte[] derSignature) throws JWASignatureException {

        if(derSignature.length < 8 || derSignature[0] != 48) {
            throw new JWASignatureException("Invalid DER signature");
        }

        int offset;
        if(derSignature[1] > 0) {
            offset = 2;
        } 
		else if(derSignature[1] == (byte) 0x81) {
            offset = 3;
        } 
		else {
            throw new JWASignatureException("Invalid DER signature");
        }

        byte rLength = derSignature[offset + 1];

        int i = rLength;
        while((i > 0) && (derSignature[(offset + 2 + rLength) - i] == 0)) {
            i--;
        }

        byte sLength = derSignature[offset + 2 + rLength + 1];

        int j = sLength;
        while((j > 0) && (derSignature[(offset + 2 + rLength + 2 + sLength) - j] == 0)) {
            j--;
        }

        int rawLen = Math.max(i, j);
        rawLen = Math.max(rawLen, this.algorithm.getCurve().getSignatureLength() / 2);

        if((derSignature[offset - 1] & 0xff) != derSignature.length - offset
            || (derSignature[offset - 1] & 0xff) != 2 + rLength + 2 + sLength
            || derSignature[offset] != 2
            || derSignature[offset + 2 + rLength] != 2) {
            throw new JWASignatureException("Invalid DER signature");
        }

        final byte[] concatSignature = new byte[2 * rawLen];

        System.arraycopy(derSignature, (offset + 2 + rLength) - i, concatSignature, rawLen - i, i);
        System.arraycopy(derSignature, (offset + 2 + rLength + 2 + sLength) - j, concatSignature, 2 * rawLen - j, j);

        return concatSignature;
    }
	
	/**
	 * <p>
	 * Encodes the specified JOSE object signature to ASN1./DER
	 * </p>
	 * 
	 * @param signature	a JOSE object signature
	 * 
	 * @return a ASN1./DER signature
	 * 
	 * @throws JWASignatureException if the specified signature is invalid
	 */
	private byte[] signatureToDER(byte[] signature) throws JWASignatureException {
        int rawLen = signature.length / 2;

        int i = rawLen;

        while((i > 0) && (signature[rawLen - i] == 0)) {
            i--;
        }

        int j = i;

        if(signature[rawLen - i] < 0) {
            j += 1;
        }

        int k = rawLen;

        while((k > 0) && (signature[2 * rawLen - k] == 0)) {
            k--;
        }

        int l = k;

        if(signature[2 * rawLen - k] < 0) {
            l += 1;
        }

        int len = 2 + j + 2 + l;

        if(len > 255) {
            throw new JWASignatureException("Invalid JOSE object signature");
        }

        int offset;

        final byte[] derSignature;

        if(len < 128) {
            derSignature = new byte[2 + 2 + j + 2 + l];
            offset = 1;
        }
		else {
            derSignature = new byte[3 + 2 + j + 2 + l];
            derSignature[1] = (byte) 0x81;
            offset = 2;
        }

        derSignature[0] = 48;
        derSignature[offset++] = (byte) len;
        derSignature[offset++] = 2;
        derSignature[offset++] = (byte) j;

        System.arraycopy(signature, rawLen - i, derSignature, (offset + j) - i, i);

        offset += j;

        derSignature[offset++] = 2;
        derSignature[offset++] = (byte) l;

        System.arraycopy(signature, 2 * rawLen - k, derSignature, (offset + l) - k, k);

        return derSignature;
    }
}

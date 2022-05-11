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
package io.inverno.mod.security.jose.internal;

import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * <p>
 * JOSE utilities class.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class JOSEUtils {
	
	/**
	 * Default secure random.
	 */
	public static final SecureRandom DEFAULT_SECURE_RANDOM = new SecureRandom();
	
	/**
	 * Base64URL encoder without padding.
	 */
	public static final Base64.Encoder BASE64_NOPAD_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	
	/**
	 * <p>
	 * Returns the integer value as an unsigned byte array of the specified length padded with leading zeros.
	 * </p>
	 * 
	 * <p>
	 * In <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.2">RFC7518 Section 6.2</a> it is not clearly specify that x, y and d must
	 * be converted to unsigned byte arrays however it is specified that their size must be that of a coordinate for the specified curve as a result
	 * we must get rid of the sign bit. Note that elliptic curve coordinates are always positive integers.
	 * </p>
	 * 
	 * @param value  an integer
	 * @param length the length
	 * 
	 * @return a byte array
	 */
	public static byte[] toPaddedUnsignedBytes(BigInteger value, int length) throws JWKProcessingException {
		byte[] bytes = value.toByteArray();
        if (bytes.length == length) {
            return bytes;
        }

        int start = (bytes[0] == 0 && bytes.length != 1) ? 1 : 0;
        int count = bytes.length - start;

        if (count > length) {
			// This should never happen here since the length is deduced from the curve
            throw new JWKProcessingException("standard length exceeded for value");
        }

        byte[] paddedBytes = new byte[length];
        System.arraycopy(bytes, start, paddedBytes, paddedBytes.length - count, count);
        return paddedBytes;
	}
	
	/**
	 * <p>
	 * Converts the specified integer value to unsigned byte array.
	 * </p>
	 * 
	 * @param value the integer to convert
	 * 
	 * @return the integer as unsigned byte array
	 */
	public static byte[] toUnsignedBytes(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if(bytes[0] == 0 && bytes.length != 1) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }
	
	/**
	 * <p>
	 * Converts the specified integer value to unsigned byte array.
	 * </p>
	 * 
	 * @param value the integer to convert
	 * 
	 * @return the integer as unsigned byte array
	 */
	public static byte[] toUnsignedBytes(int value) {
		return new byte[] {
			(byte)((value >>> 24) & 0xff),
			(byte)((value >>> 16) & 0xff),
			(byte)((value >>> 8) & 0xff),
			(byte)(value & 0xff),
		};
	}
	
	/**
	 * <p>
	 * Generates an initialization vector of the specified length using the specified secure random.
	 * </p>
	 * 
	 * @param secureRandom a secure random
	 * @param length the initialization vector length
	 * 
	 * @return an initialization vector
	 */
	public static byte[] generateInitializationVector(SecureRandom secureRandom, int length) {
		byte[] iv = new byte[length];
		secureRandom.nextBytes(iv);
		return iv;
	}
	
	/**
	 * <p>
	 * Generates a salt of the specified length using the specified secure random.
	 * </p>
	 * 
	 * @param secureRandom a secure random
	 * @param length the initialization vector length
	 * 
	 * @return a salt
	 */
	public static byte[] generateSalt(SecureRandom secureRandom, int length) {
		byte[] salt = new byte[length];
		secureRandom.nextBytes(salt);
		return salt;
	}
	
	/**
	 * <p>
	 * Returns the X.509 SHA1 thumbprint of the specified certificate.
	 * </p>
	 * 
	 * @param certificate an X.509 certificate
	 * 
	 * @return an X.509 SHA1 thumbprint
	 * 
	 * @throws JWKProcessingException if there was an error generating the thumbprint
	 */
	public static String toX509CertificateSha1Thumbprint(X509Certificate certificate) throws JWKProcessingException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] der = certificate.getEncoded();
			md.update(der);
			return BASE64_NOPAD_URL_ENCODER.encodeToString(md.digest());
		} 
		catch (NoSuchAlgorithmException | CertificateEncodingException e) {
			throw new JWKProcessingException("Error generating X509 certificate thumbprint", e);
		} 
	}
	
	/**
	 * <p>
	 * Returns the X.509 SHA256 thumbprint of the specified certificate.
	 * </p>
	 * 
	 * @param certificate an X.509 certificate
	 * 
	 * @return an X.509 SHA1 thumbprint
	 * 
	 * @throws JWKProcessingException if there was an error generating the thumbprint
	 */
	public static String toX509CertificateSha256Thumbprint(X509Certificate certificate) throws JWKProcessingException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] der = certificate.getEncoded();
			md.update(der);
			return BASE64_NOPAD_URL_ENCODER.encodeToString(md.digest());
		} 
		catch (NoSuchAlgorithmException | CertificateEncodingException e) {
			throw new JWKProcessingException("Error generating X509 certificate thumbprint", e);
		}
	}
}

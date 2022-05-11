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

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.AbstractX509JWK;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwk.okp.OKPJWK;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Base Octet Key Pair JSON Web Key implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 */
public abstract class AbstractOKPJWK<A extends PublicKey, B extends PrivateKey> extends AbstractX509JWK<A, B> implements OKPJWK<A, B> {

	/**
	 * The Curve parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3">RFC8037 Section 3</a>.
	 */
	protected final OKPCurve curve;
	
	/**
	 * The public key parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3">RFC8037 Section 3</a>.
	 */
	protected final String x;
	
	/**
	 * The private key parameter as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3">RFC8037 Section 3</a>.
	 */
	protected final String d;
	
	/**
	 * The public key.
	 */
	protected A publicKey;
	
	/**
	 * The private key.
	 */
	protected Optional<B> privateKey;
	
	/**
	 * <p>
	 * Creates an untrusted public OKP JWK with the specified curve and public key value.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the public key value encoded as Base64URL without padding
	 */
	public AbstractOKPJWK(OKPCurve curve, String x) {
		this(curve, x, null, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a public OKP JWK with the specified curve, public key value and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param curve       an elliptic curve
	 * @param x           the public key encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public AbstractOKPJWK(OKPCurve curve, String x, X509Certificate certificate) {
		this(curve, x, null, null, certificate, certificate != null);
	}
	
	/**
	 * <p>
	 * Creates an untrusted private OKP JWK with the specified curve, public key value and private key value.
	 * </p>
	 * 
	 * @param curve an elliptic curve
	 * @param x     the public key value encoded as Base64URL without padding
	 * @param d     the private key value encoded as Base64URL without padding
	 */
	public AbstractOKPJWK(OKPCurve curve, String x, String d) {
		this(curve, x, d, null, null, false);
	}
	
	/**
	 * <p>
	 * Creates a private OKP JWK with the specified curve, public key value, private key value and private key.
	 * </p>
	 * 
	 * @param curve   an elliptic curve
	 * @param x       the public key value encoded as Base64URL without padding
	 * @param d       the private key value encoded as Base64URL without padding
	 * @param key     a private key
	 * @param trusted true to create a trusted JWK, false otherwise
	 */
	public AbstractOKPJWK(OKPCurve curve, String x, String d, B key, boolean trusted) {
		this(curve, x, d, key, null, trusted);
	}
	
	/**
	 * <p>
	 * Creates a public OKP JWK with the specified curve, public key value, private key value and certificate.
	 * </p>
	 * 
	 * <p>
	 * The JWK is considered trusted if the specified certificate, which is assumed to be validated, is not null.
	 * </p>
	 * 
	 * @param curve       an elliptic curve
	 * @param x           the public key value encoded as Base64URL without padding
	 * @param d           the private key value encoded as Base64URL without padding
	 * @param certificate an X.509 certificate
	 */
	public AbstractOKPJWK(OKPCurve curve, String x, String d, X509Certificate certificate) {
		this(curve, x, null, null, certificate, certificate != null);
	}
	
	/**
	 * <p>
	 * Creates a private OKP JWK with the specified curve, public coordinates, private key value, OKP private key and certificate.
	 * </p>
	 *
	 * @param curve       an elliptic curve
	 * @param x           the public key value encoded as Base64URL without padding
	 * @param d           the private key value encoded as Base64URL without padding
	 * @param key         a private key
	 * @param certificate an X.509 certificate
	 * @param trusted     true to create a trusted JWK, false otherwise
	 */
	public AbstractOKPJWK(OKPCurve curve, String x, String d, B key, X509Certificate certificate, boolean trusted) {
		super(KEY_TYPE, key, certificate, trusted);
		
		this.curve = curve;
		this.x = x;
		this.d = d;
		this.privateKey = key != null ? Optional.of(key) : null;
	}
	
	@Override
	public String getCurve() {
		return this.curve.getCurve();
	}

	@Override
	public String getPublicKey() {
		return this.x;
	}
	
	@Override
	public String getPrivateKey() {
		return this.d;
	}
	
	@Override
	public String toJWKThumbprint(MessageDigest digest) {
		return toJWKThumbprint(digest, this.curve.getCurve(), this.kty, this.x);
	}
	
	/**
	 * <p>
	 * Generates and returns an OKP JWK thumbprint using the specified digest.
	 * </p>
	 * 
	 * @param digest the message digest to use
	 * @param crv the JWA elliptic curve
	 * @param kty the key type ({@code OKP})
	 * @param x the public key value encoded as Base64URL without padding
	 * 
	 * @return an OKP JWK thumbprint or null if some input data are null
	 */
	static String toJWKThumbprint(MessageDigest digest, String crv, String kty, String x) {
		if(crv == null || kty == null || x == null) {
			return null;
		}
		StringBuilder input = new StringBuilder();
		input.append('{');
		input.append("\"crv\":\"").append(crv).append("\",");
		input.append("\"kty\":\"").append(kty).append("\",");
		input.append("\"x\":\"").append(x).append("\"");
		input.append('}');
		
		return JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(digest.digest(input.toString().getBytes()));
	}
	
	/**
	 * <p>
	 * Reverses the specified byte array.
	 * </p>
	 * 
	 * @param arr the byte array to reverse
	 */
	protected static void reverse(byte [] arr) {
        int i = 0;
        int j = arr.length - 1;

        while (i < j) {
            swap(arr, i, j);
            i++;
            j--;
        }
    }
	
	/**
	 * <p>
	 * Swaps the bytes at positions i and j in the specified byte array.
	 * </p>
	 *
	 * @param arr a byte array
	 * @param i   an index
	 * @param j   an index
	 */
	protected static void swap(byte[] arr, int i, int j) {
        byte tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(curve, d, x);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractOKPJWK<?, ?> other = (AbstractOKPJWK<?, ?>) obj;
		return curve == other.curve && Objects.equals(d, other.d) && Objects.equals(x, other.x);
	}
}

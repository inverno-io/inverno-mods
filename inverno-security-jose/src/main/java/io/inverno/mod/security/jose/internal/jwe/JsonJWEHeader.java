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
package io.inverno.mod.security.jose.internal.jwe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A generic JWE header implementation used when building or reading JSON JWE objects.
 * </p>
 * 
 * <p>
 * This implementation has the following abilities:
 * </p>
 * 
 * <ul>
 * <li>track the parameters set in the header to be able to easily determine whether two headers overlap</li>
 * <li>merge a header into another</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class JsonJWEHeader extends GenericJWEHeader {

	private final Set<String> parametersSet;
	
	/**
	 * <p>
	 * Creates a JSON JWE header.
	 * </p>
	 */
	public JsonJWEHeader() {
		this.parametersSet = new HashSet<>();
	}

	/**
	 * <p>
	 * Creates a JSON JWE header.
	 * </p>
	 * 
	 * @param alg a key management JWA algorithm
	 * @param enc an encryption JWA algorithm
	 */
	public JsonJWEHeader(String alg, String enc) {
		super(alg, enc);
		this.parametersSet = new HashSet<>();
	}
	
	/**
	 * <p>
	 * Merges the specified header into this header.
	 * </p>
	 * 
	 * @param header the JSON JWE header to merge
	 * 
	 * @return this header after the merge
	 */
	public JsonJWEHeader merge(JsonJWEHeader header) {
		if(header != null) {
			if(header.alg != null) {
				this.alg = header.alg;
			}
			if(header.zip != null) {
				this.zip = header.zip;
			}
			if(header.cty != null) {
				this.cty = header.cty;
			}
			if(header.crit != null) {
				this.crit = header.crit;
			}
			if(header.customParameters != null) {
				if(this.customParameters == null) {
					this.customParameters = new HashMap<>();
				}
				this.customParameters.putAll(header.customParameters);
			}
			if(header.enc != null) {
				this.enc = header.enc;
			}
			if(header.jwk != null) {
				this.jwk = header.jwk;
			}
			if(header.jku != null) {
				this.jku = header.jku;
			}
			if(header.getKeyId() != null) {
				this.kid = header.getKeyId();
			}
			if(header.typ != null) {
				this.typ = header.typ;
			}
			if(header.x5c != null) {
				this.x5c = header.x5c;
			}
			if(header.x5t != null) {
				this.x5t = header.x5t;
			}
			if(header.x5t_S256 != null) {
				this.x5t_S256 = header.x5t_S256;
			}
			if(header.x5u != null) {
				this.x5u = header.x5u;
			}
		}
		return this;
	}

	/**
	 * <p>
	 * Returns the set of parameters sets in the JWE header.
	 * </p>
	 * 
	 * @return a set of parameters
	 */
	@JsonIgnore
	public Set<String> getParametersSet() {
		return parametersSet;
	}
	
	@Override
	public JsonJWEHeader algorithm(String alg) {
		this.parametersSet.add("alg");
		return (JsonJWEHeader)super.algorithm(alg);
	}

	@Override
	public JsonJWEHeader jwkSetURL(URI jku) {
		this.parametersSet.add("jku");
		return (JsonJWEHeader)super.jwkSetURL(jku);
	}

	@Override
	public JsonJWEHeader jwk(Map<String, Object> jwk) {
		this.parametersSet.add("jwk");
		return (JsonJWEHeader)super.jwk(jwk);
	}

	@Override
	public JsonJWEHeader keyId(String kid) {
		this.parametersSet.add("kid");
		return (JsonJWEHeader)super.keyId(kid);
	}

	@Override
	public JsonJWEHeader x509CertificateURL(URI x5u) {
		this.parametersSet.add("x5u");
		return (JsonJWEHeader)super.x509CertificateURL(x5u);
	}

	@Override
	public JsonJWEHeader x509CertificateChain(String[] x5c) {
		this.parametersSet.add("x5c");
		return (JsonJWEHeader)super.x509CertificateChain(x5c);
	}

	@Override
	public JsonJWEHeader x509CertificateSHA1Thumbprint(String x5t) {
		this.parametersSet.add("x5t");
		return (JsonJWEHeader)super.x509CertificateSHA1Thumbprint(x5t);
	}

	@Override
	public JsonJWEHeader x509CertificateSHA256Thumbprint(String x5t_S256) {
		this.parametersSet.add("x5t#S256");
		return (JsonJWEHeader)super.x509CertificateSHA256Thumbprint(x5t_S256);
	}

	@Override
	public JsonJWEHeader type(String typ) {
		this.parametersSet.add("typ");
		return (JsonJWEHeader)super.type(typ);
	}

	@Override
	public JsonJWEHeader contentType(String cty) {
		this.parametersSet.add("cty");
		return (JsonJWEHeader)super.contentType(cty);
	}

	@Override
	public JsonJWEHeader critical(String... crit) {
		this.parametersSet.add("crit");
		return (JsonJWEHeader)super.critical(crit);
	}

	@Override
	public JsonJWEHeader addCustomParameter(String key, Object value) {
		this.parametersSet.add(key);
		return (JsonJWEHeader)super.addCustomParameter(key, value);
	}
	
	@Override
	public JsonJWEHeader encryptionAlgorithm(String enc) {
		this.enc = enc;
		return (JsonJWEHeader)this;
	}

	@Override
	public JsonJWEHeader compressionAlgorithm(String zip) {
		this.zip = zip;
		return (JsonJWEHeader)this;
	}
}

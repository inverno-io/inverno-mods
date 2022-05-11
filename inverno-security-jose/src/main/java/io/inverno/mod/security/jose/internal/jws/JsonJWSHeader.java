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
package io.inverno.mod.security.jose.internal.jws;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A generic JWS header implementation used when building or reading JSON JWS objects.
 * </p>
 * 
 * <p>
 * This implementation has the ability to track overlapping parameters
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class JsonJWSHeader extends GenericJWSHeader {

	private Set<String> overlappedParameters;
	
	/**
	 * <p>
	 * Creates a JSON JWS header.
	 * </p>
	 */
	public JsonJWSHeader() {
	}

	/**
	 * <p>
	 * Creates a JSON JWS header.
	 * </p>
	 * 
	 * @param alg a signature JWA algorithm
	 */
	public JsonJWSHeader(String alg) {
		super(alg);
	}
	
	/**
	 * <p>
	 * Starts the parameter overlap recording.
	 * </p>
	 */
	public void startRecordOverlap() {
		this.overlappedParameters = new HashSet<>();
	}
	
	/**
	 * <p>
	 * Stops the parameter overlap recording and returns the overlapped parameters.
	 * </p>
	 * 
	 * @return the set of overlapped parameters
	 */
	public Set<String> stopRecordOverlap() {
		Set<String> result = this.overlappedParameters;
		this.overlappedParameters = null;
		return result;
	}
	
	@Override
	public JsonJWSHeader algorithm(String alg) {
		if(this.overlappedParameters != null && this.alg != null && alg != null) {
			this.overlappedParameters.add("alg");
		}
		return (JsonJWSHeader)super.algorithm(alg);
	}

	@Override
	public JsonJWSHeader jwkSetURL(URI jku) {
		if(this.overlappedParameters != null && this.jku != null && jku != null) {
			this.overlappedParameters.add("jku");
		}
		return (JsonJWSHeader)super.jwkSetURL(jku);
	}

	@Override
	public JsonJWSHeader jwk(Map<String, Object> jwk) {
		if(this.overlappedParameters != null && this.jwk != null && jwk != null) {
			this.overlappedParameters.add("jwk");
		}
		return (JsonJWSHeader)super.jwk(jwk);
	}

	@Override
	public JsonJWSHeader keyId(String kid) {
		if(this.overlappedParameters != null && this.kid != null && kid != null) {
			this.overlappedParameters.add("kid");
		}
		return (JsonJWSHeader)super.keyId(kid);
	}

	@Override
	public JsonJWSHeader x509CertificateURL(URI x5u) {
		if(this.overlappedParameters != null && this.x5u != null && x5u != null) {
			this.overlappedParameters.add("x5u");
		}
		return (JsonJWSHeader)super.x509CertificateURL(x5u);
	}

	@Override
	public JsonJWSHeader x509CertificateChain(String[] x5c) {
		if(this.overlappedParameters != null && this.x5c != null && x5c != null) {
			this.overlappedParameters.add("x5c");
		}
		return (JsonJWSHeader)super.x509CertificateChain(x5c);
	}

	@Override
	public JsonJWSHeader x509CertificateSHA1Thumbprint(String x5t) {
		if(this.overlappedParameters != null && this.x5t != null && x5t != null) {
			this.overlappedParameters.add("x5t");
		}
		return (JsonJWSHeader)super.x509CertificateSHA1Thumbprint(x5t);
	}

	@Override
	public JsonJWSHeader x509CertificateSHA256Thumbprint(String x5t_S256) {
		if(this.overlappedParameters != null && this.x5t_S256 != null && x5t_S256 != null) {
			this.overlappedParameters.add("x5t#S256");
		}
		return (JsonJWSHeader)super.x509CertificateSHA256Thumbprint(x5t_S256);
	}

	@Override
	public JsonJWSHeader type(String typ) {
		if(this.overlappedParameters != null && this.typ != null && typ != null) {
			this.overlappedParameters.add("typ");
		}
		return (JsonJWSHeader)super.type(typ);
	}

	@Override
	public JsonJWSHeader contentType(String cty) {
		if(this.overlappedParameters != null && this.cty != null && cty != null) {
			this.overlappedParameters.add("cty");
		}
		return (JsonJWSHeader)super.contentType(cty);
	}

	@Override
	public JsonJWSHeader critical(String... crit) {
		if(this.overlappedParameters != null && this.crit != null && crit != null) {
			this.overlappedParameters.add("crit");
		}
		return (JsonJWSHeader)super.critical(crit);
	}

	@Override
	public JsonJWSHeader addCustomParameter(String key, Object value) {
		if(value != null && this.overlappedParameters != null && this.customParameters.containsKey(key)) {
			this.overlappedParameters.add(key);
		}
		return (JsonJWSHeader)super.addCustomParameter(key, value);
	}

	@Override
	public JsonJWSHeader base64EncodePayload(Boolean b64) {
		if(this.overlappedParameters != null && this.b64 != null && b64 != null) {
			this.overlappedParameters.add("b64");
		}
		return (JsonJWSHeader)super.base64EncodePayload(b64);
	}
}

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
package io.inverno.mod.security.jose;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jws.JWSHeader;
import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A JOSE Header contains the parameters describing the cryptographic operations and parameters employed to secure a JOSE object.
 * </p>
 * 
 * <p>
 * It is part of a JOSE object and it defines the cryptographic information required to sign/verify or encrypt/decrypt a JOSE object such as a JSON Web Signature or a JSON Web Encryption.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see JWEHeader
 * @see JWSHeader
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface JOSEHeader {

	/**
	 * <p>
	 * Returns the cryptographic algorithm used to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * Depending on the type of JOSE object considered, it might designate a signature or a key management algorithm.
	 * </p>
	 * 
	 * @return a JWA algorithm
	 */
	@JsonProperty("alg")
	String getAlgorithm();
	
	/**
	 * <p>
	 * Returns the JWK Set URL.
	 * </p>
	 * 
	 * <p>
	 * This URL points to a set of JSON-encoded public keys, one of which corresponds to the key used to secure the JOSE object.
	 * </p>
	 * 
	 * @return a URL or null
	 */
	@JsonProperty("jku")
	URI getJWKSetURL();
	
	/**
	 * <p>
	 * Returns the public JWK that corresponds to the key used to secure the JOSE object.
	 * </p>
	 * 
	 * @return a JWK or null
	 */
	@JsonProperty("jwk")
	Map<String, Object> getJWK();
	
	/**
	 * <p>
	 * Returns the id of the key that was used to secure the JOSE object.
	 * </p>
	 * 
	 * @return a key id or null
	 */
	@JsonProperty("kid")
	String getKeyId();
	
	/**
	 * <p>
	 * Returns the URL of the X.509 certificate or certificates chain that corresponds to the key that was used to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The certificates chain located at the URL must be in PEM format. The certificate containing the public key corresponding to the key that was used to secure the JOSE object must be the first
	 * certificate in the chain.
	 * </p>
	 * 
	 * @return a URL or null
	 */
	@JsonProperty("x5u")
	URI getX509CertificateURL();

	/**
	 * <p>
	 * Returns the X.509 certificates chain that corresponds to the key that was used to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The certificates chain is represented as an array of Base64URL encoded DER PKIX certificates. The certificate containing the public key corresponding to the key that was used to secure the JOSE
	 * object must be the first certificate in the chain.
	 * </p>
	 * 
	 * @return a certificates chain or null
	 */
	@JsonProperty("x5c")
	String[] getX509CertificateChain();
	
	/**
	 * <p>
	 * Returns the X.509 SHA1 thumbprint of the certificate that corresponds to the key that was used to secure the JOSE object.
	 * </p>
	 * 
	 * @return an X.509 SHA1 thumbprint or null
	 */
	@JsonProperty("x5t")
	String getX509CertificateSHA1Thumbprint();
	
	/**
	 * <p>
	 * Returns the X.509 SHA256 thumbprint of the certificate that corresponds to the key that was used to secure the JOSE object.
	 * </p>
	 * 
	 * @return an X.509 SHA256 thumbprint or null
	 */
	@JsonProperty("x5t#S256")
	String getX509CertificateSHA256Thumbprint();
	
	/**
	 * <p>
	 * Returns the media type of the complete JOSE object.
	 * </p>
	 * 
	 * <p>
	 * To keep messages compact in common situations, the {@code application/} prefix of a media type can be omitted when no other {@code /} appears in the value.
	 * </p>
	 * 
	 * @return a media type or null
	 */
	@JsonProperty("typ")
	String getType();
	
	/**
	 * <p>
	 * Returns the media type of the JOSE object payload.
	 * </p>
	 * 
	 * <p>
	 * To keep messages compact in common situations, the {@code application/} prefix of a media type can be omitted when no other {@code /} appears in the value.
	 * </p>
	 * 
	 * @return a media type or null
	 */
	@JsonProperty("cty")
	String getContentType();
	
	/**
	 * <p>
	 * Returns the set of custom parameters that must be understood and processed.
	 * </p>
	 * 
	 * <p>
	 * Critical parameters are not registered JOSE header parameters and they are present in header's custom parameters.
	 * </p>
	 * 
	 * @return a set of custom parameters or null
	 */
	@JsonProperty("crit")
	Set<String> getCritical();
	
	/**
	 * <p>
	 * Returns the map of custom parameters.
	 * </p>
	 * 
	 * @return a map of custom parameters
	 */
	@JsonAnyGetter
	Map<String, Object> getCustomParameters();
	
	/**
	 * <p>
	 * Returns the actual key that was used to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * Depending on the context it may refer to the private key that was used to sign/encrypt the JOSE object or to the public key that was used to verify/decrypt the JOSE object.
	 * </p>
	 * 
	 * @return a JWK
	 */
	@JsonIgnore
	JWK getKey();
	
	/**
	 * <p>
	 * Returns the header encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded header without padding
	 */
	@JsonIgnore
	String getEncoded();
	
	@Override
	int hashCode();
	
	@Override
	boolean equals(Object obj);
}

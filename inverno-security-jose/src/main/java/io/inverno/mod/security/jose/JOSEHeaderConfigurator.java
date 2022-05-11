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

import java.net.URI;
import java.util.Map;

/**
 * <p>
 * A JOSE header configurator is used in {@link JOSEObjectBuilder} or {@link JsonJOSEObjectBuilder} to configure JOSE headers when building JOSE objects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JOSE header configurator type
 */
public interface JOSEHeaderConfigurator<A extends JOSEHeaderConfigurator<A>> {

	/**
	 * <p>
	 * specifies the cryptographic algorithm to use to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * Depending on the type of JOSE object to build, it might designate a signature or a key management algorithm.
	 * </p>
	 * 
	 * @param alg a JWA algorithm
	 * 
	 * @return this builder
	 */
	A algorithm(String alg);

	/**
	 * <p>
	 * Specifies the JWK Set URL that contains the key to use to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The URL must points to a set of JSON-encoded public keys.
	 * </p>
	 * 
	 * @param jku the JWK Set URL
	 * 
	 * @return this builder
	 */
	A jwkSetURL(URI jku);

	/**
	 * <p>
	 * Specifies the JWK to use to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * It is represented as a map to comply with the JOSE object definition, the builder eventually tries to resolve an actual JWK based on these parameters.
	 * </p>
	 * 
	 * @param jwk a JWK as a map
	 * 
	 * @return this builder
	 */
	A jwk(Map<String, Object> jwk);

	/**
	 * <p>
	 * Specifies the id of the key to use to secure the JOSE object.
	 * </p>
	 * 
	 * @param kid a key id
	 * 
	 * @return this builder
	 */
	A keyId(String kid);

	/**
	 * <p>
	 * Specifies the URL of the X.509 certificate or certificates chain that corresponds to the key to use to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The certificates chain located at the URL must be in PEM format. The first certificate in the chain must correspond to the key to use to secure the JOSE object.
	 * </p>
	 * 
	 * @param x5u a URL
	 * 
	 * @return this builder
	 */
	A x509CertificateURL(URI x5u);

	/**
	 * <p>
	 * Specifies the X.509 certificates chain that corresponds to the key to use to secure the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The certificates chain must be represented as an array of Base64URL encoded DER PKIX certificates. The first certificate in the chain must correspond to the key to use to secure the JOSE
	 * object.
	 * </p>
	 * 
	 * @param x5c a certificates chain
	 * 
	 * @return this builder
	 */
	A x509CertificateChain(String[] x5c);

	/**
	 * <p>
	 * Specifies the X.509 SHA1 thumbprint of the certificate that corresponds to the key to use to secure the JOSE object.
	 * </p>
	 * 
	 * @param x5t an X.509 SHA1 thumbprint
	 * 
	 * @return this builder
	 */
	A x509CertificateSHA1Thumbprint(String x5t);

	/**
	 * <p>
	 * Specifies the X.509 SHA256 thumbprint of the certificate that corresponds to the key to use to secure the JOSE object.
	 * </p>
	 * 
	 * @param x5t_S256 an X.509 SHA256 thumbprint
	 * 
	 * @return this builder
	 */
	A x509CertificateSHA256Thumbprint(String x5t_S256);

	/**
	 * <p>
	 * Specifies the media type of the complete JOSE object.
	 * </p>
	 * 
	 * <p>
	 * To keep messages compact in common situations, the {@code application/} prefix of a media type can be omitted when no other {@code /} appears in the value.
	 * </p>
	 * 
	 * @param typ a media type
	 * 
	 * @return this builder
	 */
	A type(String typ);

	/**
	 * <p>
	 * Specifies the media type of the JOSE object payload.
	 * </p>
	 * 
	 * <p>
	 * To keep messages compact in common situations, the {@code application/} prefix of a media type can be omitted when no other {@code /} appears in the value.
	 * </p>
	 * 
	 * <p>
	 * The builder might use this media type to determine how to serialize/deserialize a JOSE object payload.
	 * </p>
	 * 
	 * @param cty a media type
	 * 
	 * @return this builder
	 */
	A contentType(String cty);

	/**
	 * <p>
	 * Specifies the set of custom parameters that must be understood and processed.
	 * </p>
	 * 
	 * <p>
	 * Critical parameters can not be registered JOSE header parameters and they must be present in header's custom parameters.
	 * </p>
	 * 
	 * @param crit a set of custom parameters
	 * 
	 * @return this builder
	 */
	A critical(String... crit);

	/**
	 * <p>
	 * Specifies a custom parameter to add to the header.
	 * </p>
	 *
	 * @param key   the custom parameter name
	 * @param value the custom parameter value
	 *
	 * @return this builder
	 */
	A addCustomParameter(String key, Object value);
}

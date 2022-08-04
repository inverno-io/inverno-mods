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
package io.inverno.mod.security.jose.jwk;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWK URL resolver is used within {@link JWKFactory} and {@link JWKBuilder} to resolve JWK Set resources from JWK Set URL (i.e. {@code jku}) or X.509 certificate from X.509 URLs (i.e. {@code x5u}).
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWKURLResolver {
	
	/**
	 * <p>
	 * Resolves a JSON JWK or JWK set at the specified location and returns corresponding keys represented as maps.
	 * </p>
	 * 
	 * @param jku the URI of the JSON JWK or JWK set resource
	 * 
	 * @return a publisher of parsed keys represented as maps
	 * 
	 * @throws JWKResolveException if there was an error resolving the resource
	 */
	Publisher<Map<String, Object>> resolveJWKSetURL(URI jku) throws JWKResolveException;
	
	/**
	 * <p>
	 * Resolves the X.509 certificates chain at the specified location as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.5">RFC7515 Section 4.1.5</a>
	 * </p>
	 * 
	 * @param x5u the URI of the X.509 certificate chain
	 * 
	 * @return a single X.509 certificates chain publisher
	 * 
	 * @throws JWKResolveException if there was an error resolving the certificates chain
	 */
	Mono<List<X509Certificate>> resolveX509CertificateURL(URI x5u) throws JWKResolveException;
}

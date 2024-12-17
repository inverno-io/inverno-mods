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

package io.inverno.mod.security.jose.internal.jwk;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.JOSEObjectBuilder;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A wrapper for the {@link JWKURLResolver} which can switch off JWK Set URL and X.509 certificate URL resolution by configuration.
 * </p>
 * 
 * <p>
 * The {@link JWKURLResolver} is used in {@link GenericJWKService#read(java.net.URI) } to resolve JWK sets explicitly and indirectly in {@link JOSEObjectBuilder} to resolve keys from the {@code jku}
 * properties. We only want to disable resolution when building or reading JOSE objects and always have it enabled otherwise.
 * </p>
 * 
 * <p>
 * The URL resolution can be disabled by configuration (see {@link JOSEConfiguration#resolve_jku()} and {@link JOSEConfiguration#resolve_x5u()}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "switchableJWKURLResolver", visibility = Bean.Visibility.PRIVATE )
public class SwitchableJWKURLResolver implements JWKURLResolver {

	private static final Logger LOGGER = LogManager.getLogger(SwitchableJWKURLResolver.class);
	
	private final JOSEConfiguration configuration;
	
	private final JWKURLResolver urlResolver;
	
	/**
	 * <p>
	 * Wraps the specified JWK URL resolver to make it switchable.
	 * </p>
	 *
	 * @param configuration the JOSE module configuration
	 * @param urlResolver   the module JWK URL resolver
	 */
	public SwitchableJWKURLResolver(JOSEConfiguration configuration, JWKURLResolver urlResolver) {
		this.configuration = configuration;
		this.urlResolver = urlResolver;
	}
	
	@Override
	public Publisher<Map<String, Object>> resolveJWKSetURL(URI jku) throws JWKResolveException {
		if(!this.configuration.resolve_jku()) {
			return Mono.fromRunnable(() -> LOGGER.warn("JWK set URL resolver is disabled"));
		}
		return this.urlResolver.resolveJWKSetURL(jku);
	}

	@Override
	public Mono<List<X509Certificate>> resolveX509CertificateURL(URI x5u) throws JWKResolveException {
		if(!this.configuration.resolve_x5u()) {
			return Mono.fromRunnable(() -> LOGGER.warn("X509 certificate URL resolver is disabled"));
		}
		return this.urlResolver.resolveX509CertificateURL(x5u);
	}
}

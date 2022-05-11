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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON Web Key URL resolver implementation.
 * </p>
 *
 * <p>
 * This is an overridable bean which can be overriden by injecting a custom {@link JWKURLResolver} instance when building the JOSE module.
 * </p>
 * 
 * <p>
 * This implementation uses an X.509 certificate path validator to be able to validate the resolved X.509 certificates and a resource service to load resources.
 * </p>
 * 
 * <p>
 * The URL resolution will be disabled if the optional resource service is missing or it can be disabled by configuration (see {@link JOSEConfiguration#resolve_jku()} and
 * {@link JOSEConfiguration#resolve_x5u()}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Overridable
@Bean( name = "jwkURLResolver", visibility = Bean.Visibility.PRIVATE )
public class GenericJWKURLResolver implements @Provide JWKURLResolver {

	private static final Logger LOGGER = LogManager.getLogger(GenericJWKURLResolver.class);
	
	private final JOSEConfiguration configuration;
	private final X509JWKCertPathValidator certPathValidator;
	private final ObjectMapper mapper;
	
	private ResourceService resourceService;
	
	/**
	 * <p>
	 * Creates a generic JWK URL resolver.
	 * </p>
	 * 
	 * @param configuration     the JOSE module configuration
	 * @param certPathValidator an X.509 Certificate path validator
	 * @param mapper            an object mapper
	 */
	public GenericJWKURLResolver(JOSEConfiguration configuration, X509JWKCertPathValidator certPathValidator, ObjectMapper mapper) {
		this.configuration = configuration;
		this.certPathValidator = certPathValidator;
		this.mapper = mapper;
	}

	/**
	 * <p>
	 * Sets the resource service.
	 * </p>
	 * 
	 * @param resourceService a resource service
	 */
	public void setResourceService(ResourceService resourceService) {
		this.resourceService = resourceService;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Publisher<Map<String, Object>> resolveJWKSetURL(URI jku) throws JWKResolveException {
		return Mono.justOrEmpty(jku)
			.filter(ign -> {
				if(!this.configuration.resolve_jku()) {
					LOGGER.warn("JWK set URL resolver is disabled");
					return false;
				}
				else if(this.resourceService == null) {
					LOGGER.warn("JWK set URL resolver is disabled: missing resource service");
					return false;
				}
				return true;
			})
			.flatMapMany(uri -> Flux.from(this.resourceService.getResource(uri).read()
				.orElseThrow(() -> new JWKResolveException("Unable to retrieve JWK set from URL, resource is not readable: " + uri)))
				.reduceWith(() -> {
						try {
							return Pipe.open();
						}
						catch(IOException e) {
							throw new IllegalStateException("Can't open pipe", e);
						}
					},
					(pipe, chunk) -> {
						try {
							chunk.getBytes(0, pipe.sink(), chunk.readableBytes());
						}
						catch(IOException e) {
							throw new JWKResolveException("Error resolving JWK Set from URL: " + uri, e);
						}
						finally {
							chunk.release();
						}
						return pipe;
					}
				)
				.flatMapMany(pipe -> {
					try {
						pipe.sink().close();
						try(InputStream jkuStream = Channels.newInputStream(pipe.source())) {
							Map<String, Object> parsedJku;
							try {
								parsedJku = this.mapper.readerForMapOf(Object.class).readValue(jkuStream);
							}
							catch(JsonProcessingException e) {
								throw new JWKResolveException("Error reading JWK", e);
							}

							if(parsedJku.containsKey("keys")) {
								return Flux.fromIterable((List<Map<String,Object>>)parsedJku.get("keys"));
							}
							else {
								return Flux.just(parsedJku);
							}
						}
					}
					catch(IOException e) {
						throw new JWKResolveException("Error resolving JWK Set from URL: " + uri, e);
					}
				})
				.onErrorStop()
			);
	}
	
	@Override
	public Mono<X509Certificate> resolveX509CertificateURL(URI x5u) throws JWKResolveException {
		return Mono.justOrEmpty(x5u)
			.filter(ign -> {
				if(!this.configuration.resolve_x5u()) {
					LOGGER.warn("X509 certificate URL resolver is disabled");
					return false;
				}
				else if(this.resourceService == null) {
					LOGGER.warn("X509 certificate URL resolver is disabled: missing resource service");
					return false;
				}
				return true;
			})
			.flatMap(uri -> Flux.from(this.resourceService.getResource(uri).read()
				.orElseThrow(() -> new JWKResolveException("Unable to retrieve X.509 certificate chain from URL, resource is not readable: " + uri)))
				.reduceWith(() -> {
						try {
							return Pipe.open();
						}
						catch(IOException e) {
							throw new IllegalStateException("Can't open pipe", e);
						}
					}, 
					(pipe, chunk) -> {
						try {
							chunk.getBytes(0, pipe.sink(), chunk.readableBytes());
						}
						catch(IOException e) {
							throw new JWKResolveException("Error resolving X.509 certificate chain from URL: " + uri, e);
						}
						finally {
							chunk.release();
						}
						return pipe;
					}
				)
				.flatMap(pipe -> {
					try {
						pipe.sink().close();
						try(InputStream x5uStream = Channels.newInputStream(pipe.source())) {
							CertificateFactory cf =  CertificateFactory.getInstance("X.509");
							List<X509Certificate> certificates = cf.generateCertificates(x5uStream).stream().map(c -> (X509Certificate)c).collect(Collectors.toList());
							return this.certPathValidator.validate(certificates);
						}
					} 
					catch(IOException | CertificateException e) {
						throw new JWKResolveException("Error resolving X.509 certificate chain from URL: " + uri, e);
					}
				})
				.onErrorStop()
			);
	}
}

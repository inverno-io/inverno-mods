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
import io.inverno.core.annotation.Provide;
import io.inverno.mod.security.jose.JOSEHeader;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKFactory;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKFactory;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKBuildException;
import io.inverno.mod.security.jose.jwk.JWKFactory;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKURLResolver;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import io.inverno.mod.security.jose.jwk.ec.ECJWKFactory;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWKFactory;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import io.inverno.mod.security.jose.jwk.okp.EdECJWKFactory;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import io.inverno.mod.security.jose.jwk.okp.XECJWKFactory;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWK;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWKFactory;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWKFactory;
import io.inverno.mod.security.jose.jws.JWSReadException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON Web Key service.
 * </p>
 * 
 * <p>
 * This implementation relies on standard JWK factories to create, read and generate JSON Web keys. Custom JWK factories can also be injected when building the JOSE module.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "jwkService" )
public final class GenericJWKService implements @Provide JWKService {
	
	private static final Logger LOGGER = LogManager.getLogger(GenericJWKService.class);
	
	private final GenericECJWKFactory ecJWKFactory;
	private final GenericRSAJWKFactory rsaJWKFactory;
	private final GenericOCTJWKFactory octJWKFactory;
	private final GenericEdECJWKFactory edecJWKFactory;
	private final GenericXECJWKFactory xecJWKFactory;
	private final GenericPBES2JWKFactory pbes2JWKFactory;
	private final JWKStore jwkStore;
	private final JWKURLResolver urlResolver;
	private final ObjectMapper mapper;
	
	private final List<JWKFactory<?, ?, ?>> jwkFactories;

	/**
	 * <p>
	 * Creates a generic JWK service.
	 * </p>
	 *
	 * @param ecJWKFactory    the Elliptic Curve JWK factory
	 * @param rsaJWKFactory   the RSA JWK factory
	 * @param octJWKFactory   the Octet JWK factory
	 * @param edecJWKFactory  the Edwards-curve JWK factory
	 * @param xecJWKFactory   the extended Elliptic Curve JWK factory
	 * @param pbes2JWKFactory the password-based JWK factory
	 * @param jwkStore        a JWK store
	 * @param urlResolver     a JWK URL resolver
	 * @param mapper          an object mapper
	 */
	public GenericJWKService(
			GenericECJWKFactory ecJWKFactory, 
			GenericRSAJWKFactory rsaJWKFactory, 
			GenericOCTJWKFactory octJWKFactory, 
			GenericEdECJWKFactory edecJWKFactory, 
			GenericXECJWKFactory xecJWKFactory, 
			GenericPBES2JWKFactory pbes2JWKFactory, 
			JWKStore jwkStore,
			JWKURLResolver urlResolver, 
			ObjectMapper mapper
		) {
		this.ecJWKFactory = ecJWKFactory;
		this.rsaJWKFactory = rsaJWKFactory;
		this.octJWKFactory = octJWKFactory;
		this.edecJWKFactory = edecJWKFactory;
		this.xecJWKFactory = xecJWKFactory;
		this.pbes2JWKFactory = pbes2JWKFactory;
		this.jwkStore = jwkStore;
		this.urlResolver = urlResolver;
		this.mapper = mapper;
		this.jwkFactories = new LinkedList<>();
		this.setJWKFactories(null);
	}
	
	/**
	 * <p>
	 * Sets custom JWK factories.
	 * </p>
	 * 
	 * <p>
	 * Standard JWK factories should be already injected into the constructor, they will be ignored if they are present in the specified list of JWK factories.
	 * </p>
	 * 
	 * <p>
	 * Custom JWK factories are prioritized over the standard JWK factories to allow override.
	 * </p>
	 * 
	 * @param jwkFactories a list of custom JWK factories
	 */
	public void setJWKFactories(List<JWKFactory<?, ?, ?>> jwkFactories) {
		this.jwkFactories.clear();
		this.jwkFactories.add(this.ecJWKFactory);
		this.jwkFactories.add(this.rsaJWKFactory);
		this.jwkFactories.add(this.octJWKFactory);
		this.jwkFactories.add(this.edecJWKFactory);
		this.jwkFactories.add(this.xecJWKFactory);
		this.jwkFactories.add(this.pbes2JWKFactory);
		if(jwkFactories != null && !jwkFactories.isEmpty()) {
			jwkFactories.stream()
				.filter(factory -> factory != null && factory != this.ecJWKFactory && factory != this.rsaJWKFactory && factory != this.octJWKFactory && factory != this.edecJWKFactory && factory != this.xecJWKFactory && factory != this.pbes2JWKFactory)
				.forEach(this.jwkFactories::add);
		}
	}

	@Override
	public ECJWKFactory<? extends ECJWK, ?, ?> ec() {
		return this.ecJWKFactory;
	}

	@Override
	public RSAJWKFactory<? extends RSAJWK, ?, ?> rsa() {
		return this.rsaJWKFactory;
	}

	@Override
	public OCTJWKFactory<? extends OCTJWK, ?, ?> oct() {
		return this.octJWKFactory;
	}

	@Override
	public EdECJWKFactory<? extends EdECJWK, ?, ?> edec() {
		return this.edecJWKFactory;
	}

	@Override
	public XECJWKFactory<? extends XECJWK, ?, ?> xec() {
		return this.xecJWKFactory;
	}

	@Override
	public PBES2JWKFactory<? extends PBES2JWK, ?, ?> pbes2() {
		return this.pbes2JWKFactory;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Publisher<? extends JWK> read(String jwk) throws JWKReadException, JWKBuildException, JWKProcessingException {
		final Map<String, Object> parsedJWK;
		try {
			parsedJWK = this.mapper.readerForMapOf(Object.class).readValue(jwk);
		} 
		catch(JsonProcessingException e) {
			throw new JWKReadException("Error reading JWK", e);
		}
		
		if(parsedJWK.containsKey("keys")) {
			final List<Object> keysValue;
			try {
				keysValue = (List<Object>)parsedJWK.get("keys");
			} 
			catch (ClassCastException e) {
				throw new JWSReadException("Invalid JWK set", e);
			}
			
			// We are parsing a JWK set
			// for each entry we must invoke the right factory
			// we SHOULD also ignore those that we don't understand (https://datatracker.ietf.org/doc/html/rfc7517#section-5) or that we can't load properly
			// this could be delegated to the caller which can decide what to do with errors in the resulting publisher (e.g. onErrorContinue())
			// by doing this we are consistent with the single JWK load
			return (Publisher<JWK>) Flux.fromIterable(keysValue)
				.flatMap(keyElementValue -> {
					final Map<String, Object> keyElement;
					try {
						keyElement = (Map<String, Object>)keyElementValue;
					} 
					catch (ClassCastException e) {
						throw new JWSReadException("Invalid JWK set", e);
					}
					return this.read(keyElement, true);
				});
		}
		else {
			return this.read(parsedJWK, true);
		}
	}

	@Override
	public Publisher<? extends JWK> read(Map<String, Object> jwk) throws JWKReadException, JWKBuildException, JWKProcessingException {
		return this.read(jwk, true);
	}
	
	@Override
	public Publisher<? extends JWK> read(URI uri) throws JWKReadException, JWKResolveException, JWKBuildException, JWKProcessingException {
		return Flux.from(this.urlResolver.resolveJWKSetURL(uri)).flatMap(jwk -> this.read(jwk, true));
	}

	@Override
	public Publisher<? extends JWK> read(JOSEHeader header) throws JWKReadException, JWKResolveException, JWKBuildException, JWKProcessingException {
		Map<String, Object> jwk = header.getJWK();
		if(jwk != null && !jwk.isEmpty()) {
			// Here we basically ignore the rest of the header if the specified JWK is a match
			// this is actually quite natural: if the specified JWK is not match, the header is then inconsistent and must be rejected
			// this behaviour must be documented
			return this.read(this.mergeWithHeader(header.getJWK(), header), false);
		}
		
		// otherwise we return the keys from jku that are consistent with the header (others, including erroneous one are simply filtered out)
		// and finally we return the key corresponding to header's kid, x5u, x5c, x5t, x5t#S256 in that order, failing if inconsistencies have been detected
		return Flux.concat(
			this.resolveJku(header), 
			Flux.defer(() -> {
				return Flux.from(this.read(this.mergeWithHeader(Map.of(), header), false)).onErrorResume(e -> Mono.empty());
			})
		);
	}
	
	@Override
	public Publisher<? extends JWK> generate(String alg, Map<String, Object> parameters) throws JWKGenerateException, JWKProcessingException {
		if(StringUtils.isBlank(alg)) {
			throw new JWKGenerateException("Algorithm is blank");
		}
		
		return Flux.defer(() -> {
			JWKGenerateException error = new JWKGenerateException("Failed to generate JWK");
			return Flux.fromIterable(this.jwkFactories)
				.filter(factory -> factory.supportsAlgorithm(alg))
				.switchIfEmpty(Mono.error(() -> new JWKReadException("No JWK factory found supporting algorithm " + alg)))
				.concatMap(factory -> factory.generate(alg, parameters)
					.doOnError(e -> {
						error.addSuppressed(e);
						LOGGER.debug("Error generating JWK with factory: " + factory, e);
					})
					.onErrorResume(e -> Mono.empty())
				)
				.switchIfEmpty(Mono.error(error));
		});
	}
	
	@Override
	public JWKStore store() {
		return this.jwkStore;
	}
	
	/**
	 * <p>
	 * Reads the JWK represented as a map.
	 * </p>
	 * 
	 * <p>
	 * This method will try to read the JWK with all JWK factories that supports the key type and algorithms defined in the specified JWK.
	 * </p>
	 * 
	 * <p>
	 * The resulting publisher will fail if no key could be resolved.
	 * </p>
	 * 
	 * @param jwk             a JWK represented as a map
	 * @param requiresKeyType true to fail if the key type parameter is missing, false otherwise
	 * 
	 * @return a JWK publisher
	 * 
	 * @throws JWKReadException       if there was an error reading the JWK
	 * @throws JWKBuildException      if there was an error building a JWK
	 * @throws JWKProcessingException if there was a JWK processing error
	 */
	private Publisher<? extends JWK> read(Map<String, Object> jwk, boolean requiresKeyType) throws JWKReadException, JWKBuildException, JWKProcessingException {
		if(requiresKeyType && StringUtils.isBlank((String)jwk.get("kty"))) {
			throw new JWKReadException("Key type is blank");
		}
		
		try {
			String kty = (String)jwk.get("kty");
			String alg = (String)jwk.get("alg");
			
			return Flux.defer(() -> {
				JWKReadException error = new JWKReadException("Failed to resolve JWK");
				return Flux.fromIterable(this.jwkFactories)
					.filter(factory -> kty == null || factory.supports(kty))
					.filter(factory -> alg == null || factory.supportsAlgorithm(alg))
					.switchIfEmpty(Mono.error(() -> new JWKReadException("No JWK factory found supporting key type " + kty + " and algorithm " + alg)))
					.concatMap(factory -> factory.read(jwk)
						.doOnError(e -> {
							error.addSuppressed(e);
							LOGGER.debug("Error resolving JWK with factory: " + factory, e);
						})
						.onErrorResume(e -> Mono.empty())
					)
					.switchIfEmpty(Mono.error(error));
			});
		}
		catch(ClassCastException e) {
			throw new JWKReadException("Invalid JWK string", e);
		}
	}
	
	/**
	 * <p>
	 * Merges the specified header with the specified JWK represented as a map and returns the resulting map.
	 * </p>
	 *
	 * @param jwk    a JWK represented as a map
	 * @param header a JOSE header
	 *
	 * @return the result of the merge
	 *
	 * @throws JWKReadException if there was an error during the merge
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> mergeWithHeader(Map<String, Object> jwk, JOSEHeader header) throws JWKReadException {
		Map<String, Object> mergedJWK = new HashMap<>(jwk);

		String alg = header.getAlgorithm();
		mergedJWK.compute("alg", (k,v) -> {
			if(v == null) {
				return alg;
			}
			else if(!v.equals(alg)) {
				throw new JWKReadException("Inconsistent JWK: algorithm does not match JOSE header");
			}
			return v;
		});

		String kid = header.getKeyId();
		mergedJWK.compute("kid", (k,v) -> {
			if(v == null) {
				return kid;
			}
			else if(!StringUtils.isBlank(kid) && !v.equals(kid)) {
				throw new JWKReadException("Inconsistent JWK: key id does not match JOSE header");
			}
			return v;
		});

		URI x5u = header.getX509CertificateURL();
		mergedJWK.compute("x5u", (k,v) -> {
			if(v == null) {
				return x5u;
			}
			else if(x5u != null) {
				if(v instanceof URL) {
					try {
						v = ((URL) v).toURI().normalize();
					} 
					catch(URISyntaxException e) {
						throw new JWKReadException("Invalid x5u which must be a valid URI", e);
					}
				}
				else if(v instanceof CharSequence) {
					v = URI.create(v.toString()).normalize();
				}
				else if(!(v instanceof URI)) {
					throw new JWKReadException(v.getClass() + " cannot be converted to URI");
				}

				if(!v.equals(x5u.normalize())) {
					throw new JWKReadException("Inconsistent JWK: X.509 URL doesn't match JOSE header");
				}
			}
			return v;
		});

		String[] x5c = header.getX509CertificateChain();
		mergedJWK.compute("x5c", (k,v) -> {
			if(v == null) {
				return x5c;
			}
			else if(x5c != null) {
				if(v instanceof Collection) {
					v = ((Collection<String>)v).toArray(String[]::new);
				}
				else if(!(v instanceof String[])) {
					throw new JWKReadException("x5c can't be converted to String[]: " + v.getClass());
				}

				if(Arrays.compare(x5c, (String[])v) != 0) {
					throw new JWKReadException("Inconsistent JWK: X.509 chain does not match JOSE header");
				}
			}
			return v;
		});

		String x5t = header.getX509CertificateSHA1Thumbprint();
		mergedJWK.compute("x5t", (k,v) -> {
			if(v == null) {
				return x5t;
			}
			else if(!StringUtils.isBlank(x5t) && !v.equals(x5t)) {
				throw new JWKReadException("Inconsistent JWK: X.509 thumbprint does not match JOSE header");
			}
			return v;
		});

		String x5t_S256 = header.getX509CertificateSHA256Thumbprint();
		mergedJWK.compute("x5t#S256", (k,v) -> {
			if(v == null) {
				return x5t_S256;
			}
			else if(!StringUtils.isBlank(x5t_S256) && !v.equals(x5t_S256)) {
				throw new JWKReadException("Inconsistent JWK: X.509 SHA-256 thumbprint does not match JOSE header");
			}
			return v;
		});
		return mergedJWK;
	}
	
	/**
	 * <p>
	 * Resolves the JWK Set URL in the specified JOSE header using the JWK URL resolver.
	 * </p>
	 *
	 * <p>
	 * This method merges the JWK resolved as map with the specified header before reading the result to make sure the keys are consistent with the JOSE header.
	 * </p>
	 *
	 * <p>
	 * Only the valid and consistent keys are returned by the resulting JWK publisher.
	 * </p>
	 *
	 * @param header a JOSE header
	 *
	 * @return a JWK publisher
	 *
	 * @throws JWKReadException       if there was an error reading a JWK
	 * @throws JWKResolveException    if there was an error resolving the JWK set
	 * @throws JWKBuildException      if there was an error building a JWK
	 * @throws JWKProcessingException if there was a processing error
	 */
	private Flux<? extends JWK> resolveJku(JOSEHeader header) throws JWKReadException, JWKResolveException, JWKBuildException, JWKProcessingException {
		return Flux.from(this.urlResolver.resolveJWKSetURL(header.getJWKSetURL()))
			.mapNotNull(jwk -> {
				try {
					return this.mergeWithHeader(jwk, header);
				}
				catch(Exception e) {
					LOGGER.debug(() -> {
						try {
							return "Key is inconsistent with JOSE header: " + this.mapper.writeValueAsString(jwk);
						}
						catch(JsonProcessingException jpe) {
							throw new UncheckedIOException(jpe);
						}
					}, e);
					return null;
				}
			})
			.flatMap(jwk -> Flux.from(this.read(jwk, true)).onErrorResume(e -> Mono.empty()));
	}
}

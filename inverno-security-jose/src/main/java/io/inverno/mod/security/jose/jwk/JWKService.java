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

import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEHeader;
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
import java.net.URI;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * <p>
 * The JWK Service is the main entry point for creating, reading and generating JSON Web Keys.
 * </p>
 * 
 * <p>
 * It allows to fluently create JWK objects using a {@link JWKBuilder}, read or resolve JWK from JSON serialized JWK or JOSE headers or generate new JWK using a {@link JWKGenerator}.
 * </p>
 * 
 * <p>
 * It supports and expose explicitly all registered key types specified in <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a> and
 * <a href="https://datatracker.ietf.org/doc/html/rfc8037">RFC8037</a> but it can be extended with custom {@link JWKFactory} implementations to support other key types.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWKService {

	/**
	 * <p>
	 * Returns the Elliptic Curve JWK factory.
	 * </p>
	 * 
	 * @return the Elliptic Curve JWK factory
	 */
	ECJWKFactory<? extends ECJWK, ?, ?> ec();
	
	/**
	 * <p>
	 * Returns the RSA JWK factory.
	 * </p>
	 * 
	 * @return the RSA JWK factory
	 */
	RSAJWKFactory<? extends RSAJWK, ? ,?> rsa();
	
	/**
	 * <p>
	 * Returns the Octet JWK factory.
	 * </p>
	 * 
	 * @return the Octet JWK factory
	 */
	OCTJWKFactory<? extends OCTJWK, ?, ?> oct();
	
	/**
	 * <p>
	 * Returns the Edward-Curve JWK factory.
	 * </p>
	 * 
	 * @return the Edward-Curve JWK factory
	 */
	EdECJWKFactory<? extends EdECJWK, ? ,?> edec();
	
	/**
	 * <p>
	 * Returns the extended Elliptic Curve JWK factory.
	 * </p>
	 * 
	 * @return the extended Elliptic Curve JWK factory
	 */
	XECJWKFactory<? extends XECJWK, ? ,?> xec();
	
	/**
	 * <p>
	 * Returns the Password-Based JWK factory.
	 * </p>
	 * 
	 * @return the Password-Based JWK factory
	 */
	PBES2JWKFactory<? extends PBES2JWK, ?, ?> pbes2();

	/**
	 * <p>
	 * Reads the specified JWK or JWK set JSON serialized string and resolves and returns corresponding keys.
	 * </p>
	 * 
	 * <p>
	 * This method basically iterates over all JWK factories and tries to resolve the key when the factory supports the specified key type and algorithm. The resulting publisher will fail when no key
	 * could have been resolved in which case a single {@link JWKReadException} will be emitted with suppressed errors corresponding to each factories.
	 * </p>
	 * 
	 * <p>
	 * Note that this method should also fail when a key is missing the key type. If the key type is known it is preferable to use the corresponding {@link JWKFactory} to avoid unnecessary processing.
	 * </p>
	 *
	 * @param jwk a JSON serialized JWK or JWK set
	 *
	 * @return a publisher of keys
	 *
	 * @throws JWKReadException       if there was an error reading the JSON string or a particular key
	 * @throws JWKBuildException      if there was an error building a key
	 * @throws JWKProcessingException if there was a processing error
	 */
	Publisher<? extends JWK> read(String jwk) throws JWKReadException, JWKBuildException, JWKProcessingException;
	
	/**
	 * <p>
	 * Reads the JWK or JWK set JSON serialized string at the specified URI.
	 * </p>
	 *
	 * <p>
	 * This method basically iterates over all JWK factories and tries to resolve the key when the factory supports the specified key type and algorithm. The resulting publisher will fail when no key
	 * could have been resolved in which case a single {@link JWKReadException} will be emitted with suppressed errors corresponding to each factories.
	 * </p>
	 * 
	 * <p>
	 * Note that this method should also fail when a key is missing the key type. If the key type is known it is preferable to use the corresponding {@link JWKFactory} to avoid unnecessary processing.
	 * </p>
	 * 
	 * <p>
	 * Note that this method will also fail if JWK URL resolution is disabled, either in the module's configuration or if no {@link ResourceService} has been specified.
	 * </p>
	 *
	 * @param uri the URI where to find the JWK or JWK set JSON string
	 *
	 * @return a publisher of keys
	 *
	 * @throws JWKReadException       if there was an error reading the JSON string or a particular key
	 * @throws JWKResolveException    if there was an error resolving the resource from the specified URI
	 * @throws JWKBuildException      if there was an error building a key
	 * @throws JWKProcessingException if there was a processing error
	 */
	Publisher<? extends JWK> read(URI uri) throws JWKReadException, JWKResolveException, JWKBuildException, JWKProcessingException;
	
	/**
	 * <p>
	 * Reads the JWK or JWK set represented in the specified map.
	 * </p>
	 * 
	 * <p>
	 * This method basically iterates over all JWK factories and tries to resolve the key when the factory supports the specified key type and algorithm. The resulting publisher will fail when no key
	 * could have been resolved in which case a single {@link JWKReadException} will be emitted with suppressed errors corresponding to each factories.
	 * </p>
	 * 
	 * <p>
	 * Note that this method should also fail when a key is missing the key type. If the key type is known it is preferable to use the corresponding {@link JWKFactory} to avoid unnecessary processing.
	 * </p>
	 * 
	 * @param jwk a map representing a JWK or a JWK set
	 * 
	 * @return a publisher of keys
	 * 
	 * @throws JWKReadException       if there was an error reading the JSON string or a particular key
	 * @throws JWKBuildException      if there was an error building a key
	 * @throws JWKProcessingException if there was a processing error
	 */
	Publisher<? extends JWK> read(Map<String, Object> jwk) throws JWKReadException, JWKBuildException, JWKProcessingException;
	
	/**
	 * <p>
	 * Tries to resolve the JWK from a JOSE header.
	 * </p>
	 *
	 * <p>
	 * As for other read methods, this method will iterates over all JWK factories and tries to resolve the key that matches the JOSE header when the factory supports the algorithm specified in the
	 * header. The resulting publisher will fail when no key could have been resolved in which case a single {@link JWKReadException} will be emitted with suppressed errors corresponding to each
	 * factories.
	 * </p>
	 *
	 * <p>
	 * Unlike other read methods, this method does not fail when the key type is missing since a JOSE header does not contain the key type.
	 * </p>
	 *
	 * @param header a JOSE header
	 *
	 * @return a publisher of keys
	 *
	 * @throws JWKReadException       if there was an error reading the JOSE header
	 * @throws JWKResolveException    if there was an error resolving the key using a {@link JWKStore} or a {@link JWKURLResolver}
	 * @throws JWKBuildException      if there was an error building the key
	 * @throws JWKProcessingException if there was a processing error
	 */
	Publisher<? extends JWK> read(JOSEHeader header) throws JWKReadException, JWKResolveException, JWKBuildException, JWKProcessingException;
	
	/**
	 * <p>
	 * Generates a new key using the specified parameters.
	 * </p>
	 *
	 * <p>
	 * This is a convenience method that can be used to generate a key using a custom JWK factory, you should prefer using a {@link JWKGenerator} obtained from a specific {@link JWKFactory} to avoid
	 * unnecessary processing.
	 * </p>
	 *
	 * @param alg        a JWA algorithm
	 * @param parameters a map of key parameters
	 *
	 * @return a publisher of keys
	 *
	 * @throws JWKGenerateException   if there was an error generating a key
	 * @throws JWKProcessingException if there was a processing error
	 */
	Publisher<? extends JWK> generate(String alg, Map<String, Object> parameters) throws JWKGenerateException, JWKProcessingException;
	
	/**
	 * <p>
	 * Returns the JWK store.
	 * </p>
	 * 
	 * <p>
	 * The JWK store can be used to store frequently used keys so they can be easily resolved when reading a JOSE object.
	 * </p>
	 * 
	 * <p>
	 * It is recommended to only store trusted keys inside a JWK store to prevent them from being evicted when resolving a JOSE object key.
	 * </p>
	 * 
	 * @return the JWK store
	 */
	JWKStore store();
}

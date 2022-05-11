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
package io.inverno.mod.security.jose.jwt;

import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.JOSEObjectReader;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.jwe.JWEBuilder;
import io.inverno.mod.security.jose.jwe.JWEReader;
import io.inverno.mod.security.jose.jwe.JsonJWEBuilder;
import io.inverno.mod.security.jose.jwe.JsonJWEReader;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jws.JWSBuilder;
import io.inverno.mod.security.jose.jws.JWSReader;
import io.inverno.mod.security.jose.jws.JsonJWSBuilder;
import io.inverno.mod.security.jose.jws.JsonJWSReader;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;

/**
 * <p>
 * The JWT Service is the main entry point for creating and reading JSON Web token objects.
 * </p>
 * 
 * <p>
 * A JSON Web Token is a JSON Web Signature or JSON Web Encryption object of type {@code JWT} and whose payload is a JWT Claims set. The JWT Service allows to fluently create JWT objects using
 * {@link JWSBuilder}, {@link JsonJWSBuilder}, {@link JWEBuilder} or {@link JsonJWEBuilder} and read JWT objects using {@link JWSReader}, {@link JsonJWSReader}, {@link JWEReader} or
 * {@link JsonJWEReader}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWTService {

	/**
	 * <p>
	 * Returns a new JWS JWT builder.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to sign the JWT based on the JWS JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @return a new JWS JWT builder
	 */
	default JWSBuilder<JWTClaimsSet, ?, ?> jwsBuilder() {
		return this.jwsBuilder((Type)JWTClaimsSet.class, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT builder with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to sign the JWT based on the JWS JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWS JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWSBuilder<T, ?, ?> jwsBuilder(Class<T> type) throws JOSEProcessingException {
		return this.jwsBuilder((Type)type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT builder with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to sign the JWT based on the JWS JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWS JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWSBuilder<T, ?, ?> jwsBuilder(Type type) throws JOSEProcessingException {
		return this.jwsBuilder(type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT builder using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The returned builder will use the specified keys to sign the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have been
	 * specified or if they are not consistent with the JWS JOSE header.
	 * </p>
	 *
	 * @param keys the keys to consider to sign the JWT
	 *
	 * @return a new JWS JWT builder
	 */
	default JWSBuilder<JWTClaimsSet, ?, ?> jwsBuilder(Publisher<? extends JWK> keys) {
		return this.jwsBuilder((Type)JWTClaimsSet.class, keys);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT builder with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The returned builder will use the specified keys to sign the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have been
	 * specified or if they are not consistent with the JWS JOSE header.
	 * </p>
	 *
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to sign the JWT
	 *
	 * @return a new JWS JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWSBuilder<T, ?, ?> jwsBuilder(Class<T> type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		return this.jwsBuilder((Type)type, keys);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT builder with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The returned builder will use the specified keys to sign the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have been
	 * specified or if they are not consistent with the JWS JOSE header.
	 * </p>
	 *
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to sign the JWT
	 *
	 * @return a new JWS JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	<T extends JWTClaimsSet> JWSBuilder<T, ?, ?> jwsBuilder(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException;
	
	/**
	 * <p>
	 * Returns a new JWE JWT builder.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to sign the JWT based on the JWE JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @return a new JWE JWT builder
	 */
	default JWEBuilder<JWTClaimsSet, ?, ?> jweBuilder() {
		return this.jweBuilder((Type)JWTClaimsSet.class, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT builder with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to sign the JWT based on the JWE JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWE JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWEBuilder<T, ?, ?> jweBuilder(Class<T> type) throws JOSEProcessingException {
		return this.jweBuilder((Type)type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT builder with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to sign the JWT based on the JWE JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWE JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWEBuilder<T, ?, ?> jweBuilder(Type type) throws JOSEProcessingException {
		return this.jweBuilder(type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT builder using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The returned builder will use the specified keys to sign the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have been
	 * specified or if they are not consistent with the JWE JOSE header.
	 * </p>
	 *
	 * @param keys the keys to consider to sign the JWT
	 *
	 * @return a new JWE JWT builder
	 */
	default JWEBuilder<JWTClaimsSet, ?, ?> jweBuilder(Publisher<? extends JWK> keys) {
		return this.jweBuilder((Type)JWTClaimsSet.class, keys);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT builder with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The returned builder will use the specified keys to sign the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have been
	 * specified or if they are not consistent with the JWE JOSE header.
	 * </p>
	 *
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to sign the JWT
	 *
	 * @return a new JWE JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWEBuilder<T, ?, ?> jweBuilder(Class<T> type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		return this.jweBuilder((Type)type, keys);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT builder with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The returned builder will use the specified keys to sign the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have been
	 * specified or if they are not consistent with the JWE JOSE header.
	 * </p>
	 *
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to sign the JWT
	 *
	 * @return a new JWE JWT builder
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	<T extends JWTClaimsSet> JWEBuilder<T, ?, ?> jweBuilder(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException;
	
	/**
	 * <p>
	 * Returns a new JWS JWT reader.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to verify the JWT signature based on the JWS JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @return a new JWS JWT reader
	 */
	default JWSReader<JWTClaimsSet, ?> jwsReader() {
		return this.jwsReader((Type)JWTClaimsSet.class, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT reader with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to verify the JWT signature based on the JWS JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWS JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWSReader<T, ?> jwsReader(Class<T> type) throws JOSEProcessingException {
		return this.jwsReader((Type)type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT reader with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to verify the JWT signature based on the JWS JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWS JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWSReader<T, ?> jwsReader(Type type) throws JOSEProcessingException {
		return this.jwsReader(type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT reader using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify the JWT signature, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JWS JOSE header.
	 * </p>
	 * 
	 * @param keys the keys to consider to verify the JWT signature
	 * 
	 * @return a new JWS JWT reader
	 */
	default JWSReader<JWTClaimsSet, ?> jwsReader(Publisher<? extends JWK> keys) {
		return this.jwsReader((Type)JWTClaimsSet.class, keys);
	}
	 
	/**
	 * <p>
	 * Returns a new JWS JWT reader with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify the JWT signature, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JWS JOSE header.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to verify the JWT signature
	 * 
	 * @return a new JWS JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWSReader<T, ?> jwsReader(Class<T> type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		return this.jwsReader((Type)type, keys);
	}
	
	/**
	 * <p>
	 * Returns a new JWS JWT reader with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify the JWT signature, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JWS JOSE header.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to verify the JWT signature
	 * 
	 * @return a new JWS JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	<T extends JWTClaimsSet> JWSReader<T, ?> jwsReader(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException;
	
	/**
	 * <p>
	 * Returns a new JWE JWT reader.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to decrypt the JWT based on the JWE JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @return a new JWE JWT reader
	 */
	default JWEReader<JWTClaimsSet, ?> jweReader() {
		return this.jweReader((Type)JWTClaimsSet.class, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT reader with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to decrypt the JWT based on the JWE JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWE JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWEReader<T, ?> jweReader(Class<T> type) throws JOSEProcessingException {
		return this.jweReader((Type)type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT reader with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to decrypt the JWT based on the JWE JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JWE JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JWEReader<T, ?> jweReader(Type type) throws JOSEProcessingException {
		return this.jweReader(type, null);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT reader using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to decrypt the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JWE JOSE header.
	 * </p>
	 * 
	 * @param keys the keys to consider to decrypt the JWT
	 * 
	 * @return a new JWE JWT reader
	 */
	default JWEReader<JWTClaimsSet, ?> jweReader(Publisher<? extends JWK> keys) {
		return this.jweReader((Type)JWTClaimsSet.class, keys);
	}
	
	/**
	 * <p>
	 * Returns a new JWE JWT reader with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to decrypt the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JWE JOSE header.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to decrypt the JWT
	 * 
	 * @return a new JWE JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */	
	default <T extends JWTClaimsSet> JWEReader<T, ?> jweReader(Class<T> type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		return this.jweReader((Type)type, keys);
	}

	/**
	 * <p>
	 * Returns a new JWE JWT reader with a custom JWT Claims set type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to decrypt the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JWE JOSE header.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to decrypt the JWT
	 * 
	 * @return a new JWE JWT reader
	 * 
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */	
	<T extends JWTClaimsSet> JWEReader<T, ?> jweReader(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException;
	
	/**
	 * <p>
	 * Returns a JOSE JWT Object reader to read the specified compact representation.
	 * </p>
	 * 
	 * <p>
	 * This method shall returned a JWS reader or a JWE reader based on the compact representation format.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to verify or decrypt the JWT based on the JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param compact the compact representation to read
	 * 
	 * @return a new JOSE JWT reader
	 * 
	 * @throws JWTReadException if the specified compact representation is invalid or not supported
	 */
	default JOSEObjectReader<JWTClaimsSet, ?, ? extends JOSEObject<JWTClaimsSet, ?>, ?> readerFor(String compact) throws JWTReadException {
		return this.readerFor(compact, (Type)JWTClaimsSet.class, null);
	}
	
	/**
	 * <p>
	 * Returns a JOSE JWT Object reader to read the specified compact representation with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * This method shall returned a JWS reader or a JWE reader based on the compact representation format.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to verify or decrypt the JWT based on the JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param compact the compact representation to read
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JOSE JWT reader
	 * 
	 * @throws JWTReadException if the specified compact representation is invalid or not supported
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JOSEObjectReader<T, ?, ? extends JOSEObject<T, ?>, ?> readerFor(String compact, Class<T> type) throws JWTReadException, JOSEProcessingException {
		return this.readerFor(compact, (Type)type, null);
	}
	
	/**
	 * <p>
	 * Returns a JOSE JWT Object reader to read the specified compact representation with a custom JWT Claims set type.
	 * </p>
	 * 
	 * <p>
	 * This method shall returned a JWS reader or a JWE reader based on the compact representation format.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the JWK to use to verify or decrypt the JWT based on the JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param compact the compact representation to read
	 * @param type The JWT Claims set type
	 * 
	 * @return a new JOSE JWT reader
	 * 
	 * @throws JWTReadException if the specified compact representation is invalid or not supported
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JOSEObjectReader<T, ?, ? extends JOSEObject<T, ?>, ?> readerFor(String compact, Type type) throws JWTReadException, JOSEProcessingException {
		return this.readerFor(compact, type, null);
	}
	
	/**
	 * <p>
	 * Returns a JOSE JWT Object reader to read the specified compact representation using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * This method shall returned a JWS reader or a JWE reader based on the compact representation format.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify or decrypt the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param compact the compact representation to read
	 * @param keys the keys to consider to verify or decrypt the JWT
	 * 
	 * @return a new JOSE JWT reader
	 * 
	 * @throws JWTReadException if the specified compact representation is invalid or not supported
	 */
	default JOSEObjectReader<JWTClaimsSet, ?, ? extends JOSEObject<JWTClaimsSet, ?>, ?> readerFor(String compact, Publisher<? extends JWK> keys) throws JWTReadException, JOSEProcessingException {
		return this.readerFor(compact, (Type)JWTClaimsSet.class, keys);
	}
	
	/**
	 * <p>
	 * Returns a JOSE JWT Object reader to read the specified compact representation with a custom JWT Claims set type using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * This method shall returned a JWS reader or a JWE reader based on the compact representation format.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify or decrypt the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param compact the compact representation to read
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to verify or decrypt the JWT
	 * 
	 * @return a new JOSE JWT reader
	 * 
	 * @throws JWTReadException if the specified compact representation is invalid or not supported
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	default <T extends JWTClaimsSet> JOSEObjectReader<T, ?, ? extends JOSEObject<T, ?>, ?> readerFor(String compact, Class<T> type, Publisher<? extends JWK> keys) throws JWTReadException, JOSEProcessingException {
		return this.readerFor(compact, (Type)type, keys);
	}
	
	/**
	 * <p>
	 * Returns a JOSE JWT Object reader to read the specified compact representation with a custom JWT Claims set type using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * This method shall returned a JWS reader or a JWE reader based on the compact representation format.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify or decrypt the JWT, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param <T>  the JWT Claims set type
	 * @param compact the compact representation to read
	 * @param type The JWT Claims set type
	 * @param keys the keys to consider to verify or decrypt the JWT
	 * 
	 * @return a new JOSE JWT reader
	 * 
	 * @throws JWTReadException if the specified compact representation is invalid or not supported
	 * @throws JOSEProcessingException if the specified JWT Claims set type is invalid
	 */
	<T extends JWTClaimsSet> JOSEObjectReader<T, ?, ? extends JOSEObject<T, ?>, ?> readerFor(String compact, Type type, Publisher<? extends JWK> keys) throws JWTReadException, JOSEProcessingException;
}

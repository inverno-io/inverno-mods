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
package io.inverno.mod.security.jose.jws;

import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.JsonJOSEObject;
import io.inverno.mod.security.jose.jwk.JWK;
import java.util.List;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JSON Web Signature object supporting JWS JSON representation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-7.2">RFC7515 Section 7.2</a>.
 * </p>
 * 
 * <p>
 * Unlike {@link JWS}, a JSON JWS object can be used to represent multiple JSON Web Signature objects providing different signature for a given payload. This can be used to create a JWS with multiple
 * signature that can be validated by different recipients.
 * </p>
 * 
 * <p>
 * As per RFC7515, the JSON representation is neither optimized for compactness nor URL safe.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the signature type
 */
public interface JsonJWS<A, B extends JsonJWS.Signature<A>> extends JsonJOSEObject<A> {
	
	/**
	 * <p>
	 * Returns the payload.
	 * </p>
	 * 
	 * @return the payload
	 */
	A getPayload();

	/**
	 * <p>
	 * Returns the list of signatures.
	 * </p>
	 * 
	 * <p>
	 * When considering a built JSON JWS, signatures must be of type {@link BuiltSignature} which directly exposes the built JWS objects corresponding to each signature.
	 * </p>
	 * 
	 * <p>
	 * When considering a read JSON JWS, signatures must be of type {@link ReadSignature} which exposes JWS objects per recipient as single publisher used to validate the corresponding JWS.
	 * </p>
	 * 
	 * @return a list of signatures
	 */
	List<B> getSignatures(); 
	
	/**
	 * <p>
	 * Base JSON JWS Signature exposing signature protected and unprotected JWS JOSE headers and the signature.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	interface Signature<A> {

		/**
		 * <p>
		 * Returns the protected header which is integrity protected (i.e. processed within the signature).
		 * </p>
		 * 
		 * @return the protected header
		 */
		JWSHeader getProtectedHeader();

		/**
		 * <p>
		 * Returns the unprotected header.
		 * </p>
		 * 
		 * @return the unprotected header
		 */
		JWSHeader getUnprotectedHeader();

		/**
		 * <p>
		 * Returns the signature encoded as Base64URL.
		 * </p>
		 * 
		 * @return the Base64URL encoded signature with no padding 
		 */
		String getSignature();
		
		@Override
		int hashCode();
		
		@Override
		boolean equals(Object obj);
	}
	
	/**
	 * <p>
	 * Built signature resulting from the build of a JSON JWS and exposing the signature JWS directly.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface BuiltSignature<A> extends Signature<A> {
		
		/**
		 * <p>
		 * Returns the signature JWS.
		 * </p>
		 * 
		 * @return the signature JWS
		 */
		JWS<A> getJWS();
	}
	
	/**
	 * <p>
	 * Read signature resulting from the read of a JSON JWS and exposing a single JWS publisher used to validate the signature JWS.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface ReadSignature<A> extends Signature<A> {
		
		/**
		 * <p>
		 * Returns the signature JWS publisher.
		 * </p>
		 * 
		 * <p>
		 * The resulting publisher will try to resolve the JWK to use for validating the signature based on the JWS JOSE header resulting from the merge of the protected and unprotected headers. It
		 * will fail if it wasn't able to find a suitable key.
		 * </p>
		 * 
		 * @return a JWS publisher for validating the recipient JWS
		 * 
		 * @throws JWSReadException         if there was an error reading the recipient JWS
		 * @throws JWSBuildException        if there was an error building the recipient JWS
		 * @throws JOSEObjectReadException  if there was a JOSE object reading error
		 * @throws JOSEObjectBuildException if there was a JOSE object building error
		 * @throws JOSEProcessingException  if there was a JOSE processing error
		 */
		Mono<JWS<A>> readJWS() throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;
		
		/**
		 * <p>
		 * Returns the signature JWS publisher with the specified keys.
		 * </p>
		 *
		 * <p>
		 * The resulting publisher will use the specified keys for validating the signature, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable
		 * key have been specified or if they are not consistent with the JWS JOSE header resulting from the merge of the protected and unprotected headers.
		 * </p>
		 *
		 * @param keys the keys to consider to decrypt the recipient content encryption key
		 * 
		 * @return a JWS publisher for validating the recipient JWS
		 * 
		 * @throws JWSReadException         if there was an error reading the recipient JWS
		 * @throws JWSBuildException        if there was an error building the recipient JWS
		 * @throws JOSEObjectReadException  if there was a JOSE object reading error
		 * @throws JOSEObjectBuildException if there was a JOSE object building error
		 * @throws JOSEProcessingException  if there was a JOSE processing error
		 */
		Mono<JWS<A>> readJWS(Publisher<? extends JWK> keys) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;
	}
}

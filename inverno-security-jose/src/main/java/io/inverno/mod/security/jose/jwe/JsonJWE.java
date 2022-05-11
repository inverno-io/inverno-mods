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
package io.inverno.mod.security.jose.jwe;

import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.JsonJOSEObject;
import io.inverno.mod.security.jose.jwe.JsonJWE.Recipient;
import io.inverno.mod.security.jose.jwk.JWK;
import java.util.List;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JSON Web Encryption object supporting JWE JSON representation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.2">RFC7516 Section 7.2</a>.
 * </p>
 * 
 * <p>
 * Unlike {@link JWE}, a JSON JWE object can be used to represent multiple JSON Web Encryption objects sharing the same encrypted data but providing different encrypted keys. This can be used to
 * create a JWE that can be used by multiple recipients.
 * </p>
 * 
 * <p>
 * As per RFC7516, the JSON representation is neither optimized for compactness nor URL safe.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the recipient type
 */
public interface JsonJWE<A, B extends Recipient<A>> extends JsonJOSEObject<A> {
	
	/**
	 * <p>
	 * Returns the protected header common to all recipients.
	 * </p>
	 * 
	 * @return the protected header
	 */
	JWEHeader getProtectedHeader();

	/**
	 * <p>
	 * Returns the unprotected header common to all recipients.
	 * </p>
	 * 
	 * @return the unprotected header
	 */
	JWEHeader getUnprotectedHeader();
	
	/**
	 * <p>
	 * Returns the initialization vector common to all recipients encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded initialization vector with no padding
	 */
	String getInitializationVector();

	/**
	 * <p>
	 * Returns the additional authentication data common to all recipients encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded additional authentication data with no padding
	 */
	String getAdditionalAuthenticationData();
	
	/**
	 * <p>
	 * Returns the cipher text common to all recipients encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded cipher text with no padding
	 */
	String getCipherText();
	
	/**
	 * <p>
	 * Returns the authentication tag common to all recipients encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded authentication tag with no padding
	 */
	String getAuthenticationTag();
	
	/**
	 * <p>
	 * Returns the list of recipients.
	 * </p>
	 * 
	 * <p>
	 * When considering a built JSON JWE, recipient must be of type {@link BuiltRecipient} which directly exposes the built JWE objects corresponding to recipient.
	 * </p>
	 * 
	 * <p>
	 * When considering a read JSON JWE, recipient must be of type {@link ReadRecipient} which exposes JWE objects per recipient as single publisher used to decrypt and validate the corresponding JWE.
	 * </p>
	 * 
	 * @return a list of recipients
	 */
	List<B> getRecipients();
	
	/**
	 * <p>
	 * Base JSON JWE Recipient exposing recipient specific JWE JOSE header and encrypted key.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	interface Recipient<A> {
		
		/**
		 * <p>
		 * Returns the recipient specific JWE JOSE header.
		 * </p>
		 * 
		 * @return a JOSE JWE header
		 */
		JWEHeader getHeader();
		
		/**
		 * <p>
		 * Returns the recipient specific encrypted key.
		 * </p>
		 * 
		 * @return an encrypted key
		 */
		String getEncryptedKey();
		
		@Override
		int hashCode();
		
		@Override
		boolean equals(Object obj);
	}
	
	/**
	 * <p>
	 * Built recipient resulting from the build of a JSON JWE and exposing the recipient JWE directly.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	interface BuiltRecipient<A> extends Recipient<A> {
		
		/**
		 * <p>
		 * Returns the recipient JWE.
		 * </p>
		 * 
		 * @return a JWE object
		 */
		JWE<A> getJWE();
		
		@Override
		int hashCode();
		
		@Override
		boolean equals(Object obj);
	}
	
	/**
	 * <p>
	 * Read recipient resulting from the read of a JSON JWE and exposing a single JWE publisher used to decrypt and validate the recipient JWE.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the payload type
	 */
	interface ReadRecipient<A> extends Recipient<A> {
		
		/**
		 * <p>
		 * Returns the recipient JWE publisher.
		 * </p>
		 *
		 * <p>
		 * The resulting publisher will try to resolve the JWK to use for decrypting the content encryption key based on the JWE JOSE header resulting from the merge of the protected, unprotected and
		 * recipient specific headers. It will fail if it wasn't able to find a suitable key.
		 * </p>
		 *
		 * @return a JWE publisher for decrypting and validating the recipient JWE
		 *
		 * @throws JWEReadException         if there was an error reading the recipient JWE
		 * @throws JWEBuildException        if there was an error building the recipient JWE
		 * @throws JOSEObjectReadException  if there was a JOSE object reading error
		 * @throws JOSEObjectBuildException if there was a JOSE object building error
		 * @throws JOSEProcessingException  if there was a JOSE processing error
		 */
		Mono<JWE<A>> readJWE() throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;
		
		/**
		 * <p>
		 * Returns the recipient JWE publisher with the specified keys.
		 * </p>
		 *
		 * <p>
		 * The resulting publisher will use the specified keys for decrypting the content encryption key, the first succeeding key will be retained and remaining keys will be ignored. It will fail if
		 * no suitable key have been specified or if they are not consistent with the JWE JOSE header resulting from the merge of the protected, unprotected and recipient specific headers.
		 * </p>
		 *
		 * @param keys the keys to consider to decrypt the recipient content encryption key
		 *
		 * @return a JWE publisher for decrypting and validating the recipient JWE
		 *
		 * @throws JWEReadException         if there was an error reading the recipient JWE
		 * @throws JWEBuildException        if there was an error building the recipient JWE
		 * @throws JOSEObjectReadException  if there was a JOSE object reading error
		 * @throws JOSEObjectBuildException if there was a JOSE object building error
		 * @throws JOSEProcessingException  if there was a JOSE processing error
		 */
		Mono<JWE<A>> readJWE(Publisher<? extends JWK> keys) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;
	}
}

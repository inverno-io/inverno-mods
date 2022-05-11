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

import io.inverno.mod.security.jose.JsonJOSEObjectBuilder;
import io.inverno.mod.security.jose.jwk.JWK;
import java.security.SecureRandom;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;

/**
 * <p>
 * A JSON JWE builder is used to build JSON Web Encryption objects that can be serialized to the JSON representation as defined by
 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.2">RFC7516 Section 7.2</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the JWE JOSE header configurator type 
 * @param <C> the JSON JWE builder type
 */
public interface JsonJWEBuilder<A, B extends JWEHeaderConfigurator<B>, C extends JsonJWEBuilder<A, B, C>> extends JsonJOSEObjectBuilder<A, JsonJWE<A, JsonJWE.BuiltRecipient<A>>, B, C> {

	/**
	 * <p>
	 * Specifies the additional authentication data to use when encrypting the payload.
	 * </p>
	 * 
	 * @param aad additional authentication data
	 * 
	 * @return this builder
	 */
	C additionalAuthenticationData(String aad);
	
	/**
	 * <p>
	 * Specifies the secure random to use when encrypting the payload.
	 * </p>
	 * 
	 * @param secureRandom a secure random
	 *
	 * @return this builder
	 */
	C secureRandom(SecureRandom secureRandom);
	
	/**
	 * <p>
	 * Specifies a recipient to add to the resulting JSON JWE object.
	 * </p>
	 * 
	 * <p>
	 * The builder will try to resolve the JWK to use to encrypt the content encryption key based on the JWE JOSE header resulting from the merge of the protected, unprotected and recipient specific
	 * headers. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param headerConfigurer the recipient specific JWE JOSE header configurer
	 * 
	 * @return this builder
	 */
	default C recipient(Consumer<B> headerConfigurer) {
		return this.recipient(headerConfigurer, null);
	}
	
	/**
	 * <p>
	 * Specifies a recipient to add to the resulting JSON JWE object using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The builder will use the specified keys to encrypt the content encryption key, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JWE JOSE header resulting from the merge of the protected, unprotected and recipient specific headers.
	 * </p>
	 *
	 * @param headerConfigurer the recipient specific JWE JOSE header
	 * @param keys             the keys to consider to encrypt the recipient content encryption key
	 *
	 * @return this builder
	 */
	C recipient(Consumer<B> headerConfigurer, Publisher<? extends JWK> keys);
}

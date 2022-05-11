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

import io.inverno.mod.security.jose.JsonJOSEObjectBuilder;
import io.inverno.mod.security.jose.jwk.JWK;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;

/**
 * <p>
 * A JSON JWS builder is used to build JSON Web Signature objects that can be serialized to the JSON representation as defined by
 * <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-7.2">RFC7515 Section 7.2</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the JWS JOSE header configurator type 
 * @param <C> the JSON JWS builder type
 */
public interface JsonJWSBuilder<A, B extends JWSHeaderConfigurator<B>, C extends JsonJWSBuilder<A, B, C>> extends JsonJOSEObjectBuilder<A, JsonJWS<A, JsonJWS.BuiltSignature<A>>, B, C> {

	/**
	 * <p>
	 * Specifies a signature to add to the resulting JSON JWS object.
	 * </p>
	 *
	 * <p>
	 * The builder will try to resolve the JWK to use to sign the payload based on the JWS JOSE header resulting from the merge of the protected and unprotected headers. It will fail if it wasn't able
	 * to find a suitable key.
	 * </p>
	 *
	 * @param protectedHeaderConfigurer   the protected JWE JOSE header configurer
	 * @param unprotectedHeaderConfigurer the unprotected JWE JOSE header configurer
	 *
	 * @return this builder
	 */
	default C signature(Consumer<B> protectedHeaderConfigurer, Consumer<B> unprotectedHeaderConfigurer) {
		return this.signature(protectedHeaderConfigurer, unprotectedHeaderConfigurer, null);
	}
	
	/**
	 * <p>
	 * Specifies a signature to add to the resulting JSON JWS object using the specified keys.
	 * </p>
	 *
	 * <p>
	 * The builder will use the specified keys to sign the payload, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have been specified or
	 * if they are not consistent with the JWS JOSE header resulting from the merge of the protected and unprotected headers.
	 * </p>
	 *
	 * @param protectedHeaderConfigurer   the protected JWE JOSE header configurer
	 * @param unprotectedHeaderConfigurer the unprotected JWE JOSE header configurer
	 * @param keys                        the keys to consider to sign the payload
	 *
	 * @return this builder
	 */
	C signature(Consumer<B> protectedHeaderConfigurer, Consumer<B> unprotectedHeaderConfigurer, Publisher<? extends JWK> keys);
}

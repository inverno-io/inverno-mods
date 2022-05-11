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

import io.inverno.mod.security.jose.JOSEHeaderConfigurator;

/**
 * <p>
 * A JWS JOSE header configurator is used in {@link JWSBuilder} or {@link JsonJWSBuilder} to configure JWS JOSE headers when building JWS objects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWS JOSE header configurator type
 */
public interface JWSHeaderConfigurator<A extends JWSHeaderConfigurator<A>> extends JOSEHeaderConfigurator<A> {

	/**
	 * <p>
	 * Specifies whether the payload should be encoded as Base64URL or processed unencoded.
	 * </p>
	 * 
	 * @param b64 true or null to encode the payload as Base64URL, false otherwise
	 * 
	 * @return this builder
	 */
	A base64EncodePayload(Boolean b64);
}
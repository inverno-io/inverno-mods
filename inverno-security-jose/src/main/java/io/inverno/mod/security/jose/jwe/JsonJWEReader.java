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

import io.inverno.mod.security.jose.JsonJOSEObjectReader;

/**
 * <p>
 * A JSON JWE reader is used to read JSON Web Encryption objects serialized using the JSON representation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.2">RFC7516
 * Section 7.2</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 * @param <B> the JSON JWE reader type
 */
public interface JsonJWEReader<A, B extends JsonJWEReader<A,B>> extends JsonJOSEObjectReader<A, JsonJWE<A, JsonJWE.ReadRecipient<A>>, B> {
	
}

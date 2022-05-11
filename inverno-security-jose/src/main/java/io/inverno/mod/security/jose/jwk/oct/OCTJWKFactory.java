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
package io.inverno.mod.security.jose.jwk.oct;

import io.inverno.mod.security.jose.jwk.JWKFactory;

/**
 * <p>
 * An Octet JSON Web Key factory.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the Octet JWK type
 * @param <B> the Octet JWK builder type
 * @param <C> the Octet JWK generator type
 */
public interface OCTJWKFactory<A extends OCTJWK, B extends OCTJWKBuilder<A, B>, C extends OCTJWKGenerator<A, C>> extends JWKFactory<A, B, C> {

}

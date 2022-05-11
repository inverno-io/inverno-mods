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

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * <p>
 * An X.509 JWK generator used to generate JSON Web Keys that support X.509 JOSE header parameters: x5u, x5c, x5t and x5t#S256.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the public key type
 * @param <B> the private key type
 * @param <C> the X.509 JWK type
 * @param <D> the X.509 JWK generator type
 */
public interface X509JWKGenerator<A extends PublicKey, B extends PrivateKey, C extends X509JWK<A, B>, D extends X509JWKGenerator<A, B, C, D>> extends JWKGenerator<C, D> {

}

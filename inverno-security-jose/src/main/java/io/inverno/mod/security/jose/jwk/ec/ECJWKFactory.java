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
package io.inverno.mod.security.jose.jwk.ec;

import io.inverno.mod.security.jose.jwk.X509JWKFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

/**
 * <p>
 * An Elliptic curve JSON Web Key factory.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the Elliptic curve JWK type
 * @param <B> the Elliptic curve JWK builder type
 * @param <C> the Elliptic curve JWK generator type
 */
public interface ECJWKFactory<A extends ECJWK, B extends ECJWKBuilder<A, B>, C extends ECJWKGenerator<A, C>> extends X509JWKFactory<ECPublicKey, ECPrivateKey, A, B, C> {

}

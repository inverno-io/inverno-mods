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
package io.inverno.mod.security.jose.jwk.okp;

import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;

/**
 * <p>
 * Extended Elliptic curve JSON Web Key generator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the extended Elliptic curve JWK type
 * @param <B> the extended Elliptic curve JWK generator type
 */
public interface XECJWKGenerator<A extends XECJWK, B extends XECJWKGenerator<A, B>> extends OKPJWKGenerator<XECPublicKey, XECPrivateKey, A, B> {
	
}

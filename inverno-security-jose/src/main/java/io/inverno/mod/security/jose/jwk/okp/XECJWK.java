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
 * Extended Elliptic curve JSON Web Key.
 * </p>
 * 
 * <p>
 * An Extended Elliptic curve JWK is asymmetric.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface XECJWK extends OKPJWK<XECPublicKey, XECPrivateKey> {

	@Override
	XECJWK toPublicJWK();

	@Override
	XECJWK minify();

	@Override
	XECJWK trust();
}

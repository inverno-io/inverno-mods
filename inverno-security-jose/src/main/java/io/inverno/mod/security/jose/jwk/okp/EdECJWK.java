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

import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;

/**
 * <p>
 * Edward-curve JSON Web Key.
 * </p>
 *
 * <p>
 * An Edward-curve JWK is asymmetric.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface EdECJWK extends OKPJWK<EdECPublicKey, EdECPrivateKey> {
	
	@Override
	EdECJWK toPublicJWK();

	@Override
	EdECJWK minify();

	@Override
	EdECJWK trust();
}

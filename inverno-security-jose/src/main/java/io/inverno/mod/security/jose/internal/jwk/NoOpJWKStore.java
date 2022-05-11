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
package io.inverno.mod.security.jose.internal.jwk;

import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.JWKStoreException;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A NoOp JWK store implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class NoOpJWKStore implements JWKStore {

	@Override
	public <T extends JWK> Mono<T> getByKeyId(String kid) throws JWKStoreException {
		return Mono.empty();
	}

	@Override
	public <T extends JWK> Mono<T> getBy509CertificateSHA1Thumbprint(String x5t) throws JWKStoreException {
		return Mono.empty();
	}

	@Override
	public <T extends JWK> Mono<T> getByX509CertificateSHA256Thumbprint(String x5t_S256) throws JWKStoreException {
		return Mono.empty();
	}

	@Override
	public <T extends JWK> Mono<T> getByJWKThumbprint(String jwkThumbprint) throws JWKStoreException {
		return Mono.empty();
	}

	@Override
	public Mono<Void> set(JWK jwk) throws JWKStoreException {
		throw new JWKStoreException("NoOp JWK store does not support storage operations");
	}
	
	@Override
	public Mono<Void> remove(JWK jwk) throws JWKStoreException {
		throw new JWKStoreException("NoOp JWK store does not support storage operations");
	}
}

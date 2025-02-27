/*
 * Copyright 2025 Jeremy KUHN
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
package io.inverno.mod.session.internal;

import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionIdGenerator;
import java.util.Base64;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session identifier generator that generates random {@link UUID} as session id encoded or not in Base64.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 */
public class UuidSessionIdGenerator<A, B extends Session<A>> implements SessionIdGenerator<A, B> {

	/**
	 * Default instance.
	 */
	public static final UuidSessionIdGenerator<?, ?> INSTANCE = new UuidSessionIdGenerator<>(false);

	/**
	 * Default Base64 encoding instance.
	 */
	public static final UuidSessionIdGenerator<?, ?> BASE64_ENCODED_INSTANCE = new UuidSessionIdGenerator<>(true);

	private final Mono<String> sessionIdFactory;

	/**
	 * <p>
	 * Creates a new UUID session id generator.
	 * </p>
	 *
	 * @param base64Encoded true to encode generated identifiers in Base64, false otherwise
	 */
	public UuidSessionIdGenerator(boolean base64Encoded) {
		if(base64Encoded) {
			this.sessionIdFactory = Mono.fromSupplier(() -> Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes()));
		}
		else {
			this.sessionIdFactory = Mono.fromSupplier(() -> UUID.randomUUID().toString());
		}
	}

	@Override
	public Mono<String> generate(B session) {
		return this.sessionIdFactory;
	}
}

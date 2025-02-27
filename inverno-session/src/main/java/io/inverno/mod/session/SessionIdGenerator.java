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
package io.inverno.mod.session;

import io.inverno.mod.session.internal.UuidSessionIdGenerator;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session identifier generator is used to generate unique session id.
 * </p>
 *
 * <p>
 * Implementors must ensure that identifiers are generated in a way that ensures that there is negligible probability that the same value will be accidentally assigned to different sessions.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 */
public interface SessionIdGenerator<A, B extends Session<A>> {

	/**
	 * <p>
	 * Returns a Base64 UUID session id generator.
	 * </p>
	 *
	 * @return a session id generator
	 *
	 * @param <A> the session data type
	 * @param <B> the session type
	 */
	static <A, B extends Session<A>> SessionIdGenerator<A, B> uuid() {
		return uuid(true);
	}

	/**
	 * <p>
	 * Returns a UUID session id generator.
	 * </p>
	 *
	 * @param base64Encoded true to encode the identifier in Base64, false otherwise
	 *
	 * @return a session id generator
	 *
	 * @param <A> the session data type
	 * @param <B> the session type
	 */
	@SuppressWarnings("unchecked")
	static <A, B extends Session<A>> SessionIdGenerator<A, B> uuid(boolean base64Encoded) {
		if(base64Encoded) {
			return (SessionIdGenerator<A, B>)UuidSessionIdGenerator.BASE64_ENCODED_INSTANCE;
		}
		else {
			return (SessionIdGenerator<A, B>)UuidSessionIdGenerator.INSTANCE;
		}
	}

	/**
	 * <p>
	 * Generates a new identifier for the specified session.
	 * </p>
	 *
	 * @param session a session
	 *
	 * @return a mono generating a session id
	 */
	Mono<String> generate(B session);
}

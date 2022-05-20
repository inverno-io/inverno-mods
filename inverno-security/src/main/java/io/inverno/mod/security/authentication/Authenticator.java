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
package io.inverno.mod.security.authentication;

import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@FunctionalInterface
public interface Authenticator<A extends Credentials, B extends Authentication> {

	Mono<B> authenticate(A credentials) throws AuthenticationException;

	default Authenticator<A, B> or(Authenticator<? super A, ? extends B> other) {
		return credentials -> {
			return this.authenticate(credentials).switchIfEmpty(other.authenticate(credentials));
		};
	}
	
	default <T extends Authentication> Authenticator<A, T> flatMap(Function<? super B, ? extends Mono<? extends T>> mapper) {
		return credentials -> {
			return this.authenticate(credentials).flatMap(mapper);
		};
	}
	
	default <T extends Authentication> Authenticator<A, T> map(Function<? super B, ? extends T> mapper) {
		return credentials -> {
			return this.authenticate(credentials).map(mapper);
		};
	}
}

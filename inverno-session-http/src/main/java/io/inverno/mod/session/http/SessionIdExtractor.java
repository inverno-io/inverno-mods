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
package io.inverno.mod.session.http;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session identifier extractor extracts the session identifier from an exchange, typically the request.
 * </p>
 *
 * <p>
 * It is used by the {@link SessionInterceptor} to extract the session identifier in order to resolve the session in the session context.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 */
@FunctionalInterface
public interface SessionIdExtractor<A extends ExchangeContext, B extends Exchange<A>> {

	/**
	 * <p>
	 * Extracts the session identifier from the specified exchange.
	 * </p>
	 *
	 * @param exchange the exchange
	 *
	 * @return a mono emitting the session identifier or an empty mono if the exchange didn't provide any session identifier
	 */
	Mono<String> extract(B exchange);

	/**
	 * <p>
	 * Returns a composed session identifier extractor which first invokes this extractor and, if no identifier could have been extracted, invokes the specified extractor.
	 * </p>
	 *
	 * @param other the session identifier extractor to invoke in case this extractor was not able to extract the identifier
	 *
	 * @return a composed session identifier extractor
	 */
	default SessionIdExtractor<A, B> or(SessionIdExtractor<? super A, ? super B> other) {
		return exchange -> this.extract(exchange).switchIfEmpty(other.extract(exchange));
	}
}

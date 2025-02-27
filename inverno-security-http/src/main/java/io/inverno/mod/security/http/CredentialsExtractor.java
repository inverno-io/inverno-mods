/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.security.http;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.authentication.Credentials;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials extractor is used to extract credentials from an exchange, typically the request.
 * </p>
 * 
 * <p>
 * The {@link SecurityInterceptor} uses it to extract credentials in order to authenticate the exchange and create the security context.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the credentials type
 */
@FunctionalInterface
public interface CredentialsExtractor<A extends Credentials, B extends ExchangeContext, C extends Exchange<B>> {

	/**
	 * <p>
	 * Extracts credentials from the specified exchange.
	 * </p>
	 * 
	 * @param exchange the exchange
	 * 
	 * @return a mono emitting the credentials or an empty mono if the exchange didn't provide any credentials
	 * 
	 * @throws MalformedCredentialsException if credentials in the exchange are malformed
	 */
	Mono<A> extract(C exchange) throws MalformedCredentialsException;
	
	/**
	 * <p>
	 * Returns a composed credentials extractor which first invokes this extractor and, if no credentials could have been extracted, invokes the specified extractor.
	 * </p>
	 *
	 * @param other the credentials extractor to invoke in case this extractor was not able to extract credentials
	 *
	 * @return a composed credentials extractor
	 */
	default CredentialsExtractor<A, B, C> or(CredentialsExtractor<? extends A, ? super B, ? super C> other) {
		return exchange -> this.extract(exchange).switchIfEmpty(other.extract(exchange));
	}
}

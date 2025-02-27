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
package io.inverno.mod.security.http.token;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.authentication.TokenCredentials;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials extractor that extracts a token credentials from a bearer in the {@code authorization} HTTP header as defined by <a href="https://www.rfc-editor.org/rfc/rfc6750.html#section-2.1">RFC
 * 6750 Section 2.1</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
// TODO this has to be moved to OAuth2 specific security module
public class BearerTokenCredentialsExtractor<A extends ExchangeContext, B extends Exchange<A>> implements CredentialsExtractor<TokenCredentials, A, B> {

	@Override
	public Mono<TokenCredentials> extract(B exchange) throws MalformedCredentialsException {
		return Mono.fromSupplier(() -> exchange.request().headers()
			.<Headers.Authorization>getHeader(Headers.NAME_AUTHORIZATION)
			.filter(authorizationHeader -> authorizationHeader.getAuthScheme().equals(Headers.Authorization.AUTH_SCHEME_BEARER))
			.map(authorizationHeader -> new TokenCredentials(authorizationHeader.getToken()))
			.orElse(null)
		);
	}
}

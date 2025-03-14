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
package io.inverno.mod.security.http.basic;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import java.util.Base64;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials extractor that extracts {@code basic} login credentials as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7617">RFC 7617 Section 2</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class BasicCredentialsExtractor<A extends ExchangeContext, B extends Exchange<A>> implements CredentialsExtractor<LoginCredentials, A, B> {
	
	@Override
	public Mono<LoginCredentials> extract(B exchange) throws MalformedCredentialsException {
		return Mono.fromSupplier(() -> exchange.request().headers()
			.<Headers.Authorization>getHeader(Headers.NAME_AUTHORIZATION)
			.filter(authorizationHeader -> authorizationHeader.getAuthScheme().equals(Headers.Authorization.AUTH_SCHEME_BASIC))
			.map(authorizationHeader -> {
				String[] splitCredentials = new String(Base64.getDecoder().decode(authorizationHeader.getToken())).split(":");
				switch(splitCredentials.length) {
					case 1: {
						if(splitCredentials[0].isBlank()) {
							// ":" <=> no basic credentials
							return null;
						}
						return LoginCredentials.of(splitCredentials[0], new RawPassword(""));
					}
					case 2: {
						return LoginCredentials.of(splitCredentials[0], new RawPassword(splitCredentials[1]));
					}
					default : {
						// this is invalid
						throw new MalformedCredentialsException("Invalid basic credentials");
					}
				}
				
			})
			.orElse(null)
		);
	}
}

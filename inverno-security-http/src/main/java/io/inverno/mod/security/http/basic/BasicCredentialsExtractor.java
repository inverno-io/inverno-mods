package io.inverno.mod.security.http.basic;

import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.UserCredentials;
import io.inverno.mod.security.http.CredentialsExtractor;
import reactor.core.publisher.Mono;

import java.util.Base64;

public class BasicCredentialsExtractor implements CredentialsExtractor<UserCredentials> {
	
	@Override
	public Mono<UserCredentials> extract(Exchange<?> exchange) {
		return Mono.fromSupplier(() -> exchange.request().headers()
			.<Headers.Authorization>getHeader(Headers.NAME_AUTHORIZATION)
			.filter(authorizationHeader -> authorizationHeader.getAuthScheme().equals(Headers.Authorization.AUTH_SCHEME_BASIC))
			.map(authorizationHeader -> {
				String[] splitCredentials = new String(Base64.getDecoder().decode(authorizationHeader.getToken())).split(":");
				return new UserCredentials(splitCredentials[0], splitCredentials[1]);
			})
			.orElse(null)
		);
	}
}

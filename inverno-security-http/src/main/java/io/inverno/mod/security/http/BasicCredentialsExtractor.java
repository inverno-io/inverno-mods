package io.inverno.mod.security.http;

import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.UsernamePasswordCredentials;
import reactor.core.publisher.Mono;

import java.util.Base64;

public class BasicCredentialsExtractor implements CredentialsExtractor<UsernamePasswordCredentials> {

	private static final int INDEX_AUTHORIZATION_CREDENTIALS = 6;

	@Override
	public Mono<UsernamePasswordCredentials> extract(Exchange<?> exchange) {
		return Mono.fromSupplier(() -> exchange.request().headers()
			.get(Headers.NAME_AUTHORIZATION)
			.map(authorization -> {
				String[] splitCredentials = new String(Base64.getDecoder().decode(authorization.substring(INDEX_AUTHORIZATION_CREDENTIALS))).split(":");
				return new UsernamePasswordCredentials(splitCredentials[0], splitCredentials[1]);
			})
			.orElse(null)
		);
	}
}

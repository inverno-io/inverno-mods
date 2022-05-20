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
package io.inverno.mod.security.jose.jws;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.TokenCredentials;
import io.inverno.mod.security.jose.jwk.JWK;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class JWSAuthenticator<A extends Authentication> implements Authenticator<TokenCredentials, JWSAuthentication<A>> {

	private final JWSService jwsService;
	private final Type authenticationType;
	private final Publisher<? extends JWK> keys;
	private final String[] processedParameters;
	
	public JWSAuthenticator(JWSService jwsService, Class<A> authenticationType) {
		this(jwsService, (Type)authenticationType, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	public JWSAuthenticator(JWSService jwsService, Type authenticationType) {
		this(jwsService, authenticationType, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	public JWSAuthenticator(JWSService jwsService, Class<A> authenticationType, Publisher<? extends JWK> keys) {
		this(jwsService, (Type)authenticationType, keys, (String[]) null);
	}
	
	public JWSAuthenticator(JWSService jwsService, Type authenticationType, Publisher<? extends JWK> keys) {
		this(jwsService, (Type)authenticationType, keys, (String[]) null);
	}
	
	public JWSAuthenticator(JWSService jwsService, Class<A> authenticationType, String... processedParameters) {
		this(jwsService, (Type)authenticationType, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	public JWSAuthenticator(JWSService jwsService, Type authenticationType, String... processedParameters) {
		this(jwsService, authenticationType, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	public JWSAuthenticator(JWSService jwsService, Class<A> authenticationType, Publisher<? extends JWK> keys, String... processedParameters) {
		this(jwsService, (Type)authenticationType, keys, processedParameters);
	}
	
	public JWSAuthenticator(JWSService jwsService, Type authenticationType, Publisher<? extends JWK> keys, String... processedParameters) {
		this.jwsService = jwsService;
		this.keys = keys;
		this.authenticationType = authenticationType;
		this.processedParameters = processedParameters;
	}
	
	@Override
	public Mono<JWSAuthentication<A>> authenticate(TokenCredentials credentials) throws AuthenticationException {
		return this.jwsService.<A>reader(this.authenticationType, this.keys)
			.processedParameters(this.processedParameters)
			.read(credentials.getToken(), MediaTypes.APPLICATION_JSON)
			.map(JWSAuthentication::new)
			.onErrorMap(e -> new InvalidCredentialsException("Invalid token", e));
	}
}

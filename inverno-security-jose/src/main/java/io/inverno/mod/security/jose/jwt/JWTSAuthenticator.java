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
package io.inverno.mod.security.jose.jwt;

import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.TokenCredentials;
import io.inverno.mod.security.jose.jwk.JWK;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class JWTSAuthenticator<A extends JWTClaimsSet> implements Authenticator<TokenCredentials, JWTSAuthentication<A>> {

	private final JWTService jwtService;
	private final Type type;
	private final Publisher<? extends JWK> keys;
	private final String[] processedParameters;
	
	public JWTSAuthenticator(JWTService jwtService, Class<A> type) {
		this(jwtService, (Type)type, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	public JWTSAuthenticator(JWTService jwtService, Type type) {
		this(jwtService, type, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	public JWTSAuthenticator(JWTService jwtService, Class<A> type, Publisher<? extends JWK> keys) {
		this(jwtService, (Type)type, keys, (String[]) null);
	}
	
	public JWTSAuthenticator(JWTService jwtService, Type type, Publisher<? extends JWK> keys) {
		this(jwtService, (Type)type, keys, (String[]) null);
	}
	
	public JWTSAuthenticator(JWTService jwtService, Class<A> type, String... processedParameters) {
		this(jwtService, (Type)type, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	public JWTSAuthenticator(JWTService jwtService, Type type, String... processedParameters) {
		this(jwtService, type, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	public JWTSAuthenticator(JWTService jwtService, Class<A> type, Publisher<? extends JWK> keys, String... processedParameters) {
		this(jwtService, (Type)type, keys, processedParameters);
	}
	
	public JWTSAuthenticator(JWTService jwtService, Type type, Publisher<? extends JWK> keys, String... processedParameters) {
		this.jwtService = jwtService;
		this.keys = keys;
		this.type = type;
		this.processedParameters = processedParameters;
	}

	@Override
	public Mono<JWTSAuthentication<A>> authenticate(TokenCredentials credentials) throws AuthenticationException {
		return this.jwtService.<A>jwsReader(this.type, this.keys)
			.processedParameters(this.processedParameters)
			.read(credentials.getToken())
			.map(JWTSAuthentication::new);
	}
}

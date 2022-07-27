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
package io.inverno.mod.security.jose.jwe;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.TokenCredentials;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.jwk.JWK;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An authenticator implementation that authenticates JWE token credentials and expose the original authentication.
 * </p>
 * 
 * <p>
 * The expected token must be a valid JWE compact string.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the original authentication type
 */
public class JWEAuthenticator<A extends Authentication>  implements Authenticator<TokenCredentials, JWEAuthentication<A>> {

	/**
	 * The JWE service.
	 */
	private final JWEService jweService;
	
	/**
	 * The original authentication type.
	 */
	private final Type authenticationType;
	
	/**
	 * The keys to consider to verify the JWS.
	 */
	private final Publisher<? extends JWK> keys;
	
	/**
	 * The parameters processed by the application.
	 */
	private final String[] processedParameters;
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service and original authentication type.
	 * </p>
	 *
	 * @param jweService         the JWE service
	 * @param authenticationType the original authentication type
	 */
	public JWEAuthenticator(JWEService jweService, Class<A> authenticationType) {
		this(jweService, (Type)authenticationType, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service and original authentication type.
	 * </p>
	 *
	 * @param jweService         the JWE service
	 * @param authenticationType the original authentication type
	 */
	public JWEAuthenticator(JWEService jweService, Type authenticationType) {
		this(jweService, authenticationType, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service, original authentication type and keys.
	 * </p>
	 *
	 * @param jweService         the JWE service
	 * @param authenticationType the original authentication type
	 * @param keys               the keys to consider to decode the JWE
	 */
	public JWEAuthenticator(JWEService jweService, Class<A> authenticationType, Publisher<? extends JWK> keys) {
		this(jweService, (Type)authenticationType, keys, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service, original authentication type and keys.
	 * </p>
	 *
	 * @param jweService         the JWE service
	 * @param authenticationType the original authentication type
	 * @param keys               the keys to consider to decode the JWE
	 */
	public JWEAuthenticator(JWEService jweService, Type authenticationType, Publisher<? extends JWK> keys) {
		this(jweService, (Type)authenticationType, keys, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service, original authentication type and processed parameters.
	 * </p>
	 *
	 * @param jweService          the JWE service
	 * @param authenticationType  the original authentication type
	 * @param processedParameters the parameters processed by the application
	 */
	public JWEAuthenticator(JWEService jweService, Class<A> authenticationType, String... processedParameters) {
		this(jweService, (Type)authenticationType, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service, original authentication type and processed parameters.
	 * </p>
	 *
	 * @param jweService          the JWE service
	 * @param authenticationType  the original authentication type
	 * @param processedParameters the parameters processed by the application
	 */
	public JWEAuthenticator(JWEService jweService, Type authenticationType, String... processedParameters) {
		this(jweService, authenticationType, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service, original authentication type, keys and processed parameters.
	 * </p>
	 *
	 * @param jweService          the JWE service
	 * @param authenticationType  the original authentication type
	 * @param keys                the keys to consider to decode the JWE
	 * @param processedParameters the parameters processed by the application
	 */
	public JWEAuthenticator(JWEService jweService, Class<A> authenticationType, Publisher<? extends JWK> keys, String... processedParameters) {
		this(jweService, (Type)authenticationType, keys, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWE authenticator with the specified JWE service, original authentication type, keys and processed parameters.
	 * </p>
	 *
	 * @param jweService          the JWE service
	 * @param authenticationType  the original authentication type
	 * @param keys                the keys to consider to cecode the JWE
	 * @param processedParameters the parameters processed by the application
	 */
	public JWEAuthenticator(JWEService jweService, Type authenticationType, Publisher<? extends JWK> keys, String... processedParameters) {
		this.jweService = jweService;
		this.keys = keys;
		this.authenticationType = authenticationType;
		this.processedParameters = processedParameters;
	}
	
	@Override
	public Mono<JWEAuthentication<A>> authenticate(TokenCredentials credentials) {
		return this.jweService.<A>reader(this.authenticationType, this.keys)
			.processedParameters(this.processedParameters)
			.read(credentials.getToken(), MediaTypes.APPLICATION_JSON)
			.map(JWEAuthentication::new)
			.onErrorResume(JOSEProcessingException.class, e -> Mono.just(new JWEAuthentication<>(e)));
	}
}

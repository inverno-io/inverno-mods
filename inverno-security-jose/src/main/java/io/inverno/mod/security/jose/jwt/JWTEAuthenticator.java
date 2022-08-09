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

import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.TokenCredentials;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.jwk.JWK;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An authenticator implementation that authenticates JWTE token credentials.
 * </p>
 * 
 * <p>
 * The expected token must be a valid JWTE compact string.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWT claims set type
 */
public class JWTEAuthenticator<A extends JWTClaimsSet> implements Authenticator<TokenCredentials, JWTEAuthentication<A>> {

	/**
	 * the JWT service.
	 */
	private final JWTService jwtService;
	
	/**
	 * The JWT claims set type.
	 */
	private final Type type;
	
	/**
	 * The keys to consider to decode the JWTE.
	 */
	private final Publisher<? extends JWK> keys;
	
	/**
	 * The parameters processed by the application.
	 */
	private final String[] processedParameters;
	
	/**
	 * The list of validators to use to validate the JWT.
	 */
	private final List<JWTClaimsSetValidator> validators;
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service.
	 * </p>
	 *
	 * @param jwtService the JWT service
	 */
	public JWTEAuthenticator(JWTService jwtService) {
		this(jwtService, (Type)JWTClaimsSet.class, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service and keys.
	 * </p>
	 *
	 * @param jwtService the JWT service
	 * @param keys       the keys to consider to verify the JWTS
	 */
	public JWTEAuthenticator(JWTService jwtService, Publisher<? extends JWK> keys) {
		this(jwtService, (Type)JWTClaimsSet.class, keys, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service and processed parameters.
	 * </p>
	 *
	 * @param jwtService          the JWT service
	 * @param processedParameters the parameters processed by the application
	 */
	public JWTEAuthenticator(JWTService jwtService, String... processedParameters) {
		this(jwtService, (Type)JWTClaimsSet.class, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service, keys and processed parameters.
	 * </p>
	 *
	 * @param jwtService          the JWT service
	 * @param keys                the keys to consider to verify the JWTS
	 * @param processedParameters the parameters processed by the application
	 */
	public JWTEAuthenticator(JWTService jwtService, Publisher<? extends JWK> keys, String... processedParameters) {
		this(jwtService, (Type)JWTClaimsSet.class, keys, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service and JWT claims set type.
	 * </p>
	 *
	 * @param jwtService the JWT service
	 * @param type       the JWT claims set type
	 */
	public JWTEAuthenticator(JWTService jwtService, Class<A> type) {
		this(jwtService, (Type)type, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service and JWT claims set type.
	 * </p>
	 *
	 * @param jwtService the JWT service
	 * @param type       the JWT claims set type
	 */
	public JWTEAuthenticator(JWTService jwtService, Type type) {
		this(jwtService, type, (Publisher<? extends JWK>)null, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service, JWT claims set type and keys.
	 * </p>
	 *
	 * @param jwtService the JWT service
	 * @param type       the JWT claims set type
	 * @param keys       the keys to consider to decode the JWTE
	 */
	public JWTEAuthenticator(JWTService jwtService, Class<A> type, Publisher<? extends JWK> keys) {
		this(jwtService, (Type)type, keys, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service, JWT claims set type and keys.
	 * </p>
	 *
	 * @param jwtService the JWT service
	 * @param type       the JWT claims set type
	 * @param keys       the keys to consider to decode the JWTE
	 */
	public JWTEAuthenticator(JWTService jwtService, Type type, Publisher<? extends JWK> keys) {
		this(jwtService, (Type)type, keys, (String[]) null);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service, JWT claims set type and processed parameters.
	 * </p>
	 *
	 * @param jwtService          the JWT service
	 * @param type                the JWT claims set type
	 * @param processedParameters the parameters processed by the application
	 */
	public JWTEAuthenticator(JWTService jwtService, Class<A> type, String... processedParameters) {
		this(jwtService, (Type)type, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service, JWT claims set type and processed parameters.
	 * </p>
	 *
	 * @param jwtService          the JWT service
	 * @param type                the JWT claims set type
	 * @param processedParameters the parameters processed by the application
	 */
	public JWTEAuthenticator(JWTService jwtService, Type type, String... processedParameters) {
		this(jwtService, type, (Publisher<? extends JWK>)null, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service, JWT claims set type, keys and processed parameters.
	 * </p>
	 *
	 * @param jwtService          the JWT service
	 * @param type                the JWT claims set type
	 * @param keys                the keys to consider to decode the JWTE
	 * @param processedParameters the parameters processed by the application
	 */
	public JWTEAuthenticator(JWTService jwtService, Class<A> type, Publisher<? extends JWK> keys, String... processedParameters) {
		this(jwtService, (Type)type, keys, processedParameters);
	}
	
	/**
	 * <p>
	 * Creates a JWTE authenticator with the specified JWT service, JWT claims set type, keys and processed parameters.
	 * </p>
	 *
	 * @param jwtService          the JWT service
	 * @param type                the JWT claims set type
	 * @param keys                the keys to consider to decode the JWTE
	 * @param processedParameters the parameters processed by the application
	 */
	public JWTEAuthenticator(JWTService jwtService, Type type, Publisher<? extends JWK> keys, String... processedParameters) {
		this.jwtService = jwtService;
		this.keys = keys;
		this.type = type;
		this.processedParameters = processedParameters;
		this.validators = new LinkedList<>();
	}
	
	/**
	 * <p>
	 * Adds the specified validator to the JWT claims set.
	 * </p>
	 * 
	 * @param validator the validator to add
	 * 
	 * @return the JWTE authenticator
	 */
	public JWTEAuthenticator validate(JWTClaimsSetValidator validator) {
		this.validators.add(validator);
		return this;
	}
	
	/**
	 * <p>
	 * Sets the JWT claims set validators.
	 * </p>
	 * 
	 * @param validators a list of validators or null to clear the validators
	 */
	public void setValidators(List<JWTClaimsSetValidator> validators) {
		this.validators.clear();
		if(validators != null) {
			for(JWTClaimsSetValidator validator : validators) {
				this.validators.add(validator);
			}
		}
	}
	
	/**
	 * <p>
	 * Returns the list of JWT claims set validators.
	 * </p>
	 * 
	 * @return the JWT claims set validators
	 */
	public final List<JWTClaimsSetValidator> getValidators() {
		return Collections.unmodifiableList(validators);
	}

	@Override
	public Mono<JWTEAuthentication<A>> authenticate(TokenCredentials credentials) {
		return this.jwtService.<A>jweReader(this.type, this.keys)
			.processedParameters(this.processedParameters)
			.read(credentials.getToken())
			.map(jwe -> {
				this.validators.forEach(jwe.getPayload()::validate);
				return new JWTEAuthentication<>(jwe);
			})
			.onErrorResume(JOSEProcessingException.class, e -> Mono.just(new JWTEAuthentication<>(e)));
	}
}

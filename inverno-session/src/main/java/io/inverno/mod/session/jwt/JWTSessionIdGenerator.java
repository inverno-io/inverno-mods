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
package io.inverno.mod.session.jwt;

import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.jwe.JWEHeaderConfigurator;
import io.inverno.mod.security.jose.jws.JWSHeaderConfigurator;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTService;
import io.inverno.mod.session.SessionIdGenerator;
import io.inverno.mod.session.internal.jwt.JWTESessionIdGenerator;
import io.inverno.mod.session.internal.jwt.JWTSSessionIdGenerator;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session id generator that generates JWT identifier containing session expiration settings and stateless session data.
 * </p>
 *
 * <p>
 * It is also used by JWT session stores to validate and decrypt JWT when resolving session.
 * </p>
 *
 * <p>
 * JWT expiration time and issued at claims must be expressed in seconds since epoch, as a result session creation time and session expiration time must be rounded to the lowest integer when
 * generating the JWT session id.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 */
public interface JWTSessionIdGenerator<A, B> extends SessionIdGenerator<A, JWTSession<A, B>> {

	/**
	 * The maximum inactive interval claim name.
	 */
	String CLAIM_MAX_INACTIVE_INTERVAL = "https://inverno.io/session/max_inactive_interval";
	/**
	 * The session data claim name.
	 */
	String CLAIM_SESSION_DATA = "https://inverno.io/session/data";

	@Override
	default Mono<String> generate(JWTSession<A, B> session) {
		return this.generateJWT(session).map(JOSEObject::toCompact);
	}

	/**
	 * <p>
	 * Generates a JWT session identifier for the specified JWT session.
	 * </p>
	 *
	 * <p>
	 * Implementors should ensure that JWT are generated with unique JWT token identifiers (i.e. JTI) to prevent against replay attack.
	 * </p>
	 *
	 * @param session a JWT session
	 *
	 * @return a mono generating a JWT
	 */
	Mono<? extends JOSEObject<JWTClaimsSet, ?>> generateJWT(JWTSession<A, B> session);

	/**
	 * <p>
	 * Validates and decrypts the specified JWT session identifier.
	 * </p>
	 *
	 * @param sessionId a JWT session identifier
	 *
	 * @return a mono reading a JWT session identifier
	 */
	Mono<? extends JOSEObject<JWTClaimsSet, ?>> readJWT(String sessionId);

	/**
	 * <p>
	 * Returns a new JWT session id generator that generates JWT as JWS which guarantees the integrity of the data within the session id.
	 * </p>
	 *
	 * @param <A>              the session data type
	 * @param <B>              the stateless session data type
	 * @param jwtService       the JWT service
	 * @param headerConfigurer the JWT header configurer
	 *
	 * @return a new JWT JWS session id generator
	 */
	static <A, B> JWTSessionIdGenerator<A, B> jws(JWTService jwtService, Consumer<JWSHeaderConfigurator<?>> headerConfigurer) {
		return new JWTSSessionIdGenerator<>(jwtService, headerConfigurer);
	}

	/**
	 * <p>
	 * Returns a new JWT session id generator that generates JWT as JWS with the specified issuer and which guarantees the integrity of the data within the session id.
	 * </p>
	 *
	 * <p>
	 * The issuer is validated when reading JWT session identifier.
	 * </p>
	 *
	 * @param <A>              the session data type
	 * @param <B>              the stateless session data type
	 * @param jwtService       the JWT service
	 * @param headerConfigurer the JWT header configurer
	 * @param issuer           the issuer
	 *
	 * @return a new JWT JWS session id generator
	 */
	static <A, B> JWTSessionIdGenerator<A, B> jws(JWTService jwtService, Consumer<JWSHeaderConfigurator<?>> headerConfigurer, String issuer) {
		return new JWTSSessionIdGenerator<>(jwtService, headerConfigurer, issuer);
	}

	/**
	 * <p>
	 * Returns a new JWT session id generator that generates JWT as JWE guarantees both integrity and confidentiality of the data within the session id.
	 * </p>
	 *
	 * @param <A>              the session data type
	 * @param <B>              the stateless session data type
	 * @param jwtService       the JWT service
	 * @param headerConfigurer the JWT header configurer
	 *
	 * @return a new JWT JWS session id generator
	 */
	static <A, B> JWTSessionIdGenerator<A, B> jwe(JWTService jwtService, Consumer<JWEHeaderConfigurator<?>> headerConfigurer) {
		return new JWTESessionIdGenerator<>(jwtService, headerConfigurer);
	}

	/**
	 * <p>
	 * Returns a new JWT session id generator that generates JWT as JWS with the specified issuer and which guarantees both integrity and confidentiality of the data within the session id.
	 * </p>
	 *
	 * <p>
	 * The issuer is validated when reading JWT session identifier.
	 * </p>
	 *
	 * @param <A>              the session data type
	 * @param <B>              the stateless session data type
	 * @param jwtService       the JWT service
	 * @param headerConfigurer the JWT header configurer
	 * @param issuer           the issuer
	 *
	 * @return a new JWT JWE session id generator
	 */
	static <A, B> JWTSessionIdGenerator<A, B> jwe(JWTService jwtService, Consumer<JWEHeaderConfigurator<?>> headerConfigurer, String issuer) {
		return new JWTESessionIdGenerator<>(jwtService, headerConfigurer, issuer);
	}
}

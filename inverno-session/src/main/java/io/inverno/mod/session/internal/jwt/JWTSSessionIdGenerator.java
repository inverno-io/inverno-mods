/*
 * Copyright 2025 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.session.internal.jwt;

import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSHeaderConfigurator;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWT session id generator that generates JWT as JWS which guarantees the integrity of the stateless session data within the session id.
 * </p>
 *
 * <p>
 * When specified, the issuer is validated when reading JWT session identifier.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 */
public class JWTSSessionIdGenerator<A, B> extends AbstractJWTSessionIdGenerator<A, B, JWS<JWTClaimsSet>> {

	private final Consumer<JWSHeaderConfigurator<?>> headerConfigurer;

	/**
	 * <p>
	 * Creates a JWS JWT session id generator.
	 * </p>
	 *
	 * @param jwtService       the JWT service
	 * @param headerConfigurer a JWS header configurer
	 */
	public JWTSSessionIdGenerator(JWTService jwtService, Consumer<JWSHeaderConfigurator<?>> headerConfigurer) {
		this(jwtService, headerConfigurer, null);
	}

	/**
	 * <p>
	 * Creates a JWS JWT session id generator.
	 * </p>
	 *
	 * @param jwtService       the JWT service
	 * @param headerConfigurer a JWS header configurer
	 * @param issuer           the issuer
	 */
	public JWTSSessionIdGenerator(JWTService jwtService, Consumer<JWSHeaderConfigurator<?>> headerConfigurer, String issuer) {
		super(jwtService, issuer);
		this.headerConfigurer = headerConfigurer;
	}

	@Override
	protected Mono<JWS<JWTClaimsSet>> doGenerateJWT(JWTClaimsSet.Builder<JWTClaimsSet, ?> jwtClaimsSetBuilder) {
		return this.jwtService.jwsBuilder()
			.header(this.headerConfigurer::accept)
			.payload(jwtClaimsSetBuilder.build())
			.build();
	}

	@Override
	protected Mono<JWS<JWTClaimsSet>> doReadJWT(String sessionId) {
		return this.jwtService.jwsReader().read(sessionId);
	}
}

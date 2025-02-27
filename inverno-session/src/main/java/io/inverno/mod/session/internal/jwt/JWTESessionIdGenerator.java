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

import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEHeaderConfigurator;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTClaimsSetValidator;
import io.inverno.mod.security.jose.jwt.JWTService;
import io.inverno.mod.session.jwt.JWTSession;
import io.inverno.mod.session.jwt.JWTSessionIdGenerator;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWT session id generator that generates JWT as JWE which guarantees both integrity and confidentiality of the stateless session data within the session id.
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
public class JWTESessionIdGenerator<A, B> extends AbstractJWTSessionIdGenerator<A, B, JWE<JWTClaimsSet>> {

	private final Consumer<JWEHeaderConfigurator<?>> headerConfigurer;

	/**
	 * <p>
	 * Creates a JWE JWT session id generator.
	 * </p>
	 *
	 * @param jwtService       the JWT service
	 * @param headerConfigurer the JWE header configurer
	 */
	public JWTESessionIdGenerator(JWTService jwtService, Consumer<JWEHeaderConfigurator<?>> headerConfigurer) {
		this(jwtService, headerConfigurer, null);
	}

	/**
	 * <p>
	 * Creates a JWE JWT session id generator.
	 * </p>
	 *
	 * @param jwtService       the JWT service
	 * @param headerConfigurer the JWE header configurer
	 * @param issuer           the issuer
	 */
	public JWTESessionIdGenerator(JWTService jwtService, Consumer<JWEHeaderConfigurator<?>> headerConfigurer, String issuer) {
		super(jwtService, issuer);
		this.headerConfigurer = headerConfigurer;
	}

	/**
	 * <p>
	 * Returns the JWT issuer.
	 * </p>
	 *
	 * @return the issuer
	 */
	public String getIssuer() {
		return issuer;
	}

	@Override
	protected Mono<JWE<JWTClaimsSet>> doGenerateJWT(JWTClaimsSet.Builder<JWTClaimsSet, ?> jwtClaimsSetBuilder) {
		return this.jwtService.jweBuilder()
			.header(this.headerConfigurer::accept)
			.payload(jwtClaimsSetBuilder.build())
			.build();
	}

	@Override
	protected Mono<JWE<JWTClaimsSet>> doReadJWT(String sessionId) {
		return this.jwtService.jweReader().read(sessionId);
	}
}

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

import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTClaimsSetValidator;
import io.inverno.mod.security.jose.jwt.JWTService;
import io.inverno.mod.session.jwt.JWTSession;
import io.inverno.mod.session.jwt.JWTSessionIdGenerator;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base {@link JWTSessionIdGenerator} implementation class.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 * @param <C> the JOSE object type
 */
abstract class AbstractJWTSessionIdGenerator<A, B, C extends JOSEObject<JWTClaimsSet, ?>> implements JWTSessionIdGenerator<A, B> {

	protected final JWTService jwtService;
	protected final String issuer;

	/**
	 * <p>
	 * Creates a base JWT session id generator.
	 * </p>
	 *
	 * @param jwtService the JWT service
	 * @param issuer     the issuer
	 */
	protected AbstractJWTSessionIdGenerator(JWTService jwtService, String issuer) {
		this.jwtService = jwtService;
		this.issuer = issuer;
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
	public Mono<C> generateJWT(JWTSession<A, B> session) {
		return Mono.defer(() -> {
			JWTClaimsSet.Builder<JWTClaimsSet, ?> jwtClaimsSetBuilder = JWTClaimsSet.of();
			jwtClaimsSetBuilder.jwtId(UUID.randomUUID().toString());

			if(StringUtils.isNotBlank(this.issuer)) {
				jwtClaimsSetBuilder.issuer(this.issuer);
			}

			jwtClaimsSetBuilder.issuedAt(Math.floorDiv(session.getCreationTime(), 1000));
			if(session.getMaxInactiveInterval() != null) {
				jwtClaimsSetBuilder.addCustomClaim(CLAIM_MAX_INACTIVE_INTERVAL, session.getMaxInactiveInterval());
			}
			else {
				jwtClaimsSetBuilder.expirationTime(Math.floorDiv(session.getExpirationTime(), 1000));
			}

			if(session.getStatelessData() != null) {
				jwtClaimsSetBuilder.addCustomClaim(CLAIM_SESSION_DATA, session.getStatelessData());
			}

			return this.doGenerateJWT(jwtClaimsSetBuilder);
		});
	}

	/**
	 * <p>
	 * Generates the JWT from the prefilled JWT claims set builder.
	 * </p>
	 *
	 * @param jwtClaimsSetBuilder a prefilled JWT claims set builder
	 *
	 * @return a mono generating the JWT
	 */
	protected abstract Mono<C> doGenerateJWT(JWTClaimsSet.Builder<JWTClaimsSet, ?> jwtClaimsSetBuilder);

	@Override
	public Mono<C> readJWT(String sessionId) {
		return this.doReadJWT(sessionId)
			.doOnNext(jwt -> {
				if(StringUtils.isNotBlank(this.issuer)) {
					jwt.getPayload().validate(JWTClaimsSetValidator.issuer(this.issuer)).ifInvalidThrow();
				}
			});
	}

	/**
	 * <p>
	 * Reads the JWT session id and returns the corresponding JWT object.
	 * </p>
	 *
	 * @param sessionId a JWT session id
	 *
	 * @return a mono emitting the JWT corresponding object
	 */
	protected abstract Mono<C> doReadJWT(String sessionId);
}

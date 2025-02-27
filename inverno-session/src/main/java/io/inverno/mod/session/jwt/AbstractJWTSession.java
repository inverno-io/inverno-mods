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

import io.inverno.mod.base.converter.Convertible;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.session.AbstractSession;
import io.inverno.mod.session.SessionDataSaveStrategy;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base {@link JWTSession} implementation class.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 */
public abstract class AbstractJWTSession<A, B> extends AbstractSession<A, JWTSession<A, B>> implements JWTSession<A, B> {

	/**
	 * The JWT session token id (i.e. JTI)
	 */
	protected String tokenId;
	/**
	 * The stateless session data stored in the JWT session id.
	 */
	protected B statelessSessionData;
	/**
	 * Flag indicating whether stateless data were set.
	 */
	protected boolean statelessDataSet;
	/**
	 * Flag indicating whether stateless data were get.
	 */
	protected boolean statelessDataGet;
	/**
	 * Stateless session data save strategy.
	 */
	protected SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy;
	/**
	 * Flag indicating whether expiration settings were changed.
	 */
	protected boolean expirationSet;

	/**
	 * <p>
	 * Creates a new JWT session.
	 * </p>
	 *
	 * @param sessionIdGenerator               a JWT session id generator
	 * @param sessionStore                     a JWT session store
	 * @param maxInactiveInterval              the maximum inactive interval in milliseconds
	 * @param expirationTime                   the expiration time in milliseconds
	 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
	 *
	 * @throws IllegalArgumentException if both maximum inactive interval and expiration time are null
	 */
	protected AbstractJWTSession(JWTSessionIdGenerator<A, B> sessionIdGenerator, JWTSessionStore<A, B> sessionStore, Long maxInactiveInterval, Long expirationTime, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy) throws IllegalArgumentException{
		this(sessionIdGenerator, sessionStore, null, null, System.currentTimeMillis(), System.currentTimeMillis(), maxInactiveInterval, expirationTime, null, statelessSessionDataSaveStrategy, true);
	}

	/**
	 * <p>
	 * Creates an existing JWT session.
	 * </p>
	 *
	 * @param sessionIdGenerator               a JWT session id generator
	 * @param sessionStore                     a JWT session store
	 * @param id                               the JWT session id
	 * @param claimsSet                        the JWT session claims set
	 * @param statelessSessionData             the stateless session data
	 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
	 * @param lastAccessedTime                 the last accessed time in milliseconds
	 * @param isNew                            true to set the original id to null, false otherwise
	 *
	 * @throws IllegalArgumentException if both maximum inactive interval and expiration time are null
	 */
	protected AbstractJWTSession(JWTSessionIdGenerator<A, B> sessionIdGenerator, JWTSessionStore<A, B> sessionStore, String id, JWTClaimsSet claimsSet, B statelessSessionData, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy, long lastAccessedTime, boolean isNew) throws IllegalArgumentException {
		this(sessionIdGenerator, sessionStore, id, claimsSet.getJWTId(), claimsSet.getIssuedAt() * 1000, lastAccessedTime, claimsSet.getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(Convertible::asLong).orElse(null), claimsSet.getExpirationTime() != null ? claimsSet.getExpirationTime() * 1000 : null, statelessSessionData, statelessSessionDataSaveStrategy, isNew);
	}

	/**
	 * <p>
	 * Creates an existing JWT session.
	 * </p>
	 *
	 * @param sessionIdGenerator               a JWT session id generator
	 * @param sessionStore                     a JWT session store
	 * @param id                               the JWT session id
	 * @param tokenId                          tje JWT token id (i.e. JTI)
	 * @param creationTime                     the creation time in milliseconds
	 * @param lastAccessedTime                 the last accessed time in milliseconds
	 * @param maxInactiveInterval              the maximum inactive interval in milliseconds
	 * @param expirationTime                   the expiration time in milliseconds
	 * @param statelessSessionData             the stateless session data save strategy
	 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
	 * @param isNew                            true to set the original id to null, false otherwise
	 *
	 * @throws IllegalArgumentException if both maximum inactive interval and expiration time are null
	 */
	protected AbstractJWTSession(JWTSessionIdGenerator<A, B> sessionIdGenerator, JWTSessionStore<A, B> sessionStore, String id, String tokenId, long creationTime, long lastAccessedTime, Long maxInactiveInterval, Long expirationTime, B statelessSessionData, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy, boolean isNew) throws IllegalArgumentException {
		super(sessionIdGenerator, sessionStore, id, creationTime, lastAccessedTime, maxInactiveInterval, expirationTime, isNew);
		this.tokenId = tokenId;
		this.statelessSessionData = statelessSessionData;
		this.statelessSessionDataSaveStrategy = statelessSessionDataSaveStrategy;
	}

	/**
	 * <p>
	 * Returns the JWT token id (i.e. JTI)
	 * </p>
	 *
	 * @return the JWT token id
	 */
	public String getTokenId() {
		return tokenId;
	}

	@Override
	protected void setLastAccessedTime(long lastAccessedTime) {
		super.setLastAccessedTime(lastAccessedTime);
	}

	@Override
	public void setStatelessData(B statelessSessionData) {
		this.statelessSessionData = statelessSessionData;
		this.statelessDataSet = true;
	}

	@Override
	public B getStatelessData() {
		this.statelessDataGet = true;
		return this.statelessSessionData;
	}

	@Override
	public void setMaxInactiveInterval(long maxInactiveInterval) {
		if(this.maxInactiveInterval == null || this.maxInactiveInterval != maxInactiveInterval) {
			this.expirationTime = null;
			this.maxInactiveInterval = maxInactiveInterval;
			this.expirationSet = true;
		}
	}

	@Override
	public void setExpirationTime(long expirationTime) {
		if(this.expirationTime == null || this.expirationTime != expirationTime) {
			this.expirationTime = expirationTime;
			this.maxInactiveInterval = null;
			this.expirationSet = true;
		}
	}

	@Override
	public boolean isExpired() {
		return super.isExpired();
	}

	@Override
	public Mono<String> refreshId(boolean force) {
		return Mono.defer(() -> {
			if(force || this.id == null || this.expirationSet || this.statelessDataSet || (this.statelessDataGet && this.statelessSessionDataSaveStrategy.getAndSetSaveState(this.statelessSessionData, false))) {
				String oldTokenId = this.tokenId;
				return ((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).generateJWT(this)
					.flatMap(jwt -> {
						this.id = jwt.toCompact();
						this.tokenId = jwt.getPayload().getJWTId();
						this.statelessDataSet = false;
						this.statelessDataGet = false;
						this.expirationSet = false;
						if(oldTokenId != null) {
							return ((JWTSessionStore<A, B>)this.sessionStore).moveByTokenId(oldTokenId, jwt).thenReturn(this.id);
						}
						return Mono.just(this.id);
					});
			}
			else {
				return Mono.just(this.id);
			}
		});
	}

	@Override
	public Mono<Void> invalidate() {
		return Mono.defer(() -> {
			if(this.invalidated) {
				return Mono.empty();
			}
			this.invalidated = true;
			return ((JWTSessionStore<A, B>)this.sessionStore).removeByTokenId(this.tokenId);
		});
	}
}

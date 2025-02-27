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
package io.inverno.mod.session;

import reactor.core.publisher.Mono;

/**
 * <p>
 * Base {@link Session} implementation class.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 */
public abstract class AbstractSession<A, B extends Session<A>> implements Session<A> {

	/**
	 * The session id generator.
	 */
	protected final SessionIdGenerator<A, B> sessionIdGenerator;
	/**
	 * The session store.
	 */
	protected final SessionStore<A, B> sessionStore;
	/**
	 * The original id.
	 */
	protected final String originalId;
	/**
	 * The creation time in milliseconds
	 */
	protected final long creationTime;

	/**
	 * The session id.
	 */
	protected String id;
	/**
	 * The last accessed time in milliseconds.
	 */
	protected long lastAccessedTime;
	/**
	 * The maximum inactive interval in milliseconds.
	 */
	protected Long maxInactiveInterval;
	/**
	 * The expiration time in milliseconds.
	 */
	protected Long expirationTime;
	/**
	 * Flag indicating whether session was invalidated.
	 */
	protected boolean invalidated;

	/**
	 * <p>
	 * Creates a new session.
	 * </p>
	 *
	 * @param sessionIdGenerator  the session id generator
	 * @param sessionStore        the session store
	 * @param maxInactiveInterval the maximum inactive interval in milliseconds
	 * @param expirationTime      the expiration time in milliseconds
	 *
	 * @throws IllegalArgumentException if both maximum inactive interval and expiration time are null
	 */
	protected AbstractSession(SessionIdGenerator<A, B> sessionIdGenerator, SessionStore<A, B> sessionStore, Long maxInactiveInterval, Long expirationTime) throws IllegalArgumentException {
		this(sessionIdGenerator, sessionStore, null, System.currentTimeMillis(), System.currentTimeMillis(), maxInactiveInterval, expirationTime, true);
	}

	/**
	 * <p>
	 * Creates an existing session.
	 * </p>
	 *
	 * @param sessionIdGenerator  the session id generator
	 * @param sessionStore        the session store
	 * @param id                  the original session id
	 * @param creationTime        the creation time in milliseconds
	 * @param lastAccessedTime    the last accessed time in milliseconds
	 * @param maxInactiveInterval the maximum inactive interval in milliseconds
	 * @param expirationTime      the expiration time in milliseconds
	 * @param isNew               true to set the original id to null, false otherwise
	 *
	 * @throws IllegalArgumentException if both maximum inactive interval and expiration time are null
	 */
	protected AbstractSession(SessionIdGenerator<A, B> sessionIdGenerator, SessionStore<A, B> sessionStore, String id, long creationTime, long lastAccessedTime, Long maxInactiveInterval, Long expirationTime, boolean isNew) throws IllegalArgumentException {
		this.sessionIdGenerator = sessionIdGenerator;
		this.sessionStore = sessionStore;
		this.id = id;
		this.originalId = isNew ? null : id;
		this.creationTime = creationTime;
		this.lastAccessedTime = lastAccessedTime;
		this.maxInactiveInterval = maxInactiveInterval;
		if(maxInactiveInterval == null) {
			if(expirationTime == null) {
				throw new IllegalArgumentException("One of maximum inactive interval or expiration time is required");
			}
			this.expirationTime = expirationTime;
		}
	}

	@Override
	public String getOriginalId() {
		return this.originalId;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public long getCreationTime() {
		return this.creationTime;
	}

	@Override
	public long getLastAccessedTime() {
		return this.lastAccessedTime;
	}

	/**
	 * <p>
	 * Sets the last accessed time.
	 * </p>
	 *
	 * @param lastAccessedTime the last accessed time
	 */
	protected void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	@Override
	public void setMaxInactiveInterval(long maxInactiveInterval) {
		this.expirationTime = null;
		this.maxInactiveInterval = maxInactiveInterval;
	}

	@Override
	public Long getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	@Override
	public void setExpirationTime(long expirationTime) {
		this.maxInactiveInterval = null;
		this.expirationTime = expirationTime;
	}

	@Override
	public long getExpirationTime() {
		return this.expirationTime != null ? this.expirationTime : this.lastAccessedTime + this.maxInactiveInterval;
	}

	@Override
	public boolean isExpired() {
		if(this.expirationTime != null) {
			return System.currentTimeMillis() > this.expirationTime;
		}
		else if(this.maxInactiveInterval != null) {
			return this.lastAccessedTime + this.maxInactiveInterval <= System.currentTimeMillis();
		}
		// should never happen
		throw new IllegalStateException();
	}

	@Override
	public boolean isInvalidated() {
		return this.invalidated;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<String> refreshId(boolean force) {
		return Mono.defer(() -> {
			if(force || this.id == null) {
				String oldSessionId = this.id;
				return this.sessionIdGenerator
					.generate((B) this)
					.flatMap(newSessionId -> {
						this.id = newSessionId;
						if(oldSessionId != null) {
							return this.sessionStore.move(oldSessionId, newSessionId).thenReturn(newSessionId);
						}
						return Mono.just(newSessionId);
					});
			}
			else {
				return Mono.just(this.id);
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> save() {
		return this.sessionStore.save((B)this);
	}

	@Override
	public Mono<Void> invalidate() {
		return Mono.defer(() -> {
			if(this.invalidated) {
				return Mono.empty();
			}
			this.invalidated = true;
			return this.sessionStore.remove(this.id);
		});
	}
}

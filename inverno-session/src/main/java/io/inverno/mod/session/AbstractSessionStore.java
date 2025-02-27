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

/**
 * <p>
 * Base {@link SessionStore} implementation class.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 */
public abstract class AbstractSessionStore<A, B extends Session<A>> implements SessionStore<A, B> {

	/**
	 * The session id generator.
	 */
	protected final SessionIdGenerator<A, B> sessionIdGenerator;
	/**
	 * The initial maximum inactive interval in milliseconds to set when creating a new session.
	 */
	protected final Long maxInactiveInterval;
	/**
	 * The period in milliseconds after a which a new session must expire.
	 */
	protected final Long expireAfterPeriod;

	/**
	 * <p>
	 * Creates a base session store.
	 * </p>
	 *
	 * @param sessionIdGenerator  a session id generator
	 * @param maxInactiveInterval the initial maximum inactive interval in milliseconds
	 * @param expireAfterPeriod   the period in milliseconds after which a new session must expire
	 *
	 * @throws IllegalArgumentException if both maximum inactive interval and expire after period are null
	 */
	protected AbstractSessionStore(SessionIdGenerator<A, B> sessionIdGenerator, Long maxInactiveInterval, Long expireAfterPeriod) throws IllegalArgumentException {
		this.sessionIdGenerator = sessionIdGenerator;
		this.maxInactiveInterval = maxInactiveInterval;
		if(maxInactiveInterval == null) {
			if(expireAfterPeriod == null) {
				throw new IllegalArgumentException("One of maximum inactive interval or expire after period is required");
			}
			this.expireAfterPeriod = expireAfterPeriod;
		}
		else {
			this.expireAfterPeriod = null;
		}
	}

	/**
	 * <p>
	 * Returns the session id generator.
	 * </p>
	 *
	 * @return the session id generator
	 */
	public SessionIdGenerator<A, ? extends Session<A>> getSessionIdGenerator() {
		return sessionIdGenerator;
	}

	/**
	 * <p>
	 * Returns the initial maximum inactive interval in milliseconds to set when creating a new session.
	 * </p>
	 *
	 * @return the maximum inactive interval in milliseconds
	 */
	public Long getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	/**
	 * <p>
	 * Returns the period in milliseconds after a which a new session must expire.
	 * </p>
	 *
	 * @return the period in milliseconds after which a new session must expire
	 */
	public Long getExpireAfterPeriod() {
		return expireAfterPeriod;
	}

	/**
	 * <p>
	 * A base session store builder.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 * @param <B> the session type
	 * @param <C> the session store type
	 * @param <D> the session store builder type
	 */
	protected static abstract class Builder<A, B extends Session<A>, C extends AbstractSessionStore<A, B>, D extends Builder<A, B, C, D>> {

		/**
		 * The session id generator.
		 */
		protected final SessionIdGenerator<A, B> sessionIdGenerator;

		/**
		 * The initial maximum inactive interval in milliseconds to set when creating a new session.
		 */
		protected Long maxInactiveInterval;
		/**
		 * The period in milliseconds after a which a new session must expire.
		 */
		protected Long expireAfterPeriod;

		/**
		 * <p>
		 * Creates a base session store builder.
		 * </p>
		 *
		 * @param sessionIdGenerator a session id generator
		 */
		protected Builder(SessionIdGenerator<A, B> sessionIdGenerator) {
			this.sessionIdGenerator = sessionIdGenerator;
			this.maxInactiveInterval = Session.DEFAULT_MAX_INACTIVE_INTERVAL;
		}

		/**
		 * <p>
		 * Sets the initial maximum inactive interval in milliseconds to set when creating a new session.
		 * </p>
		 *
		 * @param maxInactiveInterval a maximum inactive interval in milliseconds
		 *
		 * @return the builder
		 */
		@SuppressWarnings("unchecked")
		public D maxInactiveInterval(long maxInactiveInterval) {
			this.maxInactiveInterval = maxInactiveInterval;
			this.expireAfterPeriod = null;
			return (D)this;
		}

		/**
		 * <p>
		 * Sets the period in milliseconds after a which a new session must expire.
		 * </p>
		 *
		 * @param expireAfterPeriod a period in milliseconds
		 *
		 * @return the builder
		 */
		@SuppressWarnings("unchecked")
		public D expireAfterPeriod(long expireAfterPeriod) {
			this.expireAfterPeriod = expireAfterPeriod;
			this.maxInactiveInterval = null;
			return (D)this;
		}

		/**
		 * <p>
		 * Builds a session store.
		 * </p>
		 *
		 * @return a new session store
		 */
		public abstract C build();
	}
}

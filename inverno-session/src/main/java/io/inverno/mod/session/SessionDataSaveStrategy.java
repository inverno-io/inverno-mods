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

import io.inverno.mod.session.internal.GenericSessionDataSaveStrategies;

/**
 * <p>
 * A strategy for determining whether resolved session data shall be saved when saving a session.
 * </p>
 *
 * <p>
 * This can be used by specific {@link SessionStore} that wants to optimize communications with an underlying data store. Since the session instance holds an opaque session data instance which may or
 * may not be updated after being returned by {@link Session#getData()}, a session store needs such external strategy to determine whether data should be saved or not. The session data save strategy
 * shall have the knowledge of the actual session data type and therefore be able to determine changes in the data providing it holds some state.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 */
@FunctionalInterface
public interface SessionDataSaveStrategy<A> {

	/**
	 * <p>
	 * Returns a strategy where session data are saved whenever they are resolved.
	 * </p>
	 *
	 * <p>
	 * This is the default strategy for stateful session data stored in the backend. It should be suitable for most usage, but it is not optimal and using the {@link #onSetOnly()} strategy or
	 * providing a more specific implementation when performance is at stake is recommended.
	 * </p>
	 *
	 * <p>
	 * This strategy is not suited for stateless data which are stored in the session id (see {@link io.inverno.mod.session.jwt.JWTSession}) as it may lead to multiple session id refresh during the
	 * processing of a single request and as such it shall never be used in that particular case.
	 * </p>
	 *
	 * @return a strategy that advices to save session data on get (i.e. always)
	 *
	 * @param <A> the session data type
	 */
	@SuppressWarnings("unchecked")
	static <A> SessionDataSaveStrategy<A> onGet() {
		return (SessionDataSaveStrategy<A>) GenericSessionDataSaveStrategies.ON_GET;
	}

	/**
	 * <p>
	 * Returns a strategy where session data are only saved when it was explicitly set in the session.
	 * </p>
	 *
	 * <p>
	 * This is the default behaviour for stateless session data stored in the session id in the frontend (see {@link io.inverno.mod.session.jwt.JWTSession}).
	 * </p>
	 *
	 * @return a strategy that advices to save session data on set only
	 *
	 * @param <A> the session data type
	 */
	@SuppressWarnings("unchecked")
	static <A> SessionDataSaveStrategy<A> onSetOnly() {
		return (SessionDataSaveStrategy<A>) GenericSessionDataSaveStrategies.ON_SET_ONLY;
	}

	/**
	 * <p>
	 * Determines whether the specified session data should be saved and set the new save state into the data.
	 * </p>
	 *
	 * <p>
	 * This is typically invoked in a session store implementation right before saving a session with the {@code saveNeeded} set to {@code false} to prevent duplicate savings.
	 * </p>
	 *
	 * @param sessionData the session data to save
	 * @param saveNeeded  the new save state to store in the session data
	 *
	 * @return true if the data should be saved, false otherwise
	 */
	boolean getAndSetSaveState(A sessionData, boolean saveNeeded);
}

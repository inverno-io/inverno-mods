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
package io.inverno.mod.session.http.context;

import io.inverno.mod.session.Session;

/**
 * <p>
 * A session context for exposing basic sessions.
 * </p>
 *
 * <p>
 * It basically fixes the session type in order to simplify configuration in an application that does not require more advanced or custom session types.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 */
public interface BasicSessionContext<A> extends SessionContext<A, Session<A>> {

	/**
	 * <p>
	 * An intercepted basic session context used by the session interceptor to populate the session context with a basic session.
	 * </p>
	 *
	 * <p>
	 * As for the {@link SessionContext.Intercepted}, it should only be considered when configuring basic session support.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 */
	interface Intercepted<A> extends BasicSessionContext<A>, SessionContext.Intercepted<A, Session<A>> {

	}
}

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
package io.inverno.mod.session.internal;

import io.inverno.mod.session.SessionDataSaveStrategy;

/**
 * <p>
 * Generic {@link SessionDataSaveStrategy} implementations.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 */
public final class GenericSessionDataSaveStrategies {

	/**
	 * On get session data save strategy which always returns true.
	 */
	public static final SessionDataSaveStrategy<?> ON_GET = (data, saveState) -> true;

	/**
	 * On set only session data save strategy which always returns false.
	 */
	public static final SessionDataSaveStrategy<?> ON_SET_ONLY = (data, saveState) -> false;

	private GenericSessionDataSaveStrategies() {}
}

/*
 * Copyright 2022 Jeremy KUHN
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

package io.inverno.mod.base.net;

/**
 * <p>
 * Net server configuration.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface NetServerConfiguration extends NetConfiguration {

	/**
	 * <p>
	 * The accept backlog.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 1024}.
	 * </p>
	 * 
	 * @return the accept backlog
	 */
	default Integer accept_backlog() {
		return 1024;
	}
	
	/**
	 * <p>
	 * Enables/Disables TCP fast open.
	 * </p>
	 * 
	 * @return fast-open requests limit
	 */
	Integer tcp_fast_open();
}

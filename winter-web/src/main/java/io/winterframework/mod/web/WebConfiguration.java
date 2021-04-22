/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.web;

import io.winterframework.core.annotation.NestedBean;
import io.winterframework.mod.configuration.Configuration;
import io.winterframework.mod.http.server.HttpServerConfiguration;

/**
 * <p>
 * Web module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Configuration
public interface WebConfiguration {

	/**
	 * <p>
	 * The HTTP server configuration.
	 * </p>
	 * 
	 * @return the HTTP server configuration
	 */
	@NestedBean
	HttpServerConfiguration http_server();
	
	/**
	 * <p>
	 * Enables/disables routes to generated <a href="https://www.openapis.org/">Open
	 * API</a> specifications.
	 * </p>
	 * 
	 * @return true to expose generated Open API specifications, false otherwise
	 */
	default boolean enable_open_api() {
		return false;
	}
	
	/**
	 * <p>
	 * Enables/disables routes to <a href="https://www.webjars.org/">WebJars</a>
	 * resources.
	 * </p>
	 * 
	 * @return true to expose WebJars resources, false otherwise
	 */
	default boolean enable_webjars() {
		return false;
	}
}

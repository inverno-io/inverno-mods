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
 * @author jkuhn
 *
 */
@Configuration
public interface WebConfiguration {

	@NestedBean
	HttpServerConfiguration web();
	
	default boolean enable_open_api() {
		return false;
	}
	
	default boolean enable_webjars() {
		return false;
	}
}

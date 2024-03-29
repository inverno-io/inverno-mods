/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.test.config.moduleD;

import io.inverno.core.annotation.NestedBean;

import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.test.config.moduleA.ConfigA;

@Configuration
public interface ConfigD {

	default String param1() {
		return "abc";
	}
	
	@NestedBean
	default ConfigA configA() {
		return new ConfigA() {
			
			public String param1() {
				return "default param1";
			}
			
			public int param2() {
				return 1234;
			}
		};
	}
}

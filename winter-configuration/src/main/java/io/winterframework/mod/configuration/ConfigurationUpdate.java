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
package io.winterframework.mod.configuration;

import java.util.Map;

/**
 * @author jkuhn
 *
 */
public interface ConfigurationUpdate<A extends ConfigurationUpdate<A, B, C>, B extends ExecutableConfigurationUpdate<A, B, C>, C extends ConfigurationUpdateResult<?>> {

	default B set(String name1, Object value1) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1));
	}
	
	default B set(String name1, Object value1, String name2, Object value2) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2));
	}

	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3));
	}
	
	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4));
	}
	
	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5));
	}
	
	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6));
	}
	
	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7));
	}
	
	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8));
	}
	
	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8, String name9, Object value9) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8, name9, value9));
	}
	
	default B set(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5, String name6, Object value6, String name7, Object value7, String name8, Object value8, String name9, Object value9, String name10, Object value10) throws IllegalArgumentException {
		return this.set(Map.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5, name6, value6, name7, value7, name8, value8, name9, value9, name10, value10));
	}
	
	B set(Map<String, Object> values) throws IllegalArgumentException;
}

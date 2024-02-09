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
package io.inverno.mod.web.server.internal;

import java.util.Map;

import io.inverno.mod.web.server.PathParameters;

/**
 * <p>
 * Mutable path parameters.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface MutablePathParameters extends PathParameters {

	/**
	 * <p>
	 * Sets the specified parameter.
	 * </p>
	 * 
	 * <p>
	 * When the specified value is empty, it is considered null (i.e. missing).
	 * </p>
	 * 
	 * @param name  the parameter name
	 * @param value the parameter value
	 */
	void put(String name, String value);
	
	/**
	 * <p>
	 * Sets all specified parameters.
	 * </p>
	 * 
	 * <p>
	 * When a specified value is empty, it is considered null (i.e. missing).
	 * </p>
	 * 
	 * @param parameters a map of parameters
	 */
	void putAll(Map<String, String> parameters);
	
	/**
	 * <p>
	 * Removes the parameter with the specified name.
	 * </p>
	 * 
	 * @param name the parameter name
	 * 
	 * @return the removed value or null if no value was removed
	 */
	String remove(String name);
}

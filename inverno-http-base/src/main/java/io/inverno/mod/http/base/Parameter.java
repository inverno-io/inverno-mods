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
package io.inverno.mod.http.base;

import io.inverno.mod.base.converter.Convertible;
import java.lang.reflect.Type;

/**
 * <p>
 * Base parameter interface defining common HTTP parameter (eg. header, cookie, query parameter...).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface Parameter extends Convertible<String> {

	/**
	 * <p>
	 * Returns the parameter name.
	 * </p>
	 * 
	 * @return a name
	 */
	@Override
	String getName();
	
	/**
	 * <p>
	 * A factory for creating parameters.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	interface Factory {
		
		/**
		 * <p>
		 * Creates a parameter with the specified name and value.
		 * </p>
		 *
		 * @param <T>   the value type
		 * @param name  the name
		 * @param value the value
		 *
		 * @return a new parameter
		 */
		<T> Parameter create(String name, T value);

		/**
		 * <p>
		 * Creates a parameter with the specified name and value.
		 * </p>
		 *
		 * @param <T>   the value type
		 * @param name  the name
		 * @param value the value
		 * @param type  the value type
		 *
		 * @return a new parameter
		 */
		default <T> Parameter create(String name, T value, Class<T> type) {
			return this.create(name, value, (Type)type);
		}

		/**
		 * <p>
		 * Creates a parameter with the specified name and value.
		 * </p>
		 *
		 * @param <T>   the value type
		 * @param name  the name
		 * @param value the value
		 * @param type  the value type
		 *
		 * @return a new parameter
		 */
		<T> Parameter create(String name, T value, Type type);

	}
}

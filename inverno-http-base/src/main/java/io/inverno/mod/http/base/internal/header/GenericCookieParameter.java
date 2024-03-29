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
package io.inverno.mod.http.base.internal.header;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.CookieParameter;
import io.inverno.mod.http.base.internal.GenericParameter;

/**
 * <p>
 * Generic {@link CookieParameter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see GenericParameter
 */
public class GenericCookieParameter extends GenericParameter implements CookieParameter {
	
	/**
	 * <p>
	 * Creates a generic cookie parameter.
	 * </p>
	 * 
	 * @param parameterConverter a string object parameter
	 * @param name               the parameter name
	 * @param value              the parameter value
	 */
	public GenericCookieParameter(ObjectConverter<String> parameterConverter, String name, String value) {
		super(name, value, parameterConverter);
	}
}

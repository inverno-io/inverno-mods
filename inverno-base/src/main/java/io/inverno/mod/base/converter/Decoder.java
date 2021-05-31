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
package io.inverno.mod.base.converter;

import java.lang.reflect.Type;

/**
 * <p>
 * A decoder is used to decode an object into another object.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <From> the encoded type
 * @param <To>   the decoded type
 */
public interface Decoder<From, To> {

	/**
	 * <p>
	 * Decodes the specified value to an object whose type is represented by the
	 * specified class.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the object to decode
	 * @param type  the class of the decoded object
	 * 
	 * @return a decoded object
	 * @throws ConverterException if there was an error decoding the value
	 */
	<T extends To> T decode(From value, Class<T> type) throws ConverterException;
	
	/**
	 * <p>
	 * Decodes the specified value to an object of the specified type.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the object to decode
	 * @param type  the type of the decoded object
	 * 
	 * @return a decoded object
	 * @throws ConverterException if there was an error decoding the value
	 */
	<T extends To> T decode(From value, Type type) throws ConverterException;
}

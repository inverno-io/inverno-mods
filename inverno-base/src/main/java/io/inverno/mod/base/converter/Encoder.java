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
 * An encoder is used to encode an object into another object.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <From> the type of object to encode
 * @param <To>   the type of the encoded object
 */
public interface Encoder<From, To> {

	/**
	 * <p>
	 * Encodes the specified value to the encoded type.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the object to encode
	 * 
	 * @return an encoded object
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	<T extends From> To encode(T value) throws ConverterException;
	
	/**
	 * <p>
	 * Encodes the specified value whose type is represented by the specified class to the encoded type.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the object to encode
	 * @param type  the class of the decoded object
	 * 
	 * @return an encoded object
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	<T extends From> To encode(T value, Class<T> type) throws ConverterException;
	
	/**
	 * <p>
	 * Encodes the specified value whose type is the specified type to the encoded type.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the object to encode
	 * @param type  the type of the decoded object
	 * 
	 * @return an encoded object
	 *
	 * @throws ConverterException if there was an error encoding the value
	 */
	<T extends From> To encode(T value, Type type) throws ConverterException;
}

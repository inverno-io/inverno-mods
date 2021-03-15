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
package io.winterframework.mod.base.converter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * A decoder that can decode an object to a collection of objects.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Decoder
 *
 * @param <From> the encoded type
 * @param <To>   the decoded type
 */
public interface SplittableDecoder<From, To> extends Decoder<From, To> {

	/**
	 * <p>
	 * Decodes the specified value whose type is represented by the specified class
	 * to a list of decoded values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the value to decode
	 * @param type  the class of the decoded object
	 * 
	 * @return a list of decoded values
	 */
	<T extends To> List<T> decodeToList(From value, Class<T> type);
	
	/**
	 * <p>
	 * Decodes the specified value whose type is the specified type to a list of
	 * decoded values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the value to decode
	 * @param type  the type of the decoded object
	 * 
	 * @return a list of decoded values
	 */
	<T extends To> List<T> decodeToList(From value, Type type);
	
	/**
	 * <p>
	 * Decodes the specified value whose type is represented by the specified class
	 * to a set of decoded values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the value to decode
	 * @param type  the class of the decoded object
	 * 
	 * @return a set of decoded values
	 */
	<T extends To> Set<T> decodeToSet(From value, Class<T> type);
	
	/**
	 * <p>
	 * Decodes the specified value whose type is the specified type to a set of
	 * decoded values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the value to decode
	 * @param type  the type of the decoded object
	 * 
	 * @return a set of decoded values
	 */
	<T extends To> Set<T> decodeToSet(From value, Type type);
	
	/**
	 * <p>
	 * Decodes the specified value whose type is represented by the specified class
	 * to an array of decoded values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the value to decode
	 * @param type  the class of the decoded object
	 * 
	 * @return an array of decoded values
	 */
	<T extends To> T[] decodeToArray(From value, Class<T> type);
	
	/**
	 * <p>
	 * Decodes the specified value whose type is the specified type to an array of
	 * decoded values.
	 * </p>
	 * 
	 * @param <T>   the type of the decoded object
	 * @param value the value to decode
	 * @param type  the type of the decoded object
	 * 
	 * @return an array of decoded values
	 */
	<T extends To> T[] decodeToArray(From value, Type type);
}

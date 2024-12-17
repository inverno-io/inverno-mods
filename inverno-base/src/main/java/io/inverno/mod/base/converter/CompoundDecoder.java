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
 * A compound decoder is used in a {@link CompositeDecoder} to decode particular types of objects.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see CompositeDecoder
 * @see CompositeConverter
 * 
 * @param <From> the encoded type
 * @param <To>   the decoded type
 */
public interface CompoundDecoder<From, To> extends Decoder<From, To> {

	/**
	 * <p>
	 * Determines whether the decoder can decode the type represented by the specified class.
	 * </p>
	 * 
	 * @param <T>  the type of the object to decode
	 * @param type the class of the object to decode
	 * 
	 * @return true if the decoder can decode the type, false otherwise
	 */
	<T extends To> boolean canDecode(Class<T> type);
	
	/**
	 * <p>
	 * Determines whether the decoder can decode the specified type.
	 * </p>
	 * 
	 * @param type the type of the object to decode
	 * 
	 * @return true if the decoder can decode the type, false otherwise
	 */
	boolean canDecode(Type type);
}

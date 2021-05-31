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
 * A compound encoder is used in a {@link CompositeEncoder} to encode particular
 * types of objects.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see CompositeEncoder
 * @see CompositeConverter
 * 
 * @param <From> the decoded type
 * @param <To>   the encoded type
 */
public interface CompoundEncoder<From, To> extends Encoder<From, To> {

	/**
	 * <p>
	 * Determines whether the encoder can encode the type represented by the
	 * specified class.
	 * </p>
	 * 
	 * @param <T>  the type of the object to encode
	 * @param type the class of the object to encode
	 * 
	 * @return true if the encoder can encode the type, false otherwise
	 */
	<T extends From> boolean canEncode(Class<T> type);
	
	/**
	 * <p>
	 * Determines whether the encoder can encode the specified type.
	 * </p>
	 * 
	 * @param type the type of the object to decode
	 * 
	 * @return true if the encoder can encode the type, false otherwise
	 */
	boolean canEncode(Type type); 
}

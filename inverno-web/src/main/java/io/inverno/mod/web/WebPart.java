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
package io.inverno.mod.web;

import java.lang.reflect.Type;

import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.http.server.Part;

/**
 * <p>
 * A part with payload decoding support.
 * </p>
 * 
 * <p>
 * Implementors should rely on {@link MediaTypeConverter} to decode a part's
 * payload based on its content type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see MediaTypeConverter
 */
public interface WebPart extends Part {

	/**
	 * <p>
	 * Returns a decoder to decode the part's payload to the specified type based on
	 * the part's content type.
	 * </p>
	 * 
	 * @param <A>  the decoded type
	 * @param type a class of A
	 * 
	 * @return a decoder
	 */
	<A> InboundDataDecoder<A> decoder(Class<A> type);
	
	/**
	 * <p>
	 * Returns a decoder to decode the part's payload to the specified type based on
	 * the part's content type.
	 * </p>
	 * 
	 * @param <A>  the decoded type
	 * @param type the decoded type
	 * 
	 * @return a decoder
	 */
	<A> InboundDataDecoder<A> decoder(Type type);
}

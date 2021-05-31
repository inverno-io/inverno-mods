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

/**
 * <p>
 * An object converter that can convert particular media types.
 * </p>
 * 
 * <p>
 * A typical implementation would convert objects from/to serialized data
 * formatted according to a given media type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ReactiveConverter
 *
 * @param <From> the encoded type
 */
public interface MediaTypeConverter<From> extends ReactiveConverter<From, Object> {

	/**
	 * <p>
	 * Determines whether the converter can convert the specified media type.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return true if the converter can convert the media type, false otherwise
	 */
	boolean canConvert(String mediaType);
}

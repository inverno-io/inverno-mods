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
package io.winterframework.mod.commons.converter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author jkuhn
 *
 */
public interface SplittableDecoder<A, B extends SplittableDecoder<A, B>> extends Decoder<A, B> {

	<T> Collection<T> toCollectionOf(A value, Class<T> type) throws ValueCodecException;
	
	<T> List<T> toListOf(A value, Class<T> type) throws ValueCodecException;
	
	<T> Set<T> toSetOf(A value, Class<T> type) throws ValueCodecException;
	
	<T> T[] toArrayOf(A value, Class<T> type) throws ValueCodecException;
}

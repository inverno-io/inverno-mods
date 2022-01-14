/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.redis.lettuce.internal.operations;

import io.inverno.mod.redis.operations.Bound;
import io.lettuce.core.Range;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public final class SortedSetUtils {
	
	/**
	 * 
	 */
	private SortedSetUtils() {}
	
	/**
	 * <p>
	 * Converts min/max bounds to Lettuce range
	 * </p>
	 * 
	 * @param <T>
	 * @param min
	 * @param max
	 * @return 
	 */
	public static <T> Range<T> convertRange(Bound<? extends T> min, Bound<? extends T> max) {
		return Range.from(convertBoundary(min), convertBoundary(max));
	}
	
	/**
	 * <p>
	 * Converts bound to Lettuce boundary
	 * </p>
	 * 
	 * @param <T>
	 * @param bound
	 * @return 
	 */
	public static <T> Range.Boundary<T> convertBoundary(Bound<? extends T> bound) {
		
		if(bound.isUnbounded()) {
			return Range.Boundary.unbounded();
		}
		if(bound.isInclusive()) {
			return Range.Boundary.including(bound.getValue());
		}
		else {
			return Range.Boundary.excluding(bound.getValue());
		}
	}
}

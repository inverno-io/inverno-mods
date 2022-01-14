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
package io.inverno.mod.redis.operations;

import java.util.Objects;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> bound type
 */
public class Bound<A> {

	private static final Bound<?> UNBOUNDED = new Bound<>(null, true);
	
	private final A value;
	private final boolean inclusive;
	
	/**
	 * @param value
	 * @param inclusive 
	 */
	private Bound(A value, boolean inclusive) {
		this.value = value;
		this.inclusive = inclusive;
	}
	
	/**
	 * 
	 * @return 
	 */
	public boolean isInclusive() {
		return this.inclusive;
	}
	
	/**
	 * 
	 * @return 
	 */
	public boolean isUnbounded() {
		return this.value == null;
	}
	
	/**
	 * 
	 * @return 
	 */
	public A getValue() {
		return this.value;
	}
	
	/**
	 * 
	 * @param <A>
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	public static <A> Bound<A> unbounded() {
		return (Bound<A>) UNBOUNDED;
	}
	
	/**
	 * 
	 * @param <A>
	 * @param value
	 * @return 
	 */
	public static <A> Bound<A> inclusive(A value) {
		Objects.requireNonNull(value);
		return new Bound<>(value, true);
	}
	
	/**
	 * 
	 * @param <A>
	 * @param value
	 * @return 
	 */
	public static <A> Bound<A> exclusive(A value) {
		Objects.requireNonNull(value);
		return new Bound<>(value, false);
	}
}

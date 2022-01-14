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
import java.util.Optional;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public class EntryOptional<A, B> {
	
	private final A key;
		
	private final Optional<B> value;

	/**
	 * 
	 * @param key
	 * @param value 
	 */
	private EntryOptional(A key, B value) {
		Objects.requireNonNull(key);
		this.key = key;
		this.value = Optional.ofNullable(value);
	}

	/**
	 * 
	 * @return 
	 */
	public A getKey() {
		return this.key;
	}

	/**
	 * 
	 * @return 
	 */
	public Optional<B> getValue() {
		return this.value;
	}

	/**
	 * 
	 * @param <A>
	 * @param <B>
	 * @param key
	 * @return 
	 */
	public static <A, B> EntryOptional<A, B> empty(A key) {
		return new EntryOptional<>(key, null);
	}

	/**
	 * 
	 * @param <A>
	 * @param <B>
	 * @param key
	 * @param value
	 * @return 
	 */
	public static <A, B> EntryOptional<A, B> of(A key, B value) {
		return new EntryOptional<>(key, Objects.requireNonNull(value));
	}

	/**
	 * 
	 * @param <A>
	 * @param <B>
	 * @param key
	 * @param value
	 * @return 
	 */
	public static <A, B> EntryOptional<A, B> ofNullable(A key, B value) {
		return new EntryOptional<>(key, value);
	}
}

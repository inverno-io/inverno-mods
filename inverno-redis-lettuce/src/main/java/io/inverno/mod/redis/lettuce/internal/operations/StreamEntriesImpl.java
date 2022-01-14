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

import io.inverno.mod.redis.operations.RedisStreamReactiveOperations;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 * @param <B>
 */
public class StreamEntriesImpl<A, B> implements RedisStreamReactiveOperations.StreamEntries<A, B> {

	private final Map<A, B> entries;
	
	/**
	 * 
	 */
	public StreamEntriesImpl() {
		this.entries = new HashMap<>();
	}

	/**
	 * 
	 * @return 
	 */
	public Map<A, B> getEntries() {
		return this.entries;
	}

	@Override
	public RedisStreamReactiveOperations.StreamEntries<A, B> entry(A field, B value) {
		this.entries.put(field, value);
		return this;
	}
}

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
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class StreamMessageIdsImpl implements RedisStreamReactiveOperations.StreamMessageIds {

	private final List<String> ids;

	/**
	 * 
	 */
	public StreamMessageIdsImpl() {
		this.ids = new LinkedList<>();
	}

	/**
	 * 
	 * @return 
	 */
	public String[] getIds() {
		return ids.toArray(String[]::new);
	}
	
	@Override
	public RedisStreamReactiveOperations.StreamMessageIds id(String id) {
		this.ids.add(id);
		return this;
	}
}

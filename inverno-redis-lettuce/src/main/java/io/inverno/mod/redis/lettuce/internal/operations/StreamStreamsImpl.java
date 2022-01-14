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
import io.lettuce.core.XReadArgs;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class StreamStreamsImpl<A> implements RedisStreamReactiveOperations.StreamStreams<A> {

	private final List<XReadArgs.StreamOffset<A>> streams;
	
	/**
	 * 
	 */
	public StreamStreamsImpl() {
		this.streams = new LinkedList<>();
	}
	
	/**
	 * 
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	public XReadArgs.StreamOffset<A>[] getStreams() {
		return this.streams.toArray(XReadArgs.StreamOffset[]::new);
	}

	@Override
	public RedisStreamReactiveOperations.StreamStreams<A> stream(A key, String messageId) {
		this.streams.add(XReadArgs.StreamOffset.from(key, messageId));
		return this;
	}
}

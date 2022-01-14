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
import io.lettuce.core.models.stream.ClaimedMessages;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 * @param <B>
 */
public class StreamClaimedMessagesImpl<A, B> implements RedisStreamReactiveOperations.StreamClaimedMessages<A, B> {

	private final ClaimedMessages<A, B> claimedMessages;
	
	/**
	 * 
	 * @param claimedMessages 
	 */
	public StreamClaimedMessagesImpl(ClaimedMessages<A, B> claimedMessages) {
		this.claimedMessages = claimedMessages; 
	}

	@Override
	public String getStreamId() {
		return this.claimedMessages.getId();
	}

	@Override
	public List<RedisStreamReactiveOperations.StreamMessage<A, B>> getMessages() {
		return this.claimedMessages.getMessages().stream().map(StreamMessageImpl::new).collect(Collectors.toList());
	}
}

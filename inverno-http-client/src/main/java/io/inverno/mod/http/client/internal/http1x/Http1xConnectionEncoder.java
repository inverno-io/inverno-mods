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
package io.inverno.mod.http.client.internal.http1x;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * <p>
 * A HTTP1.x connection encoder used to write data frame to the server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
interface Http1xConnectionEncoder {

	/**
	 * <p>
	 * Writes the specified message to the specified channel handler context.
	 * </p>
	 * 
	 * @param ctx     the channel handler context
	 * @param msg     the message to write
	 * @param promise the write promise
	 * 
	 * @return a channel future
	 */
	ChannelFuture writeFrame(ChannelHandlerContext ctx, Object msg, ChannelPromise promise);
}

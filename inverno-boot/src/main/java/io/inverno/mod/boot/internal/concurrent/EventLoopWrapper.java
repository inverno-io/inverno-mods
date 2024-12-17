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

package io.inverno.mod.boot.internal.concurrent;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;

/**
 * <p>
 * Event loop wrapper that prevents internal event loops from being shutdown.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class EventLoopWrapper extends EventLoopGroupWrapper implements EventLoop {

	public EventLoopWrapper(EventLoop eventLoop) {
		super(eventLoop);
	}
	
	@Override
	public EventLoop unwrap() {
		return (EventLoop)this.eventLoopGroup;
	}

	@Override
	public EventLoopGroup parent() {
		return this.eventLoopGroup;
	}

	@Override
	public boolean inEventLoop() {
		return ((EventLoop)this.eventLoopGroup).inEventLoop();
	}

	@Override
	public boolean inEventLoop(Thread thread) {
		return ((EventLoop)this.eventLoopGroup).inEventLoop(thread);
	}

	@Override
	public <V> Promise<V> newPromise() {
		return ((EventLoop)this.eventLoopGroup).newPromise();
	}

	@Override
	public <V> ProgressivePromise<V> newProgressivePromise() {
		return ((EventLoop)this.eventLoopGroup).newProgressivePromise();
	}

	@Override
	public <V> Future<V> newSucceededFuture(V result) {
		return ((EventLoop)this.eventLoopGroup).newSucceededFuture(result);
	}

	@Override
	public <V> Future<V> newFailedFuture(Throwable cause) {
		return ((EventLoop)this.eventLoopGroup).newFailedFuture(cause);
	}
}

/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.base.net.NetService;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * An event loop group wrapper that provides the event loops of the root event
 * loop group in order to optimize resource usage when multiple IO event loop
 * groups are used within an application.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see NetService
 */
class EventLoopGroupProxy extends MultithreadEventLoopGroup {

	/**
	 * <p>
	 * Creates a event loop group with the specified amount of threads provided by
	 * the specified root event loop group.
	 * </p>
	 * 
	 * @param nThreads           the number of threads to allocate to the group
	 * @param rootEventLoopGroup the root event loop group
	 */
	public EventLoopGroupProxy(int nThreads, EventLoopGroup rootEventLoopGroup) {
		super(nThreads, (Executor) null, rootEventLoopGroup);
	}

	@Override
	protected EventLoop newChild(Executor executor, Object... args) throws Exception {
		return ((EventLoopGroup) args[0]).next();
	}

	/**
	 * Event loop group is shutdown when the coreEventLoopGroup is shutdown.
	 */
	@Override
	@Deprecated
	public void shutdown() {
	}

	/**
	 * Event loop group is shutdown when the coreEventLoopGroup is shutdown.
	 */
	@Override
	@Deprecated
	public List<Runnable> shutdownNow() {
		return List.of();
	}
	
	/**
	 * Event loop group is shutdown when the coreEventLoopGroup is shutdown.
	 */
	@Override
	public Future<?> shutdownGracefully() {
		return new DefaultPromise<>(GlobalEventExecutor.INSTANCE).setSuccess(null);
	}

	/**
	 * Event loop group is shutdown when the coreEventLoopGroup is shutdown.
	 */
	@Override
	public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
		return new DefaultPromise<>(GlobalEventExecutor.INSTANCE).setSuccess(null);
	}
}

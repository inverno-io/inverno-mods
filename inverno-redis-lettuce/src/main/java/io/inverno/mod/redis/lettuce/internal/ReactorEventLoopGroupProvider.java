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
package io.inverno.mod.redis.lettuce.internal;

import io.inverno.mod.base.concurrent.Reactor;
import io.lettuce.core.resource.DefaultEventLoopGroupProvider;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Lettuce event loop group provider implementation based Inverno's reactor.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class ReactorEventLoopGroupProvider extends DefaultEventLoopGroupProvider {
	
	private final Reactor reactor;
	
	public ReactorEventLoopGroupProvider(Reactor reactor, int numberOfThreads) {
		super(numberOfThreads);
		this.reactor = reactor;
	}
	
	@Override
	protected <T extends EventLoopGroup> EventExecutorGroup doCreateEventLoopGroup(Class<T> type, int numberOfThreads, io.lettuce.core.resource.ThreadFactoryProvider threadFactoryProvider) {
		return this.reactor.createIoEventLoopGroup(numberOfThreads);
	}

	/**
	 * Event loop group is released when the reactor is
	 */
	@Override
	public Promise<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {
		return new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE).setSuccess(true);
	}
	
}

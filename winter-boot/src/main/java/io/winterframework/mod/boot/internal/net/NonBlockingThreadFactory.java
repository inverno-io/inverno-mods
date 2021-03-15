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
package io.winterframework.mod.boot.internal.net;

import java.util.concurrent.ThreadFactory;

import io.netty.util.concurrent.DefaultThreadFactory;
import reactor.core.scheduler.NonBlocking;

/**
 * <p>
 * {@link ThreadFactory} implementation that creates {@link NonBlocking} threads
 * in order to prevent blocking calls from the Reactor APIs.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see NonBlocking
 */
class NonBlockingThreadFactory extends DefaultThreadFactory {

	public NonBlockingThreadFactory(Class<?> poolType) {
		super(poolType);
	}

	public NonBlockingThreadFactory(String poolName) {
		super(poolName);
	}

	public NonBlockingThreadFactory(Class<?> poolType, boolean daemon) {
		super(poolType, daemon);
	}

	public NonBlockingThreadFactory(String poolName, boolean daemon) {
		super(poolName, daemon);
	}

	public NonBlockingThreadFactory(Class<?> poolType, int priority) {
		super(poolType, priority);
	}

	public NonBlockingThreadFactory(String poolName, int priority) {
		super(poolName, priority);
	}

	public NonBlockingThreadFactory(Class<?> poolType, boolean daemon, int priority) {
		super(poolType, daemon, priority);
	}

	public NonBlockingThreadFactory(String poolName, boolean daemon, int priority) {
		super(poolName, daemon, priority);
	}

	public NonBlockingThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
		super(poolName, daemon, priority, threadGroup);
	}

	@Override
	protected Thread newThread(Runnable r, String name) {
		return new NonBlockingThread(r, name);
	}
}

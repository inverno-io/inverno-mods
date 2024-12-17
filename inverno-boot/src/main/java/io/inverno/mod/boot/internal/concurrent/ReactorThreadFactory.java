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

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * <p>
 * {@link java.util.concurrent.ThreadFactory ThreadFactory} implementation that creates {@link reactor.core.scheduler.NonBlocking NonBlocking} threads in order to prevent blocking calls from the Reactor APIs.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see reactor.core.scheduler.NonBlocking NonBlocking
 */
class ReactorThreadFactory extends DefaultThreadFactory {

	private final InternalReactor reactor;
	
	public ReactorThreadFactory(InternalReactor reactor, Class<?> poolType) {
		super(poolType);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, String poolName) {
		super(poolName);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, Class<?> poolType, boolean daemon) {
		super(poolType, daemon);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, String poolName, boolean daemon) {
		super(poolName, daemon);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, Class<?> poolType, int priority) {
		super(poolType, priority);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, String poolName, int priority) {
		super(poolName, priority);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, Class<?> poolType, boolean daemon, int priority) {
		super(poolType, daemon, priority);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, String poolName, boolean daemon, int priority) {
		super(poolName, daemon, priority);
		this.reactor = reactor;
	}

	public ReactorThreadFactory(InternalReactor reactor, String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
		super(poolName, daemon, priority, threadGroup);
		this.reactor = reactor;
	}

	@Override
	protected Thread newThread(Runnable r, String name) {
		return new ReactorThread(this.reactor, r, name);
	}
}

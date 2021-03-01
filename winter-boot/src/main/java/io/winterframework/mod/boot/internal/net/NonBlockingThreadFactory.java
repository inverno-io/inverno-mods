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

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author jkuhn
 *
 */
class NonBlockingThreadFactory extends DefaultThreadFactory {

	/**
	 * @param poolType
	 */
	public NonBlockingThreadFactory(Class<?> poolType) {
		super(poolType);
	}

	/**
	 * @param poolName
	 */
	public NonBlockingThreadFactory(String poolName) {
		super(poolName);
	}

	/**
	 * @param poolType
	 * @param daemon
	 */
	public NonBlockingThreadFactory(Class<?> poolType, boolean daemon) {
		super(poolType, daemon);
	}

	/**
	 * @param poolName
	 * @param daemon
	 */
	public NonBlockingThreadFactory(String poolName, boolean daemon) {
		super(poolName, daemon);
	}

	/**
	 * @param poolType
	 * @param priority
	 */
	public NonBlockingThreadFactory(Class<?> poolType, int priority) {
		super(poolType, priority);
	}

	/**
	 * @param poolName
	 * @param priority
	 */
	public NonBlockingThreadFactory(String poolName, int priority) {
		super(poolName, priority);
	}

	/**
	 * @param poolType
	 * @param daemon
	 * @param priority
	 */
	public NonBlockingThreadFactory(Class<?> poolType, boolean daemon, int priority) {
		super(poolType, daemon, priority);
	}

	/**
	 * @param poolName
	 * @param daemon
	 * @param priority
	 */
	public NonBlockingThreadFactory(String poolName, boolean daemon, int priority) {
		super(poolName, daemon, priority);
	}

	/**
	 * @param poolName
	 * @param daemon
	 * @param priority
	 * @param threadGroup
	 */
	public NonBlockingThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
		super(poolName, daemon, priority, threadGroup);
	}

	@Override
	protected Thread newThread(Runnable r, String name) {
		return new NonBlockingThread(r, name);
	}
	
}

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

import io.netty.util.concurrent.FastThreadLocalThread;
import reactor.core.scheduler.NonBlocking;

/**
 * @author jkuhn
 *
 */
class NonBlockingThread extends FastThreadLocalThread implements NonBlocking {

	/**
	 * 
	 */
	public NonBlockingThread() {
	}

	/**
	 * @param target
	 */
	public NonBlockingThread(Runnable target) {
		super(target);
	}

	/**
	 * @param name
	 */
	public NonBlockingThread(String name) {
		super(name);
	}

	/**
	 * @param group
	 * @param target
	 */
	public NonBlockingThread(ThreadGroup group, Runnable target) {
		super(group, target);
	}

	/**
	 * @param group
	 * @param name
	 */
	public NonBlockingThread(ThreadGroup group, String name) {
		super(group, name);
	}

	/**
	 * @param target
	 * @param name
	 */
	public NonBlockingThread(Runnable target, String name) {
		super(target, name);
	}

	/**
	 * @param group
	 * @param target
	 * @param name
	 */
	public NonBlockingThread(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
	}

	/**
	 * @param group
	 * @param target
	 * @param name
	 * @param stackSize
	 */
	public NonBlockingThread(ThreadGroup group, Runnable target, String name, long stackSize) {
		super(group, target, name, stackSize);
	}

}

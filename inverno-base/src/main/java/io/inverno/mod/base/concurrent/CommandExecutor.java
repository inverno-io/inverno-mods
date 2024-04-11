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

package io.inverno.mod.base.concurrent;

import io.netty.util.internal.PlatformDependent;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * <p>
 * A lock-free command executor which guarantees the execution of commands in sequence.
 * </p>
 * 
 * <p>
 * It allows to eliminate race conditions when accessing a target holding a shared state.
 * </p>
 * 
 * <p>
 * It is backed by a multiple producer/single consumer queue storing the commands which are executed in the {@link #execute(java.util.function.Consumer) } method by a calling thread which was able to
 * acquire the lock.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the target state type
 */
public class CommandExecutor<A> {
	
	/**
	 * The command target holding the shared state.
	 */
	private final A target;
	
	/**
	 * A semahore.
	 */
	private final Semaphore semaphore;
	
	/**
	 * Multiple producer/single consumer queue.
	 */
	private final Queue<Consumer<A>> commands;

	/**
	 * <p>
	 * Creates a command executor on the specified target.
	 * </p>
	 * 
	 * @param target the target state
	 */
	public CommandExecutor(A target) {
		this.target = target;
		this.semaphore = new Semaphore(1);
		this.commands = PlatformDependent.newMpscQueue();
	}

	/**
	 * <p>
	 * Returns the target state.
	 * </p>
	 * 
	 * @return the target
	 */
	public A getTarget() {
		return target;
	}
	
	/**
	 * <p>
	 * Executes the specified command.
	 * </p>
	 *
	 * <p>
	 * The command is enqueued and eventually executed by the thread which was able to acquire the lock. As result execution can be synchronous or asynchronous depending on the concurrency level (i.e.
	 * how many threads are executing commands concurrently).
	 * </p>
	 *
	 * @param command the command to execute
	 */
	public void execute(Consumer<A> command) {
		this.commands.add(command);
		if(!this.semaphore.tryAcquire()) {
			return;
		}
		
		do {
			try {
				Consumer<A> currentCommand;
				while( (currentCommand = this.commands.poll()) != null) {
					currentCommand.accept(this.target);
				}
			}
			finally {
				this.semaphore.release();
			}
		} while(!this.commands.isEmpty() && this.semaphore.tryAcquire());
	}
}

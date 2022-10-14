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
package io.inverno.mod.base.concurrent;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import reactor.core.scheduler.NonBlocking;

/**
 * <p>
 * A reactor defines the core threading model and more specifically it provides the main event loop used at the core of Inverno's applications.
 * </p>
 *
 * <p>
 * A reactor exposes a single acceptor event loop group used to accept connection and methods to create event loop groups backed by a unique core event loop group in order to optimize thread creation
 * and usage based on hardware and software capabilities.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface Reactor {
	
	/**
	 * <p>
	 * Returns the event loop associated to the current thread.
	 * </p>
	 * 
	 * @return an optional returning the current event loop or an empty optional if the current thread is not a reactor thread.
	 */
	Optional<EventLoop> eventLoop();
	
	/**
	 * <p>
	 * Returns the worker pool.
	 * </p>
	 * 
	 * <p>
	 * The worker pool is typically used to execute blocking task outside the I/O event loop.
	 * </p>
	 * 
	 * @return an executor service
	 */
	ExecutorService getWorkerPool();
	
	/**
	 * <p>
	 * Returns the acceptor event loop group typically with one thread.
	 * </p>
	 *
	 * <p>
	 * This event loop group should be shared across network servers to accept connections.
	 * </p>
	 *
	 * @return an acceptor event loop group
	 */
	EventLoopGroup getAcceptorEventLoopGroup();
	
	/**
	 * <p>
	 * Creates an IO event loop group with all available threads.
	 * </p>
	 * 
	 * @return an IO event loop group
	 */
	EventLoopGroup createIoEventLoopGroup();
	
	/**
	 * <p>
	 * Creates an IO event loop group with the specified amount of threads.
	 * </p>
	 *
	 * @param nThreads the number of threads to allocate
	 *
	 * @return an IO event loop group
	 * @throws IllegalArgumentException if the specified number of thread exceeds the number of threads available
	 */
	EventLoopGroup createIoEventLoopGroup(int nThreads) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Returns an event loop from the core IO event loop group.
	 * </p>
	 * 
	 * @return an event loop
	 */
	EventLoop getEventLoop();
	
	/**
	 * <p>
	 * Returns the size of the core IO event loop group.
	 * </p>
	 *
	 * <p>
	 * This basically the maximum number of threads one can allocate to an IO event loop group.
	 * </p>
	 *
	 * @return The number of threads allocated to the core IO event loop group
	 */
	int getCoreIoEventLoopGroupSize();
	
	/**
	 * <p>
	 * A marker interface that is detected on {@link Thread Threads} to indicate that a thread is part of a reactor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	interface Thread extends NonBlocking {
	
		/**
		 * <p>
		 * Returns the event loop associated to the reactor thread.
		 * </p>
		 * 
		 * @return a netty event loop
		 */
		EventLoop getEventLoop();
	} 
}

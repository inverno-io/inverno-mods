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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.net.NetService.TransportType;
import io.inverno.mod.boot.BootConfiguration;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * <p>
 * Generic {@link Reactor} implementation which instantiates a core IO event
 * loop groups based on the boot module configuration and hardware/software
 * capabilities.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class GenericReactor implements Reactor, InternalReactor {

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	private final BootConfiguration configuration;
	private final TransportType transportType;
	private final ExecutorService workerPool;
	
	private final int coreEventLoopGroupSize;
	
	private EventLoopGroup coreEventLoopGroup;
	private EventLoop[] coreEventLoops;
	
	private EventLoopGroupWrapper acceptorEventLoopGroup;
	private EventLoop acceptorEventLoop;
	
	/**
	 * <p>
	 * Creates a generic Reactor.
	 * </p>
	 * 
	 * @param configuration the boot module configuration
	 * @param transportType the transport type
	 * @param workerPool    the worker pool 
	 */
	public GenericReactor(BootConfiguration configuration, TransportType transportType, ExecutorService workerPool) {
		this.configuration = configuration;
		this.transportType = transportType;
		this.workerPool = workerPool;
		
		this.coreEventLoopGroupSize = this.configuration.reactor_event_loop_group_size();
	}
	
	/**
	 * <p>
	 * Creates an event loop group.
	 * </p>
	 * 
	 * @param nThreads      the size of the event loop group
	 * @param threadFactory the thread factory
	 * 
	 * @return an new event loop group
	 */
	private EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
		switch(this.transportType) {
			case KQUEUE: return new KQueueEventLoopGroup(nThreads, threadFactory);
			case EPOLL: return new EpollEventLoopGroup(nThreads, threadFactory);
			case IO_URING: return new IOUringEventLoopGroup(nThreads, threadFactory);
			default: return new NioEventLoopGroup(nThreads, threadFactory);
		}
	}
	
	@Override
	public void init() {
		this.logger.debug("Creating core event loop group ({}) with {} threads...", () -> this.transportType.toString().toLowerCase(), () -> this.coreEventLoopGroupSize);
		this.coreEventLoopGroup = this.createEventLoopGroup(this.coreEventLoopGroupSize, new ReactorThreadFactory(this, "inverno-io-" + this.transportType.toString().toLowerCase(), false, 5));
		this.coreEventLoops = new EventLoop[this.coreEventLoopGroupSize];
		this.logger.debug("Creating acceptor event loop group ({})...", () -> this.transportType.toString().toLowerCase());
		this.acceptorEventLoopGroup = new EventLoopGroupWrapper(this.createEventLoopGroup(1, new ReactorThreadFactory(this, "inverno-acceptor-" + this.transportType.toString().toLowerCase(), false, 5)));
		
		for(int i=0;i<this.coreEventLoopGroupSize;i++) {
			this.coreEventLoops[i] = this.coreEventLoopGroup.next();
		}
		this.acceptorEventLoop = this.acceptorEventLoopGroup.unwrap().next();
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void destroy() {
		this.logger.debug("Stopping acceptor event loop group...");
		this.acceptorEventLoopGroup.unwrap().shutdownGracefully(0, 15, TimeUnit.SECONDS).addListener(new GenericFutureListener() {
			@Override
			public void operationComplete(Future future) throws Exception {
				if(!future.isSuccess()) {
					GenericReactor.this.logger.warn("Error while stopping acceptor event loop group", future.cause());
				}
				GenericReactor.this.logger.debug("Stopping core event loop group...");
				GenericReactor.this.coreEventLoopGroup.shutdownGracefully(0, 15, TimeUnit.SECONDS).addListener(new GenericFutureListener() {
					@Override
					public void operationComplete(Future future) throws Exception {
						if(!future.isSuccess()) {
							GenericReactor.this.logger.warn("Error while stopping core IO event loop group", future.cause());
						}
					}
				});
			}
		});
	}

	@Override
	public Optional<EventLoop> eventLoop() {
		java.lang.Thread currentThread = java.lang.Thread.currentThread();
		if(currentThread instanceof Reactor.Thread) {
			return Optional.of(((Reactor.Thread)currentThread).getEventLoop());
		}
		return Optional.empty();
	}
	
	@Override
	public EventLoop eventLoop(java.lang.Thread thread) {
		for(EventLoop eventLoop : this.coreEventLoops) {
			if(eventLoop.inEventLoop(thread)) {
				return eventLoop;
			}
		}
		
		if(this.acceptorEventLoop.inEventLoop(thread)) {
			return this.acceptorEventLoop;
		}
		return null;
	}
	
	@Override
	public ExecutorService getWorkerPool() {
		return this.workerPool;
	}
	
	@Override
	public EventLoopGroup getAcceptorEventLoopGroup() {
		return this.acceptorEventLoopGroup;
	}

	@Override
	public EventLoopGroup createIoEventLoopGroup() {
		return this.createIoEventLoopGroup(this.coreEventLoopGroupSize);
	}

	@Override
	public EventLoopGroup createIoEventLoopGroup(int nThreads) throws IllegalArgumentException {
		if(nThreads > this.coreEventLoopGroupSize) {
			throw new IllegalArgumentException("Number of threads: " + nThreads + " exceeds core event loop group size: " + this.coreEventLoopGroupSize);
		}
		return new EventLoopGroupProxy(nThreads, this.coreEventLoopGroup);
	}

	@Override
	public EventLoop getEventLoop() {
		return new EventLoopWrapper(this.coreEventLoopGroup.next());
	}

	@Override
	public int getCoreIoEventLoopGroupSize() {
		return this.coreEventLoopGroupSize;
	}
}

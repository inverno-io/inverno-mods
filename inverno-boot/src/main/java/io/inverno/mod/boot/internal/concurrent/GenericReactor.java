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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

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
public class GenericReactor implements Reactor, ReactorLifecycle {

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	private final BootConfiguration configuration;
	private final TransportType transportType;
	
	private final int coreEventLoopGroupSize;
	
	private EventLoopGroup acceptorEventLoopGroup;
	private EventLoopGroup coreEventLoopGroup;
	
	/**
	 * <p>
	 * Creates a generic Reactor.
	 * </p>
	 * 
	 * @param configuration the boot module configuration
	 * @param transportType the transport type
	 */
	public GenericReactor(BootConfiguration configuration, TransportType transportType) {
		this.configuration = configuration;
		this.transportType = transportType;
		
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
		this.logger.debug("Creating acceptor event loop group ({})...", () -> this.transportType.toString().toLowerCase());
		this.acceptorEventLoopGroup = this.createEventLoopGroup(1, new ReactorThreadFactory("inverno-acceptor-" + this.transportType.toString().toLowerCase(), false, 5));
		this.logger.debug("Creating core event loop group ({}) with {} threads...", () -> this.transportType.toString().toLowerCase(), () -> this.coreEventLoopGroupSize);
		this.coreEventLoopGroup = this.createEventLoopGroup(this.coreEventLoopGroupSize, new ReactorThreadFactory("inverno-io-" + this.transportType.toString().toLowerCase(), false, 5));
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void destroy() {
		this.logger.debug("Stopping acceptor event loop group...");
		this.acceptorEventLoopGroup.shutdownGracefully(0, 15, TimeUnit.SECONDS).addListener(new GenericFutureListener() {
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
	public int getCoreIoEventLoopGroupSize() {
		return this.coreEventLoopGroupSize;
	}
}

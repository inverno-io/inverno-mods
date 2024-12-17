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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.concurrent.VertxReactor;
import io.inverno.mod.base.net.NetService.TransportType;
import io.inverno.mod.boot.BootConfiguration;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.impl.VertxImpl;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * <p>
 * A {@link Reactor} implementation backed by a {@link Vertx} instance.
 * </p>
 * 
 * <p>
 * This implementation basically makes it possible to integrate with Vert.x services more efficiently by using the Vert.x event loop groups as reactor core IO event loop group.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericVertxReactor implements VertxReactor, InternalReactor {

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	private final BootConfiguration configuration;
	private final TransportType transportType;
	private final ExecutorService workerPool;
	
	private final int coreEventLoopGroupSize;
	
	private Vertx vertx;
	
	private EventLoopGroupWrapper acceptorEventLoopGroup;
	private EventLoop[] coreEventLoops;
	
	private EventLoopGroup coreEventLoopGroup;
	private EventLoop acceptorEventLoop;
	
	/**
	 * <p>Creates a Vert.x reactor.</p>
	 * 
	 * @param configuration the boot module configuration
	 * @param transportType the transport type
	 * @param workerPool    the worker pool
	 */
	public GenericVertxReactor(BootConfiguration configuration, TransportType transportType, ExecutorService workerPool) {
		this.configuration = configuration;
		this.transportType = transportType;
		this.workerPool = workerPool;
		
		this.coreEventLoopGroupSize = this.configuration.reactor_event_loop_group_size();
	}
	
	@Override
	public void init() {
		this.logger.debug("Starting Vert.x...");
		
		VertxOptions options = new VertxOptions()
			.setPreferNativeTransport(this.configuration.prefer_native_transport())
			.setEventLoopPoolSize(this.configuration.reactor_event_loop_group_size());

		this.vertx = new VertxBuilder(options).threadFactory(new ReactorVertxThreadFactory(this)).init().vertx();

		// This is kind of dangerous if Vert.x decide to make VertxImpl private we might be in trouble... 
		this.coreEventLoopGroup = ((VertxImpl)this.vertx).nettyEventLoopGroup();
		this.acceptorEventLoopGroup = new EventLoopGroupWrapper(((VertxImpl)this.vertx).getAcceptorEventLoopGroup());
		
		this.coreEventLoops = new EventLoop[this.coreEventLoopGroupSize];
		for(int i=0;i<this.coreEventLoopGroupSize;i++) {
			this.coreEventLoops[i] = this.coreEventLoopGroup.next();
		}
		this.acceptorEventLoop = this.acceptorEventLoopGroup.unwrap().next();
	}
	
	@Override
	public void destroy() {
		this.logger.debug("Stopping Vert.x...");
		
		this.vertx.close(result -> {
			if(!result.succeeded()) {
				GenericVertxReactor.this.logger.warn("Error while stopping Vertx", result.cause());
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
	public Vertx getVertx() {
		return this.vertx;
	}
	
	@Override
	public int getCoreIoEventLoopGroupSize() {
		return this.coreEventLoopGroupSize;
	}
}

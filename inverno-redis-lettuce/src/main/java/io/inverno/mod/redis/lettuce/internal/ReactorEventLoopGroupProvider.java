/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
 *
 * @author jkuhn
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

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

package io.inverno.mod.boot.internal.concurrent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * Event loop group wrapper that prevents internal event loop groups from being shutdown.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class EventLoopGroupWrapper implements EventLoopGroup {

	protected final EventLoopGroup eventLoopGroup;

	public EventLoopGroupWrapper(EventLoopGroup eventLoopGroup) {
		this.eventLoopGroup = eventLoopGroup;
	}
	
	public EventLoopGroup unwrap() {
		return this.eventLoopGroup;
	}
	
	@Override
	public EventLoop next() {
		return this.eventLoopGroup.next();
	}

	@Override
	public ChannelFuture register(Channel channel) {
		return this.eventLoopGroup.register(channel);
	}

	@Override
	public ChannelFuture register(ChannelPromise promise) {
		return this.eventLoopGroup.register(promise);
	}

	@Override
	@Deprecated
	public ChannelFuture register(Channel channel, ChannelPromise promise) {
		return this.eventLoopGroup.register(channel, promise);
	}

	@Override
	public boolean isShuttingDown() {
		return this.eventLoopGroup.isShuttingDown();
	}

	@Override
	public Future<?> shutdownGracefully() {
		return new DefaultPromise<>(GlobalEventExecutor.INSTANCE).setSuccess(null);
	}

	@Override
	public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
		return new DefaultPromise<>(GlobalEventExecutor.INSTANCE).setSuccess(null);
	}

	@Override
	public Future<?> terminationFuture() {
		return this.eventLoopGroup.terminationFuture();
	}

	@Override
	@Deprecated
	public void shutdown() {
	}

	@Override
	@Deprecated
	public List<Runnable> shutdownNow() {
		return List.of();
	}

	@Override
	public Iterator<EventExecutor> iterator() {
		return this.eventLoopGroup.iterator();
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.eventLoopGroup.submit(task);
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return this.eventLoopGroup.submit(task, result);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.eventLoopGroup.submit(task);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return this.eventLoopGroup.schedule(command, delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return this.eventLoopGroup.schedule(callable, delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return this.eventLoopGroup.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return this.eventLoopGroup.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

	@Override
	public boolean isShutdown() {
		return this.eventLoopGroup.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.eventLoopGroup.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return this.eventLoopGroup.awaitTermination(timeout, unit);
	}

	@Override
	public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return this.eventLoopGroup.invokeAll(tasks);
	}

	@Override
	public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		return this.eventLoopGroup.invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return this.eventLoopGroup.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.eventLoopGroup.invokeAny(tasks, timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		this.eventLoopGroup.execute(command);
	}
}

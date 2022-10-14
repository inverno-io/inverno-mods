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

import io.inverno.mod.base.concurrent.Reactor;
import io.netty.channel.EventLoop;
import io.vertx.core.impl.VertxThread;
import java.util.concurrent.TimeUnit;
import reactor.core.scheduler.NonBlocking;

/**
 * <p>
 * A {@link NonBlocking} Vertx thread implementation which prevents blocking
 * calls from the Reactor APIs.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see NonBlocking
 * @see ReactorVertxThreadFactory
 */
class ReactorVertxThread extends VertxThread implements Reactor.Thread {

	private final InternalReactor reactor;
	
	private EventLoop eventLoop;
	
	public ReactorVertxThread(InternalReactor reactor, Runnable target, String name, boolean worker, long maxExecTime, TimeUnit maxExecTimeUnit) {
		super(target, name, worker, maxExecTime, maxExecTimeUnit);
		this.reactor = reactor;
	}

	@Override
	public EventLoop getEventLoop() {
		if(this.eventLoop == null) {
			this.eventLoop = this.reactor.eventLoop(this);
		}
		return this.eventLoop;
	}
}

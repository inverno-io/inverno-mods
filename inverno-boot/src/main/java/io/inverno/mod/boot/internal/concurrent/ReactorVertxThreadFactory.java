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

import java.util.concurrent.TimeUnit;

import io.vertx.core.impl.VertxThread;
import io.vertx.core.spi.VertxThreadFactory;

/**
 * <p>
 * {@link VertxThreadFactory} implementation that creates {@link reactor.core.scheduler.NonBlocking NonBlocking} Vertx threads in order to prevent blocking calls from the Reactor APIs.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see reactor.core.scheduler.NonBlocking NonBlocking
 */
class ReactorVertxThreadFactory implements VertxThreadFactory {

	private final InternalReactor reactor;

	public ReactorVertxThreadFactory(InternalReactor reactor) {
		this.reactor = reactor;
	}
	
	@Override
	public VertxThread newVertxThread(Runnable target, String name, boolean worker, long maxExecTime, TimeUnit maxExecTimeUnit) {
		return new ReactorVertxThread(this.reactor, target, name, worker, maxExecTime, maxExecTimeUnit);
	}
}

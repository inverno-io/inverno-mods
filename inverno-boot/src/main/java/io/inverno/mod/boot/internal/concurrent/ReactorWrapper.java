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

import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.concurrent.VertxReactor;
import io.inverno.mod.base.net.NetService.TransportType;
import io.inverno.mod.boot.BootConfiguration;

/**
 * <p>
 * Creates and exposes the {@link Reactor} implementation based on the boot
 * module configuration.
 * </p>
 * 
 * <p>
 * If Vert.x core is present in the module path and
 * {@link BootConfiguration#reactor_prefer_vertx()} is set to true, a
 * {@link VertxReactor} is exposed, otherwise a regular {@link Reactor} is
 * exposed
 * </p>
 * 
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
@Wrapper @Bean(name = "reactor")
public class ReactorWrapper implements Supplier<Reactor> {

	private final BootConfiguration configuration;
	
	private final ReactorLifecycle reactor;
	
	public ReactorWrapper(BootConfiguration configuration, TransportType transportType) {
		this.configuration = configuration;
		if(this.isVertxAvailable()) {
			this.reactor = new GenericVertxReactor(this.configuration, transportType);
		}
		else {
			this.reactor = new GenericReactor(this.configuration, transportType);
		}
	}

	private boolean isVertxAvailable() {
		if(this.configuration.reactor_prefer_vertx()) {
			try {
				Class.forName("io.vertx.core.Vertx");
				return true;
			}
			catch(Throwable t) {
				return false;
			}
		}
		return false;
	}
	
	@Override
	public Reactor get() {
		return this.reactor;
	}
	
	@Init
	public void init() {
		this.reactor.init();
	}
	
	@Destroy
	public void destroy() {
		this.reactor.destroy();
	}
}

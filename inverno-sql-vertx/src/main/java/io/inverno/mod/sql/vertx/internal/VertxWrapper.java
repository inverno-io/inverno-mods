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
package io.inverno.mod.sql.vertx.internal;

import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.concurrent.VertxReactor;
import io.vertx.core.Vertx;

/**
 * <p>
 * A Vert.x instance wrapper bean which uses the {@link VertxReactor} instance
 * when available or creates a new one in order to create Vert.x SQL clients and
 * pools.
 * </p>
 * 
 * <p>
 * This bean is overridable and as a result it is also possible to provide a
 * custom instance on the module.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
@Overridable @Wrapper @Bean( name = "vertx" )
public class VertxWrapper implements Supplier<Vertx> {

	private final Reactor reactor;
	
	private Vertx vertx;
	
	/**
	 * <p>
	 * Creates A Vert.x wrapper.
	 * </p>
	 * 
	 * @param reactor the reactor
	 */
	public VertxWrapper(Reactor reactor) {
		this.reactor = reactor;
	}
	
	@Init
	public void init() {
		if(this.reactor instanceof VertxReactor) {
			this.vertx = ((VertxReactor)this.reactor).getVertx();
		}
		else {
			this.vertx = Vertx.vertx();
		}
	}
	
	@Destroy
	public void destroy() {
		if(!(this.reactor instanceof VertxReactor)) {
			this.vertx.close();
		}
	}

	@Override
	public Vertx get() {
		return this.vertx;
	}
}

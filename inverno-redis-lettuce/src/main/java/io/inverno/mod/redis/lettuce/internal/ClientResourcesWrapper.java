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
package io.inverno.mod.redis.lettuce.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.lettuce.core.resource.ClientResources;
import java.util.function.Supplier;
import io.inverno.mod.redis.lettuce.LettuceRedisClientConfiguration;

/**
 * <p>
 * Lettuce client resources wrapper bean that allows to use Inverno's reactor as event loop group provider.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
@Wrapper @Bean( name = "clientResources", visibility = Bean.Visibility.PRIVATE)
public class ClientResourcesWrapper implements Supplier<ClientResources> {

	private final LettuceRedisClientConfiguration configuration;
	private Reactor reactor;

	private ClientResources instance;
	
	/**
	 * <p>
	 * Creates a Client resources wrapper
	 * </p>
	 *
	 * @param configuration Lettuce redis client module configuration
	 */
	public ClientResourcesWrapper(LettuceRedisClientConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * <p>
	 * Sets Inverno's reactor in order to use Inverno's reactor as event loop group provider.
	 * </p>
	 * 
	 * @param reactor Inverno's reactor
	 */
	public void setReactor(Reactor reactor) {
		this.reactor = reactor;
	}
	
	@Override
	public ClientResources get() {
		return this.instance;
	}
	
	@Init
	public void init() {
		if(this.reactor != null) {
			this.instance = ClientResources.builder().eventLoopGroupProvider(new ReactorEventLoopGroupProvider(this.reactor, this.configuration.event_loop_group_size())).build();
		}
		else {
			this.instance = ClientResources.builder().ioThreadPoolSize(this.configuration.event_loop_group_size()).build();
		}
	}
	
	@Destroy
	public void destroy() {
		this.instance.shutdown();
	}
}

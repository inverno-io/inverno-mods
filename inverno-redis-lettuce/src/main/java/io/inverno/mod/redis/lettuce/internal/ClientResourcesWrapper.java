/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.lettuce.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.redis.lettuce.RedisClientConfiguration;
import io.lettuce.core.resource.ClientResources;
import java.util.function.Supplier;

/**
 *
 * @author jkuhn
 */
@Wrapper @Bean( name = "clientResources", visibility = Bean.Visibility.PRIVATE)
public class ClientResourcesWrapper implements Supplier<ClientResources> {

	private final RedisClientConfiguration configuration;
	private final Reactor reactor;
	
	private ClientResources instance;
	
	public ClientResourcesWrapper(RedisClientConfiguration configuration, Reactor reactor) {
		this.configuration = configuration;
		this.reactor = reactor;
	}

	@Override
	public ClientResources get() {
		return this.instance;
	}
	
	@Init
	public void init() {
		this.instance = ClientResources.builder().eventLoopGroupProvider(new ReactorEventLoopGroupProvider(this.reactor, this.configuration.event_loop_group_size())).build();
	}
	
	@Destroy
	public void destroy() {
		this.instance.shutdown();
	}
}

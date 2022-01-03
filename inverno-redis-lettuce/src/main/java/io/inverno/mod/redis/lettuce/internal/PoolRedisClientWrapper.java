/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.lettuce.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.redis.lettuce.PoolRedisClient;
import io.inverno.mod.redis.lettuce.RedisClientConfiguration;
import io.inverno.mod.redis.lettuce.RedisTransactionalClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import java.time.Duration;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jkuhn
 */
@Wrapper @Bean( name = "redisClient" )
public class PoolRedisClientWrapper implements Supplier<RedisTransactionalClient<String, String>> {

	private final RedisClientConfiguration configuration;
	private final ClientResources clientResources;
	
	private io.lettuce.core.RedisClient	client;
	private PoolRedisClient instance;
	
	public PoolRedisClientWrapper(RedisClientConfiguration configuration, ClientResources clientResources) {
		this.configuration = configuration;
		this.clientResources = clientResources;
	}
	
	@Override
	public RedisTransactionalClient<String, String> get() {
		return this.instance;
	}
	
	@Init
	public void init() {
		this.client = io.lettuce.core.RedisClient.create(this.clientResources);

		RedisURI uri;
		if(StringUtils.isNotBlank(this.configuration.uri())) {
			uri = RedisURI.create(this.configuration.uri());
		}
		else {
			RedisURI.Builder uriBuilder = RedisURI.Builder
				.redis(this.configuration.host(), this.configuration.port())
				.withSsl(this.configuration.ssl());
			
			if(StringUtils.isNotBlank(this.configuration.username()) && StringUtils.isNotBlank(this.configuration.password())) {
				uriBuilder = uriBuilder.withAuthentication(this.configuration.username(), this.configuration.password());
			}
			
			uri = uriBuilder.build();
		}
		uri.setDatabase(this.configuration.database());
		uri.setTimeout(Duration.ofMillis(this.configuration.timeout()));
		if(StringUtils.isNotBlank(this.configuration.client_name())) {
			uri.setClientName(this.configuration.client_name());
		}
		
		BoundedPoolConfig poolConfig = BoundedPoolConfig.builder()
			.minIdle(this.configuration.pool_min_idle())
			.maxIdle(this.configuration.pool_max_idle())
			.maxTotal(this.configuration.pool_max_active())
			.build();
		
		BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
			() -> this.client.connectAsync(StringCodec.UTF8, uri), 
			poolConfig
		);
		
		this.instance = new PoolRedisClient<>(pool);
	}
	
	@Destroy
	public void destroy() {
		this.client.shutdown();
	}
}

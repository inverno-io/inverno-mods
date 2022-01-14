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
import io.inverno.mod.redis.RedisTransactionalClient;
import io.inverno.mod.redis.lettuce.PoolRedisClient;
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
import io.inverno.mod.redis.lettuce.LettuceRedisClientConfiguration;

/**
 * <p>
 * Lettuce Pool Redis client wrapper bean.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
@Wrapper @Bean( name = "lettuceRedisClient" )
public class PoolRedisClientWrapper implements Supplier<RedisTransactionalClient<String, String>> {

	private final LettuceRedisClientConfiguration configuration;
	private final ClientResources clientResources;
	
	private io.lettuce.core.RedisClient	client;
	private PoolRedisClient<String, String, ?> instance;
	
	/**
	 * <p>
	 * Creates a Pool Redis client with the specified configuration and Lettuce client resources.
	 * </p>
	 *
	 * @param configuration   Lettuce redis client module configuration
	 * @param clientResources Lettuce client resources
	 */
	public PoolRedisClientWrapper(LettuceRedisClientConfiguration configuration, ClientResources clientResources) {
		this.configuration = configuration;
		this.clientResources = clientResources;
	}
	
	@Override
	public RedisTransactionalClient<String, String> get() {
		return this.instance;
	}
	
	/**
	 * Initializes the client
	 */
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
				.withSsl(this.configuration.tls());
			
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
		this.instance = new PoolRedisClient<>(pool, String.class, String.class);
	}
	
	/**
	 * Destroys the client
	 */
	@Destroy
	public void destroy() {
		this.client.shutdown();
	}
}

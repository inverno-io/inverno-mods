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
package io.inverno.mod.redis.lettuce.internal.operations;

import io.inverno.mod.redis.operations.RedisGeoReactiveOperations;
import io.lettuce.core.GeoAddArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class GeoaddBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisGeoReactiveOperations.GeoaddBuilder<A, B> {

	private final RedisGeoReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private short nxxxch;
	
	/**
	 * 
	 * @param commands 
	 */
	public GeoaddBuilderImpl(RedisGeoReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public GeoaddBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	@Override
	public GeoaddBuilderImpl<A, B, C> nx() {
		this.nxxxch = 1;
		return this;
	}

	@Override
	public GeoaddBuilderImpl<A, B, C> xx() {
		this.nxxxch = 2;
		return this;
	}

	@Override
	public GeoaddBuilderImpl<A, B, C> ch() {
		this.nxxxch = 3;
		return this;
	}

	@Override
	public Mono<Long> build(A key, double longitude, double latitude, B member) {
		if(this.commands != null) {
			return this.build(this.commands, key, longitude, latitude, member);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, longitude, latitude, member), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param longitude
	 * @param latitude
	 * @param member
	 * @return 
	 */
	private Mono<Long> build(RedisGeoReactiveCommands<A, B> localCommands, A key, double longitude, double latitude, B member) {
		switch(this.nxxxch) {
			case 1:
				return localCommands.geoadd(key, longitude, latitude, member, GeoAddArgs.Builder.nx());
			case 2:
				return localCommands.geoadd(key, longitude, latitude, member, GeoAddArgs.Builder.xx());
			case 3:
				return localCommands.geoadd(key, longitude, latitude, member, GeoAddArgs.Builder.ch());
			default:
				return localCommands.geoadd(key, longitude, latitude, member);
		}
	}

	@Override
	public Mono<Long> build(A key, Consumer<RedisGeoReactiveOperations.GeoItems<B>> items) {
		if(this.commands != null) {
			return this.build(this.commands, key, items);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, items), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param items
	 * @return 
	 */
	private Mono<Long> build(RedisGeoReactiveCommands<A, B> localCommands, A key, Consumer<RedisGeoReactiveOperations.GeoItems<B>> items) {
		GeoItemsImpl<B> itemsConfigurator = new GeoItemsImpl<>();
		items.accept(itemsConfigurator);
		
		switch(this.nxxxch) {
			case 1:
				return localCommands.geoadd(key, GeoAddArgs.Builder.nx(), itemsConfigurator.getValues());
			case 2:
				return localCommands.geoadd(key, GeoAddArgs.Builder.xx(), itemsConfigurator.getValues());
			case 3:
				return localCommands.geoadd(key, GeoAddArgs.Builder.ch(), itemsConfigurator.getValues());
			default:
				return localCommands.geoadd(key, itemsConfigurator.getValues());
		}
	}
}

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
import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
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
public class GeoradiusStoreBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisGeoReactiveOperations.GeoradiusStoreBuilder<A, B> {

	private final RedisGeoReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;

	private Long count;
	private boolean any;
	private boolean asc;
	private boolean desc;
	
	private boolean storeDist;

	/**
	 * 
	 * @param commands 
	 */
	public GeoradiusStoreBuilderImpl(RedisGeoReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public GeoradiusStoreBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	@Override
	public GeoradiusStoreBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		this.any = false;
		return this;
	}

	@Override
	public GeoradiusStoreBuilderImpl<A, B, C> countany(long count) {
		throw new UnsupportedOperationException("Implementation doesn't support STORE with COUNT ANY");
	}

	@Override
	public GeoradiusStoreBuilderImpl<A, B, C> asc() {
		this.asc = true;
		this.desc = false;
		return this;
	}

	@Override
	public GeoradiusStoreBuilderImpl<A, B, C> desc() {
		this.asc = false;
		this.desc = true;
		return this;
	}

	@Override
	public GeoradiusStoreBuilderImpl<A, B, C> storedist() {
		this.storeDist = true;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	protected GeoRadiusStoreArgs<A> buildGeoStoreArgs(A storeKey) {
		GeoRadiusStoreArgs<A> geoStoreArgs;
		if(this.storeDist) {
			geoStoreArgs = GeoRadiusStoreArgs.Builder.withStoreDist(storeKey);
		}
		else {
			geoStoreArgs = GeoRadiusStoreArgs.Builder.store(storeKey);
		}
		
		if(this.count != null) {
			geoStoreArgs.withCount(this.count);
		}
		
		if(this.asc) {
			geoStoreArgs.sort(GeoArgs.Sort.asc);
		}
		else if(this.desc) {
			geoStoreArgs.sort(GeoArgs.Sort.desc);
		}
		return geoStoreArgs;
	}
	
	@Override
	public Mono<Long> build(A source, A destination, double longitude, double latitude, double radius, RedisGeoReactiveOperations.GeoUnit unit) {
		if(this.commands != null) {
			return this.build(this.commands, source, destination, longitude, latitude, radius, unit);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), source, destination, longitude, latitude, radius, unit), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param source
	 * @param destination
	 * @param longitude
	 * @param latitude
	 * @param radius
	 * @param unit
	 * @return 
	 */
	private Mono<Long> build(RedisGeoReactiveCommands<A, B> localCommands, A source, A destination, double longitude, double latitude, double radius, RedisGeoReactiveOperations.GeoUnit unit) {
		return localCommands.georadius(source, longitude, latitude, radius, GeoUtils.convertUnit(unit), this.buildGeoStoreArgs(destination));
	}
}

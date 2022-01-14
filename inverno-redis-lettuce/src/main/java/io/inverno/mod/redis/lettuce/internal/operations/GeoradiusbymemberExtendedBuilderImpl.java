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
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
import reactor.core.publisher.Flux;
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
public class GeoradiusbymemberExtendedBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisGeoReactiveOperations.GeoradiusbymemberExtendedBuilder<A, B> {

	private final RedisGeoReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;

	private Long count;
	private boolean any;
	private boolean asc;
	private boolean desc;
	
	private boolean coord;
	private boolean dist;
	private boolean hash;

	public GeoradiusbymemberExtendedBuilderImpl(RedisGeoReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	public GeoradiusbymemberExtendedBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	@Override
	public GeoradiusbymemberExtendedBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		this.any = false;
		return this;
	}

	@Override
	public GeoradiusbymemberExtendedBuilderImpl<A, B, C> countany(long count) {
		this.count = count;
		this.any = true;
		return this;
	}

	@Override
	public GeoradiusbymemberExtendedBuilderImpl<A, B, C> asc() {
		this.asc = true;
		this.desc = false;
		return this;
	}

	@Override
	public GeoradiusbymemberExtendedBuilderImpl<A, B, C> desc() {
		this.asc = false;
		this.desc = true;
		return this;
	}
	
	@Override
	public GeoradiusbymemberExtendedBuilderImpl<A, B, C> withcoord() {
		this.coord = true;
		return this;
	}

	@Override
	public GeoradiusbymemberExtendedBuilderImpl<A, B, C> withdist() {
		this.dist = true;
		return this;
	}

	@Override
	public GeoradiusbymemberExtendedBuilderImpl<A, B, C> withhash() {
		this.hash = true;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected GeoArgs buildGeoArgs() {
		GeoArgs geoArgs = new GeoArgs();
		if(this.count != null) {
			geoArgs.withCount(this.count, this.any);
		}

		if(this.asc) {
			geoArgs.sort(GeoArgs.Sort.asc);
		}
		else if(this.desc) {
			geoArgs.sort(GeoArgs.Sort.desc);
		}
		
		if(this.coord) {
			geoArgs.withCoordinates();
		}
		if(this.dist) {
			geoArgs.withDistance();
		}
		if(this.hash) {
			geoArgs.withHash();
		}
		return geoArgs;
	}

	@Override
	public Flux<RedisGeoReactiveOperations.GeoWithin<B>> build(A key, B member, double radius, RedisGeoReactiveOperations.GeoUnit unit) {
		if(this.commands != null) {
			return this.build(this.commands, key, member, radius, unit);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, member, radius, unit), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param member
	 * @param radius
	 * @param unit
	 * @return 
	 */
	private Flux<RedisGeoReactiveOperations.GeoWithin<B>> build(RedisGeoReactiveCommands<A, B> localCommands, A key, B member, double radius, RedisGeoReactiveOperations.GeoUnit unit) {
		return localCommands.georadiusbymember(key, member, radius, GeoUtils.convertUnit(unit), this.buildGeoArgs()).map(GeoUtils::convertGeoWithin);
	}
}
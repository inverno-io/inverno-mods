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
import io.lettuce.core.GeoSearch;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
import io.lettuce.core.protocol.CommandArgs;
import java.util.Objects;
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
public class GeosearchBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisGeoReactiveOperations.GeosearchBuilder<A, B> {

	private final RedisGeoReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private B fromMember;
	private Double fromCoordinatesLongitude;
	private Double fromCoordinatesLatitude;
	private Double byRadius;
	private RedisGeoReactiveOperations.GeoUnit byRadiusUnit;
	private Double byBoxWidth;
	private Double byBoxHeight;
	private RedisGeoReactiveOperations.GeoUnit byBoxUnit;
	private boolean asc;
	private boolean desc;
	private Long count;
	private boolean any;
	
	/**
	 * 
	 * @param commands 
	 */
	public GeosearchBuilderImpl(RedisGeoReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public GeosearchBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> fromMember(B member) {
		Objects.requireNonNull(member, "member");
		this.fromMember = member;
		this.fromCoordinatesLongitude = null;
		this.fromCoordinatesLatitude = null;
		return this;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> fromCoordinates(double longitude, double latitude) {
		this.fromMember = null;
		this.fromCoordinatesLongitude = longitude;
		this.fromCoordinatesLatitude = latitude;
		return this;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> byRadius(double radius, RedisGeoReactiveOperations.GeoUnit unit) {
		Objects.requireNonNull(unit, "unit");
		this.byRadius = radius;
		this.byRadiusUnit = unit;
		this.byBoxWidth = null;
		this.byBoxHeight = null;
		this.byBoxUnit = null;
		return this;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> byBox(double width, double height, RedisGeoReactiveOperations.GeoUnit unit) {
		Objects.requireNonNull(unit, "unit");
		this.byRadius = null;
		this.byRadiusUnit = null;
		this.byBoxWidth = width;
		this.byBoxHeight = height;
		this.byBoxUnit = unit;
		return this;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> asc() {
		this.asc = true;
		this.desc = false;
		return this;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> desc() {
		this.asc = false;
		this.desc = true;
		return this;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		this.any = false;
		return this;
	}

	@Override
	public GeosearchBuilderImpl<A, B, C> countany(long count) {
		this.count = count;
		this.any = true;
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
		return geoArgs;
	}
	
	@Override
	public Flux<B> build(A key) {
		if(this.commands != null) {
			return this.build(this.commands, key);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @return 
	 */
	private Flux<B> build(RedisGeoReactiveCommands<A, B> localCommands, A key) {
		GeoSearch.GeoRef<A> geoRef;
		if(this.fromMember != null) {
			// TODO there seems to be a bug in Lettuce, Georef FromMember is currently targeting the key and not the value (ie. member) which is expected by Redis
			geoRef = new GeoSearch.GeoRef<A>() {
				@Override
				@SuppressWarnings("unchecked")
				public <K, V> void build(CommandArgs<K, V> args) {
					args.add("FROMMEMBER").addValue((V) fromMember);
				}
			};
		}
		else if(this.fromCoordinatesLongitude != null) {
			geoRef = GeoSearch.fromCoordinates(this.fromCoordinatesLongitude, this.fromCoordinatesLatitude);
		}
		else {
			throw new IllegalArgumentException("Missing center point: member or coordinates");
		}
		
		GeoSearch.GeoPredicate geoPredicate;
		if(this.byRadius != null) {
			geoPredicate = GeoSearch.byRadius(this.byRadius, GeoUtils.convertUnit(this.byRadiusUnit));
		}
		else if(this.byBoxWidth != null) {
			geoPredicate = GeoSearch.byBox(this.byBoxWidth, this.byBoxHeight, GeoUtils.convertUnit(this.byBoxUnit));
		}
		else {
			throw new IllegalArgumentException("Missing shape: radius or box");
		}
		return localCommands.geosearch(key, geoRef, geoPredicate, this.buildGeoArgs()).map(GeoWithin::getMember);
	}
}

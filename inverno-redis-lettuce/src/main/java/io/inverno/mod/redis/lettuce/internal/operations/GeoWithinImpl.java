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
import java.util.Optional;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 */
public class GeoWithinImpl<A> implements RedisGeoReactiveOperations.GeoWithin<A> {

	private final Optional<Double> distance;
	private final Optional<Long> hash;
	private final Optional<RedisGeoReactiveOperations.GeoCoordinates> coordinates;
	private final A member;
	
	/**
	 * 
	 * @param distance
	 * @param hash
	 * @param coordinates
	 * @param member 
	 */
	public GeoWithinImpl(Double distance, Long hash, RedisGeoReactiveOperations.GeoCoordinates coordinates, A member) {
		this.distance = Optional.ofNullable(distance);
		this.hash = Optional.ofNullable(hash);
		this.coordinates = Optional.ofNullable(coordinates);
		this.member = member;
	}
	
	@Override
	public Optional<Double> getDistance() {
		return this.distance;
	}

	@Override
	public Optional<Long> getHash() {
		return this.hash;
	}

	@Override
	public Optional<RedisGeoReactiveOperations.GeoCoordinates> getCoordinates() {
		return this.coordinates;
	}

	@Override
	public A getMember() {
		return this.member;
	}
}

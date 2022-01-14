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
import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoWithin;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public final class GeoUtils {

	private GeoUtils() {}
	
	/**
	 * <p>
	 * Converts geo unit to Lettuce geo unit
	 * </p>
	 * 
	 * @param unit
	 * @return 
	 */
	public static GeoArgs.Unit convertUnit(RedisGeoReactiveOperations.GeoUnit unit) {
		if(unit == null) {
			return null;
		}
		switch(unit) {
			case m: return GeoArgs.Unit.m;
			case km: return GeoArgs.Unit.km;
			case ft: return GeoArgs.Unit.ft;
			case mi: return GeoArgs.Unit.mi;
			default: throw new IllegalStateException("Unsupported unit: " + unit);
		}
	}
	
	/**
	 * <p>
	 * Converts geo coordinates to Lettuce geo coordinates.
	 * </p>
	 * 
	 * @param coordinates
	 * @return 
	 */
	public static RedisGeoReactiveOperations.GeoCoordinates convertCoordinates(GeoCoordinates coordinates) {
		if(coordinates == null) {
			return null;
		}
		return new GeoCoordinatesImpl(coordinates.getX().doubleValue(), coordinates.getY().doubleValue());
	}
	
	/**
	 * <p>
	 * Converts geo within to Lettuce geo within.
	 * </p>
	 * 
	 * @param <B>
	 * @param geoWithin
	 * @return 
	 */
	public static <B> RedisGeoReactiveOperations.GeoWithin<B> convertGeoWithin(GeoWithin<B> geoWithin) {
		return new GeoWithinImpl<>(geoWithin.getDistance(), geoWithin.getGeohash(), GeoUtils.convertCoordinates(geoWithin.getCoordinates()), geoWithin.getMember());
	}
}

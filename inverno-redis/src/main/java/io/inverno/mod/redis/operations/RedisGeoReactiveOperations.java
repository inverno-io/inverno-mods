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
package io.inverno.mod.redis.operations;

import java.util.Optional;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Geo reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisGeoReactiveOperations<A, B> {
	
	/**
	 * <a href="https://redis.io/commands/geoadd">GEOADD</a> key longitude latitude member
	 * 
	 * @param key
	 * @param longitude
	 * @param latitude
	 * @param member
	 * @return 
	 */
	Mono<Long> geoadd(A key, double longitude, double latitude, B member);
	
	/**
	 * <a href="https://redis.io/commands/geoadd">GEOADD</a> key longitude latitude member [longitude latitude member ...] 
	 * 
	 * @param key
	 * @param items
	 * @return 
	 */
	Mono<Long> geoadd(A key, Consumer<GeoItems<B>> items);
	
	/**
	 * <a href="https://redis.io/commands/geoadd">GEOADD</a> key [NX|XX] [CH] longitude latitude member [longitude latitude member ...] 
	 * 
	 * @return 
	 */
	GeoaddBuilder<A, B> geoadd();
	
	/**
	 * <a href="https://redis.io/commands/geodist">GEODIST</a> key member1 member2 [m|km|ft|mi] 
	 * 
	 * @param key
	 * @param member1
	 * @param member2
	 * @param unit
	 * @return 
	 */
	Mono<Double> geodist(A key, B member1, B member2, GeoUnit unit);
	
	/**
	 * <a href="https://redis.io/commands/geohash">GEOHASH</a> key member
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	
	Mono<Optional<String>> geohash(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/geohash">GEOHASH</a> key member [member ...] 
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Flux<Optional<String>> geohash(A key, Consumer<Values<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/geopos">GEOPOS</a> key member
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Optional<GeoCoordinates>> geopos(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/geopos">GEOPOS</a> key member [member ...]
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Flux<Optional<GeoCoordinates>> geopos(A key, Consumer<Values<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi
	 * 
	 * @param key
	 * @param longitude
	 * @param latitude
	 * @param radius
	 * @param unit
	 * @return 
	 */
	default Flux<B> georadius(A key, double longitude, double latitude, double radius, GeoUnit unit) {
		return this.georadius().build(key, longitude, latitude, radius, unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @return 
	 */
	GeoradiusBuilder<A, B> georadius();
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @return 
	 */
	GeoradiusExtendedBuilder<A, B> georadiusExtended();
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * 
	 * @return 
	 */
	GeoradiusStoreBuilder<A, B> georadiusStore();
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi
	 * 
	 * @param key
	 * @param member
	 * @param radius
	 * @param unit
	 * @return 
	 */
	default Flux<B> georadiusbymember(A key, B member, double radius, GeoUnit unit) {
		return this.georadiusbymember().build(key, member, radius, unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @return 
	 */
	GeoradiusbymemberBuilder<A, B> georadiusbymember();
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @return 
	 */
	GeoradiusbymemberExtendedBuilder<A, B> georadiusbymemberExtended();
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * 
	 * @return 
	 */
	GeoradiusbymemberStoreBuilder<A, B> georadiusbymemberStore();
	
	/**
	 * <a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]]
	 * 
	 * @return 
	 */
	GeosearchBuilder<A, B> geosearch();
	
	/**
	 * <a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]] [WITHCOORD] [WITHDIST] [WITHHASH] 
	 * 
	 * @return 
	 */
	GeosearchExtendedBuilder<A, B> geosearchExtended();
	
	/**
	 * <a href="https://redis.io/commands/geosearchstore">GEOSEARCHSTORE</a> destination source [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]] [STOREDIST] 
	 * 
	 * @return 
	 */
	GeosearchstoreBuilder<A, B> geosearchstore();

	/**
	 * <a href="https://redis.io/commands/geoadd">GEOADD</a> key [NX|XX] [CH] longitude latitude member [longitude latitude member ...] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeoaddBuilder<A, B> {
		
		/**
		 * 
		 * @return 
		 */
		GeoaddBuilder<A, B> nx();
		
		/**
		 * 
		 * @return 
		 */
		GeoaddBuilder<A, B> xx();
		
		/**
		 * 
		 * @return 
		 */
		GeoaddBuilder<A, B> ch();

		/**
		 * 
		 * @param key
		 * @param longitude
		 * @param latitude
		 * @param member
		 * @return 
		 */
		Mono<Long> build(A key, double longitude, double latitude, B member);
		
		/**
		 * 
		 * @param key
		 * @param items
		 * @return 
		 */
		Mono<Long> build(A key, Consumer<GeoItems<B>> items);
	}
	
	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]</li>
	 * <li><a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractGeoradiusBuilder<A, B, C extends AbstractGeoradiusBuilder<A, B, C>> {
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		C count(long count);
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		C countany(long count);
		
		/**
		 * 
		 * @return 
		 */
		C asc();
		
		/**
		 * 
		 * @return 
		 */
		C desc();
	}
	
	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]</li>
	 * <li><a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractGeoradiusExtendedBuilder<A, B, C extends AbstractGeoradiusExtendedBuilder<A, B, C>> extends AbstractGeoradiusBuilder<A, B, C> {
		
		/**
		 * 
		 * @return 
		 */
		C withcoord();
		
		/**
		 * 
		 * @return 
		 */
		C withdist();
		
		/**
		 * 
		 * @return 
		 */
		C withhash();
	}
	
	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key]</li>
	 * <li><a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractGeoradiusStoreBuilder<A, B, C extends AbstractGeoradiusStoreBuilder<A, B, C>> extends AbstractGeoradiusBuilder<A, B, C> {
		
		/**
		 * 
		 * @return 
		 */
		C storedist();
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeoradiusBuilder<A, B> extends AbstractGeoradiusBuilder<A, B, GeoradiusBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param longitude
		 * @param latitude
		 * @param radius
		 * @param unit
		 * @return 
		 */
		Flux<B> build(A key, double longitude, double latitude, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeoradiusExtendedBuilder<A, B> extends AbstractGeoradiusExtendedBuilder<A, B, GeoradiusExtendedBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param longitude
		 * @param latitude
		 * @param radius
		 * @param unit
		 * @return 
		 */
		Flux<GeoWithin<B>> build(A key, double longitude, double latitude, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeoradiusStoreBuilder<A, B> extends AbstractGeoradiusStoreBuilder<A, B, GeoradiusStoreBuilder<A, B>> {
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @param longitude
		 * @param latitude
		 * @param radius
		 * @param unit
		 * @return 
		 */
		Mono<Long> build(A source, A destination, double longitude, double latitude, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeoradiusbymemberBuilder<A, B> extends AbstractGeoradiusBuilder<A, B, GeoradiusbymemberBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param member
		 * @param radius
		 * @param unit
		 * @return 
		 */
		Flux<B> build(A key, B member, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeoradiusbymemberExtendedBuilder<A, B> extends AbstractGeoradiusExtendedBuilder<A, B, GeoradiusbymemberExtendedBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param member
		 * @param radius
		 * @param unit
		 * @return 
		 */
		Flux<GeoWithin<B>> build(A key, B member, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeoradiusbymemberStoreBuilder<A, B> extends AbstractGeoradiusStoreBuilder<A, B, GeoradiusbymemberStoreBuilder<A, B>> {
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @param member
		 * @param radius
		 * @param unit
		 * @return 
		 */
		Mono<Long> build(A source, A destination, B member, double radius, GeoUnit unit);
	}
	
	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]]</li>
	 * <li><a href="https://redis.io/commands/geosearchstore">GEOSEARCHSTORE</a> destination source [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractGeosearchBuilder<A, B, C extends AbstractGeosearchBuilder<A, B, C>> {
		
		/**
		 * 
		 * @param member
		 * @return 
		 */
		C fromMember(B member);
		
		/**
		 * 
		 * @param longitude
		 * @param latitude
		 * @return 
		 */
		C fromCoordinates(double longitude, double latitude);
		
		/**
		 * 
		 * @param radius
		 * @param unit
		 * @return 
		 */
		C byRadius(double radius, GeoUnit unit);
		
		/**
		 * 
		 * @param width
		 * @param height
		 * @param unit
		 * @return 
		 */
		C byBox(double width, double height, GeoUnit unit);
		
		/**
		 * 
		 * @return 
		 */
		C asc();
		
		/**
		 * 
		 * @return 
		 */
		C desc();
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		C count(long count);
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		C countany(long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeosearchBuilder<A, B> extends AbstractGeosearchBuilder<A, B, GeosearchBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<B> build(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]] [WITHCOORD] [WITHDIST] [WITHHASH] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeosearchExtendedBuilder<A, B> extends AbstractGeosearchBuilder<A, B, GeosearchExtendedBuilder<A, B>> {
		
		/**
		 * 
		 * @return 
		 */
		GeosearchExtendedBuilder<A, B> withcoord();
		
		/**
		 * 
		 * @return 
		 */
		GeosearchExtendedBuilder<A, B> withdist();
		
		/**
		 * 
		 * @return 
		 */
		GeosearchExtendedBuilder<A, B> withhash();
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<GeoWithin<B>> build(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/geosearchstore">GEOSEARCHSTORE</a> destination source [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]] [STOREDIST] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface GeosearchstoreBuilder<A, B> extends AbstractGeosearchBuilder<A, B, GeosearchstoreBuilder<A, B>> {
		
		/**
		 * 
		 * @return 
		 */
		GeosearchstoreBuilder<A, B> storedist();
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @return 
		 */
		Mono<Long> build(A source, A destination);
	}
	
	/**
	 * 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	enum GeoUnit {

        /**
         * Meter.
         */
        m,

        /**
         * Kilometer.
         */
        km,

        /**
         * Feet.
         */
        ft,

        /**
         * Mile.
         */
        mi;
    }
	
	/**
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface GeoCoordinates {
		
		/**
		 * 
		 * @return 
		 */
		double getLongitude();
		
		
		/**
		 * 
		 * @return 
		 */
		double getLatitude();
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <B> value type
	 */
	interface GeoWithin<B> {
		
		/**
		 * 
		 * @return 
		 */
		Optional<Double> getDistance();

		/**
		 * 
		 * @return 
		 */
		Optional<Long> getHash();

		/**
		 * 
		 * @return 
		 */
		Optional<GeoCoordinates> getCoordinates();
		
		/**
		 * 
		 * @return 
		 */
		B getMember();
	}
	
	/**
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <B> value type
	 */
	interface GeoItems<B> {
		
		/**
		 * 
		 * @param longitude
		 * @param latitude
		 * @param member
		 * @return 
		 */
		GeoItems<B> item(double longitude, double latitude, B member);
	}
}

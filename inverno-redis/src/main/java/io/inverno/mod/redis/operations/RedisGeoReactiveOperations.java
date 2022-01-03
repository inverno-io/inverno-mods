/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.operations;

import io.inverno.mod.redis.util.Values;
import java.util.Optional;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public interface RedisGeoReactiveOperations<A, B> /*extends RedisGeoReactiveCommands<A, B>*/ {
	
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
	 * @param <A>
	 * @param <B> 
	 */
	interface GeoaddBuilder<A, B> {
		
		GeoaddBuilder<A, B> nx();
		GeoaddBuilder<A, B> xx();
		GeoaddBuilder<A, B> ch();

		Mono<Long> build(A key, double longitude, double latitude, B member);
		Mono<Long> build(A key, Consumer<GeoItems<B>> items);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] 
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @param <A>
	 * @param <B> 
	 * @param <C> 
	 */
	interface AbstractGeoradiusBuilder<A, B, C extends AbstractGeoradiusBuilder<A, B, C>> {
		C count(long count);
		C countany(long count);
		C asc();
		C desc();
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @param <A>
	 * @param <B> 
	 * @param <C> 
	 */
	interface AbstractGeoradiusExtendedBuilder<A, B, C extends AbstractGeoradiusExtendedBuilder<A, B, C>> extends AbstractGeoradiusBuilder<A, B, C> {
		C withcoord();
		C withdist();
		C withhash();
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * 
	 * @param <A>
	 * @param <B> 
	 * @param <C> 
	 */
	interface AbstractGeoradiusStoreBuilder<A, B, C extends AbstractGeoradiusStoreBuilder<A, B, C>> extends AbstractGeoradiusBuilder<A, B, C> {
		C store(A key);
		C storedist(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeoradiusBuilder<A, B> extends AbstractGeoradiusBuilder<A, B, GeoradiusBuilder<A, B>> {
		Flux<B> build(A key, double longitude, double latitude, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeoradiusExtendedBuilder<A, B> extends AbstractGeoradiusExtendedBuilder<A, B, GeoradiusExtendedBuilder<A, B>> {
		Flux<GeoWithin<B>> build(A key, double longitude, double latitude, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadius">GEORADIUS</a> key longitude latitude radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeoradiusStoreBuilder<A, B> extends AbstractGeoradiusStoreBuilder<A, B, GeoradiusStoreBuilder<A, B>> {
		Mono<Long> build(A key, double longitude, double latitude, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeoradiusbymemberBuilder<A, B> extends AbstractGeoradiusBuilder<A, B, GeoradiusbymemberBuilder<A, B>> {
		Flux<B> build(A key, B member, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count [ANY]] [ASC|DESC]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeoradiusbymemberExtendedBuilder<A, B> extends AbstractGeoradiusExtendedBuilder<A, B, GeoradiusbymemberExtendedBuilder<A, B>> {
		Flux<GeoWithin<B>> build(A key, B member, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> key member radius m|km|ft|mi [COUNT count [ANY]] [ASC|DESC] [STORE key] [STOREDIST key] 
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeoradiusbymemberStoreBuilder<A, B> extends AbstractGeoradiusStoreBuilder<A, B, GeoradiusbymemberStoreBuilder<A, B>> {
		Mono<Long> build(A key, B member, double radius, GeoUnit unit);
	}
	
	/**
	 * <a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]]
	 * <a href="https://redis.io/commands/geosearchstore">GEOSEARCHSTORE</a> destination source [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]]
	 * 
	 * @param <A>
	 * @param <B>
	 * @param <C> 
	 */
	interface AbstractGeosearchBuilder<A, B, C extends AbstractGeosearchBuilder<A, B, C>> {
		C fromMember(B member);
		C fromCoordinates(double longitude, double latitude);
		C byRadius(double radius, GeoUnit unit);
		C byBox(double width, double height, GeoUnit unit);
		C asc();
		C desc();
		C count(long count);
		C countany(long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeosearchBuilder<A, B> extends AbstractGeosearchBuilder<A, B, GeosearchBuilder<A, B>> {
		Flux<B> build(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/geosearch">GEOSEARCH</a> key [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]] [WITHCOORD] [WITHDIST] [WITHHASH] 
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeosearchExtendedBuilder<A, B> extends AbstractGeosearchBuilder<A, B, GeosearchBuilder<A, B>> {
		GeosearchBuilder<A, B> withcoord();
		GeosearchBuilder<A, B> withdist();
		GeosearchBuilder<A, B> withhash();
		
		Flux<GeoWithin<B>> build(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/geosearchstore">GEOSEARCHSTORE</a> destination source [FROMMEMBER member] [FROMLONLAT longitude latitude] [BYRADIUS radius m|km|ft|mi] [BYBOX width height m|km|ft|mi] [ASC|DESC] [COUNT count [ANY]] [STOREDIST] 
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface GeosearchstoreBuilder<A, B> extends AbstractGeosearchBuilder<A, B, GeosearchstoreBuilder<A, B>> {
		GeosearchstoreBuilder<A, B> storedist();
		
		Mono<Long> build(A destination, A source);
	}
	
	/**
	 * 
	 */
	enum GeoUnit {

        /**
         * meter.
         */
        m,

        /**
         * kilometer.
         */
        km,

        /**
         * feet.
         */
        ft,

        /**
         * mile.
         */
        mi;
    }
	
	/**
	 * 
	 */
	interface GeoCoordinates {
		
		double getLongitude();
		
		double getLatitude();
	}
	
	/**
	 * 
	 * @param <B> 
	 */
	interface GeoWithin<B> {
		
		Optional<Double> getDistance();

		Optional<Long> gethash();

		Optional<GeoCoordinates> getCoordinates();
		
		B getMember();
	}
	
	/**
	 * 
	 * @param <B> 
	 */
	interface GeoItems<B> {
		GeoItems<B> item(double longitude, double latitude, B member);
	}
}

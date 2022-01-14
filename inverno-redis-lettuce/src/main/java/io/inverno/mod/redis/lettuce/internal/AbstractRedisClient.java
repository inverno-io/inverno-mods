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

import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.redis.RedisOperations;
import io.inverno.mod.redis.lettuce.internal.operations.GeoaddBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeoradiusBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeoradiusExtendedBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeoradiusStoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeoradiusbymemberBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeoradiusbymemberExtendedBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeoradiusbymemberStoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeosearchBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeosearchExtendedBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.GeosearchstoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.HashScanBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyCopyBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyExpireBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyExpireatBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyMigrateBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyPexpireBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyPexpireatBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyRestoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeyScanBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeySortBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.KeySortStoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.ListBlmoveBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.ListBlmpopBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.ListLmoveBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.ListLmpopBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.ListLposBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SetScanBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetBzmpopBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetScanBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZaddBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZaddIncrBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZinterBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZinterWithScoresBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZinterstoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZmpopBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZrangeBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZrangeWithScoresBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZrangestoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZunionBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZunionWithScoresBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.SortedSetZunionstoreBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StatefulRedisConnectionOperations;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXaddBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXautoclaimBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXclaimBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXgroupCreateBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXpendingExtendedBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXreadBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXreadgroupBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StreamXtrimBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StringBitfieldBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StringGetexBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StringSetBuilderImpl;
import io.inverno.mod.redis.lettuce.internal.operations.StringSetGetBuilderImpl;
import io.inverno.mod.redis.operations.Bound;
import io.inverno.mod.redis.operations.Entries;
import io.inverno.mod.redis.operations.EntryOptional;
import io.inverno.mod.redis.operations.Keys;
import io.inverno.mod.redis.operations.Values;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base Lettuce Redis client implementation class.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 * @param <B>
 * @param <C> 
 */
public abstract class AbstractRedisClient<A, B, C extends StatefulConnection<A, B>> implements RedisClient<A, B> {
	
	protected final BoundedAsyncPool<C> pool;
	
	protected final Class<A> keyType;
	
	protected final Class<B> valueType;
	
	/**
	 * <p>
	 * Creates a Redis client with the specified Lettuce pool.
	 * </p>
	 * 
	 * @param pool      a bounded async pool
	 * @param keyType   the key type
	 * @param valueType the value type
	 */
	public AbstractRedisClient(BoundedAsyncPool<C> pool, Class<A> keyType, Class<B> valueType) {
		this.pool = pool;
		this.keyType = keyType;
		this.valueType = valueType;
	}
	
	/**
	 * <p>
	 * Returns Redis Operations.
	 * </p>
	 * 
	 * @return a mono emitting RedisOperations object
	 */
	protected abstract Mono<StatefulRedisConnectionOperations<A, B, C, ?>> operations();

	@Override
	public <T> Publisher<T> connection(Function<RedisOperations<A, B>, Publisher<T>> function) {
		return Flux.usingWhen(
			this.operations(), 
			function, 
			operations -> operations.close()
		);
	}

	@Override
	public <T> Publisher<T> batch(Function<RedisOperations<A, B>, Publisher<Publisher<T>>> function) {
		return Flux.usingWhen(
			this.operations().doOnNext(o -> o.getCommands().setAutoFlushCommands(false)), 
			operations -> Flux.mergeSequential(Flux.from(function.apply(operations)).concatWithValues(Mono.<T>empty().doOnSubscribe(ign -> operations.getCommands().flushCommands()))),
			operations -> {
				// restore default and close operations
				operations.getCommands().setAutoFlushCommands(true);
				return operations.close();
			}
		);
	}

	@Override
	public Mono<Void> close() {
		return Mono.fromCompletionStage(this.pool.closeAsync());
	}

	@Override
	public Mono<Long> geoadd(A key, double longitude, double latitude, B member) {
		return Mono.from(this.connection(o -> o.geoadd(key, longitude, latitude, member)));
	}

	@Override
	public Mono<Long> geoadd(A key, Consumer<GeoItems<B>> items) {
		return Mono.from(this.connection(o -> o.geoadd(key, items)));
	}

	@Override
	public GeoaddBuilder<A, B> geoadd() {
		return new GeoaddBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Double> geodist(A key, B member1, B member2, GeoUnit unit) {
		return Mono.from(this.connection(o -> o.geodist(key, member1, member2, unit)));
	}

	@Override
	public Mono<Optional<String>> geohash(A key, B member) {
		return Mono.from(this.connection(o -> o.geohash(key, member)));
	}

	@Override
	public Flux<Optional<String>> geohash(A key, Consumer<Values<B>> members) {
		return Flux.from(this.connection(o -> o.geohash(key, members)));
	}

	@Override
	public Mono<Optional<GeoCoordinates>> geopos(A key, B member) {
		return Mono.from(this.connection(o -> o.geopos(key, member)));
	}

	@Override
	public Flux<Optional<GeoCoordinates>> geopos(A key, Consumer<Values<B>> members) {
		return Flux.from(this.connection(o -> o.geopos(key, members)));
	}

	@Override
	public GeoradiusBuilder<A, B> georadius() {
		return new GeoradiusBuilderImpl<>(this.operations());
	}

	@Override
	public GeoradiusExtendedBuilder<A, B> georadiusExtended() {
		return new GeoradiusExtendedBuilderImpl<>(this.operations());
	}

	@Override
	public GeoradiusStoreBuilder<A, B> georadiusStore() {
		return new GeoradiusStoreBuilderImpl<>(this.operations());
	}

	@Override
	public GeoradiusbymemberBuilder<A, B> georadiusbymember() {
		return new GeoradiusbymemberBuilderImpl<>(this.operations());
	}

	@Override
	public GeoradiusbymemberExtendedBuilder<A, B> georadiusbymemberExtended() {
		return new GeoradiusbymemberExtendedBuilderImpl<>(this.operations());
	}

	@Override
	public GeoradiusbymemberStoreBuilder<A, B> georadiusbymemberStore() {
		return new GeoradiusbymemberStoreBuilderImpl<>(this.operations());
	}

	@Override
	public GeosearchBuilder<A, B> geosearch() {
		return new GeosearchBuilderImpl<>(this.operations());
	}

	@Override
	public GeosearchExtendedBuilder<A, B> geosearchExtended() {
		return new GeosearchExtendedBuilderImpl<>(this.operations());
	}

	@Override
	public GeosearchstoreBuilder<A, B> geosearchstore() {
		return new GeosearchstoreBuilderImpl<>(this.operations());
	}
	
	@Override
	public Mono<Long> hdel(A key, A field) {
		return Mono.from(this.connection(o -> o.hdel(key, field)));
	}

	@Override
	public Mono<Long> hdel(A key, Consumer<Keys<A>> fields) {
		return Mono.from(this.connection(o -> o.hdel(key, fields)));
	}

	@Override
	public Mono<Boolean> hexists(A key, A field) {
		return Mono.from(this.connection(o -> o.hexists(key, field)));
	}

	@Override
	public Mono<B> hget(A key, A field) {
		return Mono.from(this.connection(o -> o.hget(key, field)));
	}

	@Override
	public Flux<EntryOptional<A, B>> hgetall(A key) {
		return Flux.from(this.connection(o -> o.hgetall(key)));
	}

	@Override
	public Mono<Long> hincrby(A key, A field, long increment) {
		return Mono.from(this.connection(o -> o.hincrby(key, field, increment)));
	}

	@Override
	public Mono<Double> hincrbyfloat(A key, A field, double increment) {
		return Mono.from(this.connection(o -> o.hincrbyfloat(key, field, increment)));
	}

	@Override
	public Flux<A> hkeys(A key) {
		return Flux.from(this.connection(o -> o.hkeys(key)));
	}

	@Override
	public Mono<Long> hlen(A key) {
		return Mono.from(this.connection(o -> o.hlen(key)));
	}

	@Override
	public Flux<EntryOptional<A, B>> hmget(A key, Consumer<Keys<A>> fields) {
		return Flux.from(this.connection(o -> o.hmget(key, fields)));
	}

	@Override
	public Mono<String> hmset(A key, Consumer<Entries<A, B>> entries) {
		return Mono.from(this.connection(o -> o.hmset(key, entries)));
	}

	@Override
	public Mono<A> hrandfield(A key) {
		return Mono.from(this.connection(o -> o.hrandfield(key)));
	}

	@Override
	public Flux<A> hrandfield(A key, long count) {
		return Flux.from(this.connection(o -> o.hrandfield(key, count)));
	}

	@Override
	public Flux<EntryOptional<A, B>> hrandfieldWithvalues(A key, long count) {
		return Flux.from(this.connection(o -> o.hrandfieldWithvalues(key, count)));
	}

	@Override
	public Mono<HashScanResult<A, B>> hscan(A key, String cursor) {
		return Mono.from(this.connection(o -> o.hscan(key, cursor)));
	}

	@Override
	public HashScanBuilder<A, B> hscan() {
		return new HashScanBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Boolean> hset(A key, A field, B value) {
		return Mono.from(this.connection(o -> o.hset(key, field, value)));
	}

	@Override
	public Mono<Long> hset(A key, Consumer<Entries<A, B>> entries) {
		return Mono.from(this.connection(o -> o.hset(key, entries)));
	}

	@Override
	public Mono<Boolean> hsetnx(A key, A field, B value) {
		return Mono.from(this.connection(o -> o.hsetnx(key, field, value)));
	}

	@Override
	public Mono<Long> hstrlen(A key, A field) {
		return Mono.from(this.connection(o -> o.hstrlen(key, field)));
	}

	@Override
	public Flux<B> hvals(A key) {
		return Flux.from(this.connection(o -> o.hvals(key)));
	}

	@Override
	public Mono<Long> pfadd(A key, B value) {
		return Mono.from(this.connection(o -> o.pfadd(key, value)));
	}

	@Override
	public Mono<Long> pfadd(A key, Consumer<Values<B>> values) {
		return Mono.from(this.connection(o -> o.pfadd(key, values)));
	}

	@Override
	public Mono<Long> pfcount(A key) {
		return Mono.from(this.connection(o -> o.pfcount(key)));
	}

	@Override
	public Mono<Long> pfcount(Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.pfcount(keys)));
	}

	@Override
	public Mono<String> pfmerge(A destkey, A sourcekey) {
		return Mono.from(this.connection(o -> o.pfmerge(destkey, sourcekey)));
	}

	@Override
	public Mono<String> pfmerge(A destkey, Consumer<Keys<A>> sourcekeys) {
		return Mono.from(this.connection(o -> o.pfmerge(destkey, sourcekeys)));
	}

	@Override
	public Mono<Boolean> copy(A source, A destination) {
		return Mono.from(this.connection(o -> o.copy(source, destination)));
	}

	@Override
	public KeyCopyBuilder<A> copy() {
		return new KeyCopyBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> del(A key) {
		return Mono.from(this.connection(o -> o.del(key)));
	}

	@Override
	public Mono<Long> del(Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.del(keys)));
	}

	@Override
	public Mono<byte[]> dump(A key) {
		return Mono.from(this.connection(o -> o.dump(key)));
	}

	@Override
	public Mono<Long> exists(A key) {
		return Mono.from(this.connection(o -> o.exists(key)));
	}

	@Override
	public Mono<Long> exists(Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.exists(keys)));
	}

	@Override
	public Mono<Boolean> expire(A key, long seconds) {
		return Mono.from(this.connection(o -> o.expire(key, seconds)));
	}

	@Override
	public Mono<Boolean> expire(A key, Duration duration) {
		return Mono.from(this.connection(o -> o.expire(key, duration)));
	}

	@Override
	public KeyExpireBuilder<A> expire() {
		return new KeyExpireBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Boolean> expireat(A key, long epochSeconds) {
		return Mono.from(this.connection(o -> o.expireat(key, epochSeconds)));
	}

	@Override
	public Mono<Boolean> expireat(A key, ZonedDateTime datetime) {
		return Mono.from(this.connection(o -> o.expireat(key, datetime)));
	}

	@Override
	public Mono<Boolean> expireat(A key, Instant instant) {
		return Mono.from(this.connection(o -> o.expireat(key, instant)));
	}

	@Override
	public KeyExpireatBuilder<A> expireat() {
		return new KeyExpireatBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> expiretime(A key) {
		return Mono.from(this.connection(o -> o.expiretime(key)));
	}

	@Override
	public Flux<A> keys(A pattern) {
		return Flux.from(this.connection(o -> o.keys(pattern)));
	}

	@Override
	public Mono<String> migrate(String host, int port, A key, int db, long timeout) {
		return Mono.from(this.connection(o -> o.migrate(host, port, key, db, timeout)));
	}

	@Override
	public KeyMigrateBuilder<A> migrate() {
		return new KeyMigrateBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Mono<Boolean> move(A key, int db) {
		return Mono.from(this.connection(o -> o.move(key, db)));
	}

	@Override
	public Mono<String> objectEncoding(A key) {
		return Mono.from(this.connection(o -> o.objectEncoding(key)));
	}

	@Override
	public Mono<Long> objectFreq(A key) {
		return Mono.from(this.connection(o -> o.objectFreq(key)));
	}

	@Override
	public Mono<Long> objectIdletime(A key) {
		return Mono.from(this.connection(o -> o.objectIdletime(key)));
	}

	@Override
	public Mono<Long> objectRefcount(A key) {
		return Mono.from(this.connection(o -> o.objectRefcount(key)));
	}

	@Override
	public Mono<Boolean> persist(A key) {
		return Mono.from(this.connection(o -> o.persist(key)));
	}

	@Override
	public Mono<Boolean> pexpire(A key, long milliseconds) {
		return Mono.from(this.connection(o -> o.pexpire(key, milliseconds)));
	}

	@Override
	public Mono<Boolean> pexpire(A key, Duration duration) {
		return Mono.from(this.connection(o -> o.pexpire(key, duration)));
	}

	@Override
	public KeyPexpireBuilder<A> pexpire() {
		return new KeyPexpireBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Boolean> pexpireat(A key, long epochMilliseconds) {
		return Mono.from(this.connection(o -> o.pexpireat(key, epochMilliseconds)));
	}

	@Override
	public Mono<Boolean> pexpireat(A key, ZonedDateTime datetime) {
		return Mono.from(this.connection(o -> o.pexpireat(key, datetime)));
	}

	@Override
	public Mono<Boolean> pexpireat(A key, Instant instant) {
		return Mono.from(this.connection(o -> o.pexpireat(key, instant)));
	}

	@Override
	public KeyPexpireatBuilder<A> pexpireat() {
		return new KeyPexpireatBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> pexpiretime(A key) {
		return Mono.from(this.connection(o -> o.pexpiretime(key)));
	}

	@Override
	public Mono<Long> pttl(A key) {
		return Mono.from(this.connection(o -> o.pttl(key)));
	}

	@Override
	public Mono<A> randomkey() {
		return Mono.from(this.connection(o -> o.randomkey()));
	}

	@Override
	public Mono<String> rename(A key, A newkey) {
		return Mono.from(this.connection(o -> o.rename(key, newkey)));
	}

	@Override
	public Mono<Boolean> renamenx(A key, A newkey) {
		return Mono.from(this.connection(o -> o.renamenx(key, newkey)));
	}

	@Override
	public Mono<String> restore(A key, long ttl, byte[] serializedValue) {
		return Mono.from(this.connection(o -> o.restore(key, ttl, serializedValue)));
	}

	@Override
	public KeyRestoreBuilder<A> restore() {
		return new KeyRestoreBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<KeyScanResult<A>> scan(String cursor) {
		return Mono.from(this.connection(o -> o.scan(cursor)));
	}

	@Override
	public KeyScanBuilder<A> scan() {
		return new KeyScanBuilderImpl<>(this.operations());
	}
	
	@Override
	public Flux<B> sort(A key) {
		return Flux.from(this.connection(o -> o.sort(key)));
	}

	@Override
	public KeySortBuilder<A, B> sort() {
		return new KeySortBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> sortStore(A key, A destination) {
		return Mono.from(this.connection(o -> o.sortStore(key, destination)));
	}

	@Override
	public KeySortStoreBuilder<A> sortStore() {
		return new KeySortStoreBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> touch(A key) {
		return Mono.from(this.connection(o -> o.touch(key)));
	}

	@Override
	public Mono<Long> touch(Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.touch(keys)));
	}

	@Override
	public Mono<Long> ttl(A key) {
		return Mono.from(this.connection(o -> o.ttl(key)));
	}

	@Override
	public Mono<String> type(A key) {
		return Mono.from(this.connection(o -> o.type(key)));
	}

	@Override
	public Mono<Long> unlink(A key) {
		return Mono.from(this.connection(o -> o.unlink(key)));
	}

	@Override
	public Mono<Long> unlink(Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.unlink(keys)));
	}

	@Override
	public Mono<Long> waitForReplication(int replicas, long timeout) {
		return Mono.from(this.connection(o -> o.waitForReplication(replicas, timeout)));
	}

	@Override
	public ListBlmoveBuilder<A, B> blmove() {
		return new ListBlmoveBuilderImpl<>(this.operations());
	}

	@Override
	public ListBlmpopBuilder<A, B> blmpop() {
		return new ListBlmpopBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<EntryOptional<A, B>> blpop(A key, double timeout) {
		return Mono.from(this.connection(o -> o.blpop(key, timeout)));
	}

	@Override
	public Mono<EntryOptional<A, B>> blpop(Consumer<Keys<A>> keys, double timeout) {
		return Mono.from(this.connection(o -> o.blpop(keys, timeout)));
	}

	@Override
	public Mono<EntryOptional<A, B>> brpop(A key, double timeout) {
		return Mono.from(this.connection(o -> o.brpop(key, timeout)));
	}

	@Override
	public Mono<EntryOptional<A, B>> brpop(Consumer<Keys<A>> keys, double timeout) {
		return Mono.from(this.connection(o -> o.brpop(keys, timeout)));
	}

	@Override
	public Mono<B> brpoplpush(A source, A destination, double timeout) {
		return Mono.from(this.connection(o -> o.brpoplpush(source, destination, timeout)));
	}

	@Override
	public Mono<B> lindex(A key, long index) {
		return Mono.from(this.connection(o -> o.lindex(key, index)));
	}

	@Override
	public Mono<Long> linsert(A key, boolean before, B pivot, B element) {
		return Mono.from(this.connection(o -> o.linsert(key, before, pivot, element)));
	}

	@Override
	public Mono<Long> llen(A key) {
		return Mono.from(this.connection(o -> o.llen(key)));
	}

	@Override
	public ListLmoveBuilder<A, B> lmove() {
		return new ListLmoveBuilderImpl<>(this.operations());
	}

	@Override
	public ListLmpopBuilder<A, B> lmpop() {
		return new ListLmpopBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<B> lpop(A key) {
		return Mono.from(this.connection(o -> o.lpop(key)));
	}

	@Override
	public Flux<B> lpop(A key, long count) {
		return Flux.from(this.connection(o -> o.lpop(key, count)));
	}

	@Override
	public Mono<Long> lpos(A key, B element) {
		return Mono.from(this.connection(o -> o.lpos(key, element)));
	}

	@Override
	public Flux<Long> lpos(A key, B element, long count) {
		return Flux.from(this.connection(o -> o.lpos(key, element, count)));
	}

	@Override
	public ListLposBuilder<A, B> lpos() {
		return new ListLposBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> lpush(A key, B element) {
		return Mono.from(this.connection(o -> o.lpush(key, element)));
	}

	@Override
	public Mono<Long> lpush(A key, Consumer<Values<B>> elements) {
		return Mono.from(this.connection(o -> o.lpush(key, elements)));
	}

	@Override
	public Mono<Long> lpushx(A key, B element) {
		return Mono.from(this.connection(o -> o.lpushx(key, element)));
	}

	@Override
	public Mono<Long> lpushx(A key, Consumer<Values<B>> elements) {
		return Mono.from(this.connection(o -> o.lpushx(key, elements)));
	}

	@Override
	public Flux<B> lrange(A key, long start, long stop) {
		return Flux.from(this.connection(o -> o.lrange(key, start, stop)));
	}

	@Override
	public Mono<Long> lrem(A key, long count, B element) {
		return Mono.from(this.connection(o -> o.lrem(key, count, element)));
	}

	@Override
	public Mono<String> lset(A key, long index, B element) {
		return Mono.from(this.connection(o -> o.lset(key, index, element)));
	}

	@Override
	public Mono<String> ltrim(A key, long start, long stop) {
		return Mono.from(this.connection(o -> o.ltrim(key, start, stop)));
	}

	@Override
	public Mono<B> rpop(A key) {
		return Mono.from(this.connection(o -> o.rpop(key)));
	}

	@Override
	public Flux<B> rpop(A key, long count) {
		return Flux.from(this.connection(o -> o.rpop(key, count)));
	}

	@Override
	public Mono<B> rpoplpush(A source, A destination) {
		return Mono.from(this.connection(o -> o.rpoplpush(source, destination)));
	}

	@Override
	public Mono<Long> rpush(A key, B element) {
		return Mono.from(this.connection(o -> o.rpush(key, element)));
	}

	@Override
	public Mono<Long> rpush(A key, Consumer<Values<B>> elements) {
		return Mono.from(this.connection(o -> o.rpush(key, elements)));
	}

	@Override
	public Mono<Long> rpushx(A key, B element) {
		return Mono.from(this.connection(o -> o.rpushx(key, element)));
	}

	@Override
	public Mono<Long> rpushx(A key, Consumer<Values<B>> elements) {
		return Mono.from(this.connection(o -> o.rpushx(key, elements)));
	}

	@Override
	public String digest(byte[] script) {
		return Mono.from(this.connection(o -> Mono.just(o.digest(script)))).block();
	}

	@Override
	public String digest(String script) {
		return Mono.from(this.connection(o -> Mono.just(o.digest(script)))).block();
	}

	@Override
	public <T> Flux<T> eval(String script, ScriptOutput output) {
		return Flux.from(this.connection(o -> o.eval(script, output)));
	}

	@Override
	public <T> Flux<T> eval(byte[] script, ScriptOutput output) {
		return Flux.from(this.connection(o -> o.eval(script, output)));
	}

	@Override
	public <T> Flux<T> eval(String script, ScriptOutput output, Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.eval(script, output, keys)));
	}

	@Override
	public <T> Flux<T> eval(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.eval(script, output, keys)));
	}

	@Override
	public <T> Flux<T> eval(String script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		return Flux.from(this.connection(o -> o.eval(script, output, keys, args)));
	}

	@Override
	public <T> Flux<T> eval(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		return Flux.from(this.connection(o -> o.eval(script, output, keys, args)));
	}

	@Override
	public <T> Flux<T> eval_ro(String script, ScriptOutput output) {
		return Flux.from(this.connection(o -> o.eval_ro(script, output)));
	}

	@Override
	public <T> Flux<T> eval_ro(byte[] script, ScriptOutput output) {
		return Flux.from(this.connection(o -> o.eval_ro(script, output)));
	}

	@Override
	public <T> Flux<T> eval_ro(String script, ScriptOutput output, Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.eval_ro(script, output, keys)));
	}

	@Override
	public <T> Flux<T> eval_ro(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.eval_ro(script, output, keys)));
	}

	@Override
	public <T> Flux<T> eval_ro(String script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		return Flux.from(this.connection(o -> o.eval_ro(script, output, keys, args)));
	}

	@Override
	public <T> Flux<T> eval_ro(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		return Flux.from(this.connection(o -> o.eval_ro(script, output, keys, args)));
	}

	@Override
	public <T> Flux<T> evalsha(String digest, ScriptOutput output) {
		return Flux.from(this.connection(o -> o.evalsha(digest, output)));
	}

	@Override
	public <T> Flux<T> evalsha(String digest, ScriptOutput output, Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.evalsha(digest, output, keys)));
	}

	@Override
	public <T> Flux<T> evalsha(String digest, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		return Flux.from(this.connection(o -> o.evalsha(digest, output, keys, args)));
	}

	@Override
	public <T> Flux<T> evalsha_ro(String digest, ScriptOutput output) {
		return Flux.from(this.connection(o -> o.evalsha_ro(digest, output)));
	}

	@Override
	public <T> Flux<T> evalsha_ro(String digest, ScriptOutput output, Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.evalsha_ro(digest, output, keys)));
	}

	@Override
	public <T> Flux<T> evalsha_ro(String digest, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		return Flux.from(this.connection(o -> o.evalsha_ro(digest, output, keys, args)));
	}

	@Override
	public Flux<Boolean> scriptExists(String... digests) {
		return Flux.from(this.connection(o -> o.scriptExists(digests)));
	}

	@Override
	public Mono<String> scriptFlush() {
		return Mono.from(this.connection(o -> o.scriptFlush()));
	}

	@Override
	public Mono<String> scriptFlush(ScriptFlushMode flushMode) {
		return Mono.from(this.connection(o -> o.scriptFlush(flushMode)));
	}

	@Override
	public Mono<String> scriptKill() {
		return Mono.from(this.connection(o -> o.scriptKill()));
	}

	@Override
	public Mono<String> scriptLoad(String script) {
		return Mono.from(this.connection(o -> o.scriptLoad(script)));
	}

	@Override
	public Mono<String> scriptLoad(byte[] script) {
		return Mono.from(this.connection(o -> o.scriptLoad(script)));
	}

	@Override
	public Mono<Long> sadd(A key, B member) {
		return Mono.from(this.connection(o -> o.sadd(key, member)));
	}

	@Override
	public Mono<Long> sadd(A key, Consumer<Values<B>> members) {
		return Mono.from(this.connection(o -> o.sadd(key, members)));
	}

	@Override
	public Mono<Long> scard(A key) {
		return Mono.from(this.connection(o -> o.scard(key)));
	}

	@Override
	public Flux<B> sdiff(A key) {
		return Flux.from(this.connection(o -> o.sdiff(key)));
	}

	@Override
	public Flux<B> sdiff(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.sdiff(keys)));
	}

	@Override
	public Mono<Long> sdiffstore(A destination, A key) {
		return Mono.from(this.connection(o -> o.sdiffstore(destination, key)));
	}

	@Override
	public Mono<Long> sdiffstore(A destination, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.sdiffstore(destination, keys)));
	}

	@Override
	public Flux<B> sinter(A key) {
		return Flux.from(this.connection(o -> o.sinter(key)));
	}

	@Override
	public Flux<B> sinter(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.sinter(keys)));
	}

	@Override
	public Mono<Long> sintercard(A key) {
		return Mono.from(this.connection(o -> o.sintercard(key)));
	}

	@Override
	public Mono<Long> sintercard(A key, long limit) {
		return Mono.from(this.connection(o -> o.sintercard(key, limit)));
	}

	@Override
	public Mono<Long> sintercard(Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.sintercard(keys)));
	}

	@Override
	public Mono<Long> sintercard(Consumer<Keys<A>> keys, long limit) {
		return Mono.from(this.connection(o -> o.sintercard(keys, limit)));
	}

	@Override
	public Mono<Long> sinterstore(A destination, A key) {
		return Mono.from(this.connection(o -> o.sinterstore(destination, key)));
	}

	@Override
	public Mono<Long> sinterstore(A destination, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.sinterstore(destination, keys)));
	}

	@Override
	public Mono<Boolean> sismember(A key, B member) {
		return Mono.from(this.connection(o -> o.sismember(key, member)));
	}

	@Override
	public Flux<B> smembers(A key) {
		return Flux.from(this.connection(o -> o.smembers(key)));
	}

	@Override
	public Flux<Boolean> smismember(A key, Consumer<Values<B>> members) {
		return Flux.from(this.connection(o -> o.smismember(key, members)));
	}

	@Override
	public Mono<Boolean> smove(A source, A destination, B member) {
		return Mono.from(this.connection(o -> o.smove(source, destination, member)));
	}

	@Override
	public Mono<B> spop(A key) {
		return Mono.from(this.connection(o -> o.spop(key)));
	}

	@Override
	public Flux<B> spop(A key, long count) {
		return Flux.from(this.connection(o -> o.spop(key, count)));
	}

	@Override
	public Mono<B> srandmember(A key) {
		return Mono.from(this.connection(o -> o.srandmember(key)));
	}

	@Override
	public Flux<B> srandmember(A key, long count) {
		return Flux.from(this.connection(o -> o.srandmember(key, count)));
	}

	@Override
	public Mono<Long> srem(A key, B member) {
		return Mono.from(this.connection(o -> o.srem(key, member)));
	}

	@Override
	public Mono<Long> srem(A key, Consumer<Values<B>> members) {
		return Mono.from(this.connection(o -> o.srem(key, members)));
	}

	@Override
	public Mono<SetScanResult<B>> sscan(A key, String cursor) {
		return Mono.from(this.connection(o -> o.sscan(key, cursor)));
	}

	@Override
	public SetScanBuilder<A, B> sscan() {
		return new SetScanBuilderImpl<>(this.operations());
	}

	@Override
	public Flux<B> sunion(A key) {
		return Flux.from(this.connection(o -> o.sunion(key)));
	}

	@Override
	public Flux<B> sunion(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.sunion(keys)));
	}

	@Override
	public Mono<Long> sunionstore(A destination, A key) {
		return Mono.from(this.connection(o -> o.sunionstore(destination, key)));
	}

	@Override
	public Mono<Long> sunionstore(A destination, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.sunionstore(destination, keys)));
	}

	@Override
	public SortedSetBzmpopBuilder<A, B> bzmpop() {
		return new SortedSetBzmpopBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmax(double timeout, A key) {
		return Mono.from(this.connection(o -> o.bzpopmax(timeout, key)));
	}

	@Override
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmax(double timeout, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.bzpopmax(timeout, keys)));
	}

	@Override
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmin(double timeout, A key) {
		return Mono.from(this.connection(o -> o.bzpopmin(timeout, key)));
	}

	@Override
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmin(double timeout, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.bzpopmin(timeout, keys)));
	}

	@Override
	public Mono<Long> zadd(A key, double score, B member) {
		return Mono.from(this.connection(o -> o.zadd(key, score, member)));
	}

	@Override
	public Mono<Long> zadd(A key, Consumer<SortedSetScoredMembers<B>> members) {
		return Mono.from(this.connection(o -> o.zadd(key, members)));
	}

	@Override
	public SortedSetZaddBuilder<A, B> zadd() {
		return new SortedSetZaddBuilderImpl<>(this.operations());
	}
	
	@Override
	public Mono<Double> zaddIncr(A key, double score, B member) {
		return Mono.from(this.connection(o -> o.zaddIncr(key, score, member)));
	}

	@Override
	public SortedSetZaddIncrBuilder<A, B> zaddIncr() {
		return new SortedSetZaddIncrBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> zcard(A key) {
		return Mono.from(this.connection(o -> o.zcard(key)));
	}

	@Override
	public Mono<Long> zcount(A key, Bound<? extends Number> min, Bound<? extends Number> max) {
		return Mono.from(this.connection(o -> o.zcount(key, min, max)));
	}

	@Override
	public Flux<B> zdiff(A key) {
		return Flux.from(this.connection(o -> o.zdiff(key)));
	}

	@Override
	public Flux<B> zdiff(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.zdiff(keys)));
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zdiffWithScores(A key) {
		return Flux.from(this.connection(o -> o.zdiffWithScores(key)));
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zdiffWithScores(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.zdiffWithScores(keys)));
	}

	@Override
	public Mono<Long> zdiffstore(A destination, A key) {
		return Mono.from(this.connection(o -> o.zdiffstore(destination, key)));
	}

	@Override
	public Mono<Long> zdiffstore(A destination, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.zdiffstore(destination, keys)));
	}

	@Override
	public Mono<Double> zincrby(A key, double increment, B member) {
		return Mono.from(this.connection(o -> o.zincrby(key, increment, member)));
	}

	@Override
	public Flux<B> zinter(A key) {
		return Flux.from(this.connection(o -> o.zinter(key)));
	}

	@Override
	public Flux<B> zinter(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.zinter(keys)));
	}

	@Override
	public SortedSetZinterBuilder<A, B> zinter() {
		return new SortedSetZinterBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zinterWithScores(A key) {
		return Flux.from(this.connection(o -> o.zinterWithScores(key)));
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zinterWithScores(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.zinterWithScores(keys)));
	}

	@Override
	public SortedSetZinterWithScoresBuilder<A, B> zinterWithScores() {
		return new SortedSetZinterWithScoresBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Mono<Long> zintercard(A key) {
		return Mono.from(this.connection(o -> o.zintercard(key)));
	}

	@Override
	public Mono<Long> zintercard(A key, long limit) {
		return Mono.from(this.connection(o -> o.zintercard(key, limit)));
	}

	@Override
	public Mono<Long> zintercard(Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.zintercard(keys)));
	}

	@Override
	public Mono<Long> zintercard(Consumer<Keys<A>> keys, long limit) {
		return Mono.from(this.connection(o -> o.zintercard(keys, limit)));
	}

	@Override
	public Mono<Long> zinterstore(A destination, A key) {
		return Mono.from(this.connection(o -> o.zinterstore(destination, key)));
	}

	@Override
	public Mono<Long> zinterstore(A destination, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.zinterstore(destination, keys)));
	}

	@Override
	public SortedSetZinterstoreBuilder<A, B> zinterstore() {
		return new SortedSetZinterstoreBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Mono<Long> zlexcount(A key, Bound<B> min, Bound<B> max) {
		return Mono.from(this.connection(o -> o.zlexcount(key, min, max)));
	}

	@Override
	public SortedSetZmpopBuilder<A, B> zmpop() {
		return new SortedSetZmpopBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Flux<Optional<Double>> zmscore(A key, B member) {
		return Flux.from(this.connection(o -> o.zmscore(key, member)));
	}

	@Override
	public Flux<Optional<Double>> zmscore(A key, Consumer<Values<B>> members) {
		return Flux.from(this.connection(o -> o.zmscore(key, members)));
	}

	@Override
	public Mono<SortedSetScoredMember<B>> zpopmax(A key) {
		return Mono.from(this.connection(o -> o.zpopmax(key)));
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zpopmax(A key, long count) {
		return Flux.from(this.connection(o -> o.zpopmax(key, count)));
	}

	@Override
	public Mono<SortedSetScoredMember<B>> zpopmin(A key) {
		return Mono.from(this.connection(o -> o.zpopmin(key)));
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zpopmin(A key, long count) {
		return Flux.from(this.connection(o -> o.zpopmin(key, count)));
	}

	@Override
	public Mono<B> zrandmember(A key) {
		return Mono.from(this.connection(o -> o.zrandmember(key)));
	}

	@Override
	public Flux<B> zrandmember(A key, long count) {
		return Flux.from(this.connection(o -> o.zrandmember(key, count)));
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zrandmemberWithScores(A key, long count) {
		return Flux.from(this.connection(o -> o.zrandmemberWithScores(key, count)));
	}

	@Override
	public Flux<B> zrange(A key, long min, long max) {
		return Flux.from(this.connection(o -> o.zrange(key, min, max)));
	}

	@Override
	public SortedSetZrangeBuilder<A, B, Long> zrange() {
		return new SortedSetZrangeBuilderImpl<>(this.operations());
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zrangeWithScores(A key, long min, long max) {
		return Flux.from(this.connection(o -> o.zrangeWithScores(key, min, max)));
	}

	@Override
	public SortedSetZrangeWithScoresBuilder<A, B, Long> zrangeWithScores() {
		return new SortedSetZrangeWithScoresBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> zrangestore(A destination, A source, long min, long max) {
		return Mono.from(this.connection(o -> o.zrangestore(destination, source, min, max)));
	}

	@Override
	public SortedSetZrangestoreBuilder<A, B, Long> zrangestore() {
		return new SortedSetZrangestoreBuilderImpl<>(this.operations());
	}
	
	@Override
	public Mono<Long> zrank(A key, B member) {
		return Mono.from(this.connection(o -> o.zrank(key, member)));
	}

	@Override
	public Mono<Long> zrem(A key, B member) {
		return Mono.from(this.connection(o -> o.zrem(key, member)));
	}

	@Override
	public Mono<Long> zrem(A key, Consumer<Values<B>> members) {
		return Mono.from(this.connection(o -> o.zrem(key, members)));
	}

	@Override
	public Mono<Long> zremrangebylex(A key, Bound<? extends B> min, Bound<? extends B> max) {
		return Mono.from(this.connection(o -> o.zremrangebylex(key, min, max)));
	}

	@Override
	public Mono<Long> zremrangebyrank(A key, long start, long stop) {
		return Mono.from(this.connection(o -> o.zremrangebyrank(key, start, stop)));
	}

	@Override
	public Mono<Long> zremrangebyscore(A key, Bound<? extends Number> min, Bound<? extends Number> max) {
		return Mono.from(this.connection(o -> o.zremrangebyscore(key, min, max)));
	}

	@Override
	public Mono<Long> zrevrank(A key, B member) {
		return Mono.from(this.connection(o -> o.zrevrank(key, member)));
	}

	@Override
	public Mono<SortedSetScanResult<B>> zscan(A key, String cursor) {
		return Mono.from(this.connection(o -> o.zscan(key, cursor)));
	}

	@Override
	public SortedSetScanBuilder<A, B> zscan() {
		return new SortedSetScanBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Double> zscore(A key, B member) {
		return Mono.from(this.connection(o -> o.zscore(key, member)));
	}

	@Override
	public Flux<B> zunion(A key) {
		return Flux.from(this.connection(o -> o.zunion(key)));
	}

	@Override
	public Flux<B> zunion(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.zunion(keys)));
	}

	@Override
	public SortedSetZunionBuilder<A, B> zunion() {
		return new SortedSetZunionBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zunionWithScores(A key) {
		return Flux.from(this.connection(o -> o.zunionWithScores(key)));
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zunionWithScores(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.zunionWithScores(keys)));
	}

	@Override
	public SortedSetZunionWithScoresBuilder<A, B> zunionWithScores() {
		return new SortedSetZunionWithScoresBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Mono<Long> zunionstore(A destination, A key) {
		return Mono.from(this.connection(o -> o.zunionstore(destination, key)));
	}

	@Override
	public Mono<Long> zunionstore(A destination, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.zunionstore(destination, keys)));
	}

	@Override
	public SortedSetZunionstoreBuilder<A, B> zunionstore() {
		return new SortedSetZunionstoreBuilderImpl<>(this.operations(), this.keyType);
	}

	@Override
	public Mono<Long> xack(A key, A group, String messageId) {
		return Mono.from(this.connection(o -> o.xack(key, group, messageId)));
	}

	@Override
	public Mono<Long> xack(A key, A group, Consumer<StreamMessageIds> messageIds) {
		return Mono.from(this.connection(o -> o.xack(key, group, messageIds)));
	}

	@Override
	public Mono<String> xadd(A key, A field, B value) {
		return Mono.from(this.connection(o -> o.xadd(key, field, value)));
	}

	@Override
	public Mono<String> xadd(A key, Consumer<StreamEntries<A, B>> entries) {
		return Mono.from(this.connection(o -> o.xadd(key, entries)));
	}

	@Override
	public StreamXaddBuilder<A, B> xadd() {
		return new StreamXaddBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<StreamClaimedMessages<A, B>> xautoclaim(A key, A group, A consumer, long minIdleTime, String start) {
		return Mono.from(this.connection(o -> o.xautoclaim(key, group, consumer, minIdleTime, start)));
	}

	@Override
	public StreamXautoclaimBuilder<A, B> xautoclaim() {
		return new StreamXautoclaimBuilderImpl<>(this.operations());
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A key, A group, A consumer, long minIdleTime, String messageId) {
		return Flux.from(this.connection(o -> o.xclaim(key, group, consumer, minIdleTime, messageId)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A key, A group, A consumer, long minIdleTime, Consumer<StreamMessageIds> messageIds) {
		return Flux.from(this.connection(o -> o.xclaim(key, group, consumer, minIdleTime, messageIds)));
	}

	@Override
	public StreamXclaimBuilder<A, B> xclaim() {
		return new StreamXclaimBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> xdel(A key, String messageId) {
		return Mono.from(this.connection(o -> o.xdel(key, messageId)));
	}

	@Override
	public Mono<Long> xdel(A key, Consumer<StreamMessageIds> messageIds) {
		return Mono.from(this.connection(o -> o.xdel(key, messageIds)));
	}

	@Override
	public Mono<String> xgroupCreate(A key, A group, String id) {
		return Mono.from(this.connection(o -> o.xgroupCreate(key, group, id)));
	}

	@Override
	public StreamXgroupCreateBuilder<A> xgroupCreate() {
		return new StreamXgroupCreateBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Boolean> xgroupCreateconsumer(A key, A group, A consumer) {
		return Mono.from(this.connection(o -> o.xgroupCreateconsumer(key, group, consumer)));
	}

	@Override
	public Mono<Long> xgroupDelconsumer(A key, A group, A consumer) {
		return Mono.from(this.connection(o -> o.xgroupDelconsumer(key, group, consumer)));
	}

	@Override
	public Mono<Boolean> xgroupDestroy(A key, A group) {
		return Mono.from(this.connection(o -> o.xgroupDestroy(key, group)));
	}

	@Override
	public Mono<String> xgroupSetid(A key, A group, String id) {
		return Mono.from(this.connection(o -> o.xgroupSetid(key, group, id)));
	}

	@Override
	public Flux<Object> xinfoConsumers(A key, A group) {
		return Flux.from(this.connection(o -> o.xinfoConsumers(key, group)));
	}

	@Override
	public Flux<Object> xinfoGroups(A key) {
		return Flux.from(this.connection(o -> o.xinfoGroups(key)));
	}

	@Override
	public Flux<Object> xinfoStream(A key) {
		return Flux.from(this.connection(o -> o.xinfoStream(key)));
	}

	@Override
	public Flux<Object> xinfoStreamFull(A key) {
		return Flux.from(this.connection(o -> o.xinfoStreamFull(key)));
	}

	@Override
	public Flux<Object> xinfoStreamFull(A key, long count) {
		return Flux.from(this.connection(o -> o.xinfoStreamFull(key, count)));
	}

	@Override
	public Mono<Long> xlen(A key) {
		return Mono.from(this.connection(o -> o.xlen(key)));
	}

	@Override
	public Mono<StreamPendingMessages> xpending(A key, A group) {
		return Mono.from(this.connection(o -> o.xpending(key, group)));
	}

	@Override
	public Flux<StreamPendingMessage> xpendingExtended(A key, A group, String start, String end, long count) {
		return Flux.from(this.connection(o -> o.xpendingExtended(key, group, start, end, count)));
	}

	@Override
	public StreamXpendingExtendedBuilder<A> xpendingExtended() {
		return new StreamXpendingExtendedBuilderImpl<>(this.operations());
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A key, String start, String end) {
		return Flux.from(this.connection(o -> o.xrange(key, start, end)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A key, String start, String end, long count) {
		return Flux.from(this.connection(o -> o.xrange(key, start, end, count)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xread(A key, String messageId) {
		return Flux.from(this.connection(o -> o.xread(key, messageId)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xread(Consumer<StreamStreams<A>> streams) {
		return Flux.from(this.connection(o -> o.xread(streams)));
	}

	@Override
	public StreamXreadBuilder<A, B> xread() {
		return new StreamXreadBuilderImpl<>(this.operations());
	}

	@Override
	public Flux<StreamMessage<A, B>> xreadgroup(A group, A consumer, A key, String messageId) {
		return Flux.from(this.connection(o -> o.xreadgroup(group, consumer, key, messageId)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xreadgroup(A group, A consumer, Consumer<StreamStreams<A>> streams) {
		return Flux.from(this.connection(o -> o.xreadgroup(group, consumer, streams)));
	}

	@Override
	public StreamXreadgroupBuilder<A, B> xreadgroup() {
		return new StreamXreadgroupBuilderImpl<>(this.operations());
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A key, String start, String end) {
		return Flux.from(this.connection(o -> o.xrevrange(key, start, end)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A key, String start, String end, long count) {
		return Flux.from(this.connection(o -> o.xrevrange(key, start, end, count)));
	}

	@Override
	public Mono<Long> xtrimMaxLen(A key, long threshold) {
		return Mono.from(this.connection(o -> o.xtrimMaxLen(key, threshold)));
	}

	@Override
	public Mono<Long> xtrimMaxLen(A key, long threshold, long count) {
		return Mono.from(this.connection(o -> o.xtrimMaxLen(key, threshold, count)));
	}

	@Override
	public Mono<Long> xtrimMinId(A key, String streamId) {
		return Mono.from(this.connection(o -> o.xtrimMinId(key, streamId)));
	}

	@Override
	public Mono<Long> xtrimMinId(A key, String streamId, long count) {
		return Mono.from(this.connection(o -> o.xtrimMinId(key, streamId, count)));
	}

	@Override
	public StreamXtrimBuilder<A> xtrim() {
		return new StreamXtrimBuilderImpl<>(this.operations());
	}
	
	@Override
	public Mono<Long> append(A key, B value) {
		return Mono.from(this.connection(o -> o.append(key, value)));
	}

	@Override
	public Mono<Long> decr(A key) {
		return Mono.from(this.connection(o -> o.decr(key)));
	}

	@Override
	public Mono<Long> decrby(A key, long decrement) {
		return Mono.from(this.connection(o -> o.decrby(key, decrement)));
	}

	@Override
	public Mono<B> get(A key) {
		return Mono.from(this.connection(o -> o.get(key)));
	}

	@Override
	public Mono<B> getdel(A key) {
		return Mono.from(this.connection(o -> o.getdel(key)));
	}

	@Override
	public Mono<B> getex(A key) {
		return Mono.from(this.connection(o -> o.getex(key)));
	}

	@Override
	public StringGetexBuilder<A, B> getex() {
		return new StringGetexBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<B> getrange(A key, long start, long end) {
		return Mono.from(this.connection(o -> o.getrange(key, start, end)));
	}

	@Override
	public Mono<B> getset(A key, B value) {
		return Mono.from(this.connection(o -> o.getset(key, value)));
	}

	@Override
	public Mono<Long> incr(A key) {
		return Mono.from(this.connection(o -> o.incr(key)));
	}

	@Override
	public Mono<Long> incrby(A key, long increment) {
		return Mono.from(this.connection(o -> o.incrby(key, increment)));
	}

	@Override
	public Mono<Double> incrbyfloat(A key, double increment) {
		return Mono.from(this.connection(o -> o.incrbyfloat(key, increment)));
	}

	@Override
	public Flux<EntryOptional<A, B>> mget(Consumer<Keys<A>> keys) {
		return Flux.from(this.connection(o -> o.mget(keys)));
	}

	@Override
	public Mono<String> mset(Consumer<Entries<A, B>> entries) {
		return Mono.from(this.connection(o -> o.mset(entries)));
	}

	@Override
	public Mono<Boolean> msetnx(Consumer<Entries<A, B>> entries) {
		return Mono.from(this.connection(o -> o.msetnx(entries)));
	}

	@Override
	public Mono<String> psetex(A key, long milliseconds, B value) {
		return Mono.from(this.connection(o -> o.psetex(key, milliseconds, value)));
	}

	@Override
	public Mono<String> set(A key, B value) {
		return Mono.from(this.connection(o -> o.set(key, value)));
	}

	@Override
	public StringSetBuilder<A, B> set() {
		return new StringSetBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<B> setGet(A key, B value) {
		return Mono.from(this.connection(o -> o.setGet(key, value)));
	}

	@Override
	public StringSetGetBuilder<A, B> setGet() {
		return new StringSetGetBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<String> setex(A key, long seconds, B value) {
		return Mono.from(this.connection(o -> o.setex(key, seconds, value)));
	}

	@Override
	public Mono<Boolean> setnx(A key, B value) {
		return Mono.from(this.connection(o -> o.setnx(key, value)));
	}

	@Override
	public Mono<Long> setrange(A key, long offset, B value) {
		return Mono.from(this.connection(o -> o.setrange(key, offset, value)));
	}

	@Override
	public Mono<Long> strlen(A key) {
		return Mono.from(this.connection(o -> o.strlen(key)));
	}

	@Override
	public Mono<Long> bitcount(A key) {
		return Mono.from(this.connection(o -> o.bitcount(key)));
	}

	@Override
	public Mono<Long> bitcount(A key, long start, long end) {
		return Mono.from(this.connection(o -> o.bitcount(key, start, end)));
	}

	@Override
	public Flux<Optional<Long>> bitfield(A key) {
		return Flux.from(this.connection(o -> o.bitfield(key)));
	}

	@Override
	public StringBitfieldBuilder<A, B> bitfield() {
		return new StringBitfieldBuilderImpl<>(this.operations());
	}

	@Override
	public Mono<Long> bitopAnd(A destKey, A key) {
		return Mono.from(this.connection(o -> o.bitopAnd(destKey, key)));
	}

	@Override
	public Mono<Long> bitopAnd(A destKey, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.bitopAnd(destKey, keys)));
	}

	@Override
	public Mono<Long> bitopOr(A destKey, A key) {
		return Mono.from(this.connection(o -> o.bitopOr(destKey, key)));
	}

	@Override
	public Mono<Long> bitopOr(A destKey, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.bitopOr(destKey, keys)));
	}

	@Override
	public Mono<Long> bitopXor(A destKey, A key) {
		return Mono.from(this.connection(o -> o.bitopXor(destKey, key)));
	}

	@Override
	public Mono<Long> bitopXor(A destKey, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.bitopXor(destKey, keys)));
	}

	@Override
	public Mono<Long> bitopNot(A destKey, A key) {
		return Mono.from(this.connection(o -> o.bitopNot(destKey, key)));
	}

	@Override
	public Mono<Long> bitopNot(A destKey, Consumer<Keys<A>> keys) {
		return Mono.from(this.connection(o -> o.bitopNot(destKey, keys)));
	}

	@Override
	public Mono<Long> bitpos(A key, boolean bit) {
		return Mono.from(this.connection(o -> o.bitpos(key, bit)));
	}

	@Override
	public Mono<Long> bitpos(A key, boolean bit, long start) {
		return Mono.from(this.connection(o -> o.bitpos(key, bit, start)));
	}

	@Override
	public Mono<Long> bitpos(A key, boolean bit, long start, long end) {
		return Mono.from(this.connection(o -> o.bitpos(key, bit, start, end)));
	}

	@Override
	public Mono<Long> getbit(A key, long offset) {
		return Mono.from(this.connection(o -> o.getbit(key, offset)));
	}

	@Override
	public Mono<Long> setbit(A key, long offset, int value) {
		return Mono.from(this.connection(o -> o.setbit(key, offset, value)));
	}
}

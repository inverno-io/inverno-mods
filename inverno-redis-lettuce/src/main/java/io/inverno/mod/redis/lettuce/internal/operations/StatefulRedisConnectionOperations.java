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

import io.inverno.mod.redis.RedisOperations;
import io.inverno.mod.redis.operations.Bound;
import io.inverno.mod.redis.operations.Entries;
import io.inverno.mod.redis.operations.EntryOptional;
import io.inverno.mod.redis.operations.Keys;
import io.inverno.mod.redis.operations.Values;
import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.SortArgs;
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.XTrimArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands;
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
import io.lettuce.core.api.reactive.RedisHLLReactiveCommands;
import io.lettuce.core.api.reactive.RedisHashReactiveCommands;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.api.reactive.RedisListReactiveCommands;
import io.lettuce.core.api.reactive.RedisScriptingReactiveCommands;
import io.lettuce.core.api.reactive.RedisSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.support.AsyncPool;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
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
public class StatefulRedisConnectionOperations<A, B, C extends StatefulConnection<A, B>, D extends BaseRedisReactiveCommands<A, B> & RedisGeoReactiveCommands<A, B> & RedisHashReactiveCommands<A, B> & RedisHLLReactiveCommands<A, B> & RedisKeyReactiveCommands<A, B> & RedisListReactiveCommands<A, B> & RedisScriptingReactiveCommands<A, B> & RedisSetReactiveCommands<A, B> & RedisSortedSetReactiveCommands<A, B> & RedisStreamReactiveCommands<A, B> & RedisStringReactiveCommands<A, B>>
	implements RedisOperations<A, B> {
	
	protected final C connection;
	
	protected final D commands;
	
	protected final AsyncPool<C> pool;
	
	protected final Class<A> keyType;
	
	protected final Class<B> valueType;
	
	/**
	 * 
	 * @param connection
	 * @param commands
	 * @param pool
	 * @param keyType
	 * @param valueType 
	 */
	public StatefulRedisConnectionOperations(C connection, D commands, AsyncPool<C> pool, Class<A> keyType, Class<B> valueType) {
		this.connection = connection;
		this.commands = commands;
		this.pool = pool;
		this.keyType = keyType;
		this.valueType = valueType;
	}
	
	/**
	 * 
	 * @return 
	 */
	public Mono<Void> close() {
		return Mono.fromCompletionStage(this.pool.release(this.connection));
	}
	
	/**
	 * 
	 * @return 
	 */
	public D getCommands() {
		return this.commands;
	}

	@Override
	public Mono<Long> geoadd(A key, double longitude, double latitude, B member) {
		return this.commands.geoadd(key, latitude, latitude, member);
	}

	@Override
	public Mono<Long> geoadd(A key, Consumer<GeoItems<B>> items) {
		GeoItemsImpl<B> itemsConfigurator = new GeoItemsImpl<>();
		items.accept(itemsConfigurator);
		return this.commands.geoadd(key, itemsConfigurator.getValues());
	}

	@Override
	public GeoaddBuilder<A, B> geoadd() {
		return new GeoaddBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Double> geodist(A key, B member1, B member2, GeoUnit unit) {
		return this.commands.geodist(key, member1, member2, GeoUtils.convertUnit(unit));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Mono<Optional<String>> geohash(A key, B member) {
		return this.commands.geohash(key, member).map(value -> Optional.ofNullable(value.getValueOrElse(null))).single();
	}

	@Override
	public Flux<Optional<String>> geohash(A key, Consumer<Values<B>> members) {
		ValuesImpl<B> valuesConfigurator = new ValuesImpl<>(this.valueType);
		members.accept(valuesConfigurator);
		return this.commands.geohash(key, valuesConfigurator.getValues()).map(value -> Optional.ofNullable(value.getValueOrElse(null)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Optional<GeoCoordinates>> geopos(A key, B member) {
		return this.commands.geopos(key, member).map(value -> Optional.ofNullable(value.map(coord -> (GeoCoordinates)new GeoCoordinatesImpl(coord.getX().doubleValue(), coord.getY().doubleValue())).getValueOrElse(null))).single();
	}

	@Override
	public Flux<Optional<GeoCoordinates>> geopos(A key, Consumer<Values<B>> members) {
		ValuesImpl<B> valuesConfigurator = new ValuesImpl<>(this.valueType);
		members.accept(valuesConfigurator);
		return this.commands.geopos(key, valuesConfigurator.getValues()).map(value -> Optional.ofNullable(value.map(coord -> (GeoCoordinates)new GeoCoordinatesImpl(coord.getX().doubleValue(), coord.getY().doubleValue())).getValueOrElse(null)));
	}

	@Override
	public GeoradiusBuilder<A, B> georadius() {
		return new GeoradiusBuilderImpl<>(this.commands);
	}

	@Override
	public GeoradiusExtendedBuilder<A, B> georadiusExtended() {
		return new GeoradiusExtendedBuilderImpl<>(this.commands);
	}

	@Override
	public GeoradiusStoreBuilder<A, B> georadiusStore() {
		return new GeoradiusStoreBuilderImpl<>(this.commands);
	}

	@Override
	public GeoradiusbymemberBuilder<A, B> georadiusbymember() {
		return new GeoradiusbymemberBuilderImpl<>(this.commands);
	}

	@Override
	public GeoradiusbymemberExtendedBuilder<A, B> georadiusbymemberExtended() {
		return new GeoradiusbymemberExtendedBuilderImpl<>(this.commands);
	}

	@Override
	public GeoradiusbymemberStoreBuilder<A, B> georadiusbymemberStore() {
		return new GeoradiusbymemberStoreBuilderImpl<>(this.commands);
	}

	@Override
	public GeosearchBuilder<A, B> geosearch() {
		return new GeosearchBuilderImpl<>(this.commands);
	}

	@Override
	public GeosearchExtendedBuilder<A, B> geosearchExtended() {
		return new GeosearchExtendedBuilderImpl<>(this.commands);
	}

	@Override
	public GeosearchstoreBuilder<A, B> geosearchstore() {
		return new GeosearchstoreBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> hdel(A key, A field) {
		return this.commands.hdel(key, field);
	}

	@Override
	public Mono<Long> hdel(A key, Consumer<Keys<A>> fields) {
		KeysImpl<A> fieldsConfigurator = new KeysImpl<>(this.keyType);
		fields.accept(fieldsConfigurator);
		return this.commands.hdel(key, fieldsConfigurator.getKeys());
	}

	@Override
	public Mono<Boolean> hexists(A key, A field) {
		return this.commands.hexists(key, field);
	}

	@Override
	public Mono<B> hget(A key, A field) {
		return this.commands.hget(key, field);
	}

	@Override
	public Flux<EntryOptional<A, B>> hgetall(A key) {
		return this.commands.hgetall(key).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	public Mono<Long> hincrby(A key, A field, long increment) {
		return this.commands.hincrby(key, field, increment);
	}

	@Override
	public Mono<Double> hincrbyfloat(A key, A field, double increment) {
		return this.commands.hincrbyfloat(key, field, increment);
	}

	@Override
	public Flux<A> hkeys(A key) {
		return this.commands.hkeys(key);
	}

	@Override
	public Mono<Long> hlen(A key) {
		return this.commands.hlen(key);
	}

	@Override
	public Flux<EntryOptional<A, B>> hmget(A key, Consumer<Keys<A>> fields) {
		KeysImpl<A> fieldsConfigurator = new KeysImpl<>(this.keyType);
		fields.accept(fieldsConfigurator);
		return this.commands.hmget(key, fieldsConfigurator.getKeys()).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	public Mono<String> hmset(A key, Consumer<Entries<A, B>> entries) {
		EntriesImpl<A, B> entriesConfigurator = new EntriesImpl<>();
		entries.accept(entriesConfigurator);
		return this.commands.hmset(key, entriesConfigurator.getEntries());
	}

	@Override
	public Mono<A> hrandfield(A key) {
		return this.commands.hrandfield(key);
	}

	@Override
	public Flux<A> hrandfield(A key, long count) {
		return this.commands.hrandfield(key, count);
	}

	@Override
	public Flux<EntryOptional<A, B>> hrandfieldWithvalues(A key, long count) {
		return this.commands.hrandfieldWithvalues(key, count).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	public Mono<HashScanResult<A, B>> hscan(A key, String cursor) {
		return this.commands.hscan(key, ScanCursor.of(cursor)).map(HashScanResultImpl::new);
	}

	@Override
	public HashScanBuilder<A, B> hscan() {
		return new HashScanBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Boolean> hset(A key, A field, B value) {
		return this.commands.hset(key, field, value);
	}

	@Override
	public Mono<Long> hset(A key, Consumer<Entries<A, B>> entries) {
		EntriesImpl<A, B> entriesConfigurator = new EntriesImpl<>();
		entries.accept(entriesConfigurator);
		return this.commands.hset(key, entriesConfigurator.getEntries());
	}

	@Override
	public Mono<Boolean> hsetnx(A key, A field, B value) {
		return this.commands.hsetnx(key, field, value);
	}

	@Override
	public Mono<Long> hstrlen(A key, A field) {
		return this.commands.hstrlen(key, field);
	}

	@Override
	public Flux<B> hvals(A key) {
		return this.commands.hvals(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> pfadd(A key, B value) {
		return this.commands.pfadd(key, value);
	}

	@Override
	public Mono<Long> pfadd(A key, Consumer<Values<B>> values) {
		ValuesImpl<B> valuesConfigurator = new ValuesImpl<>(this.valueType);
		values.accept(valuesConfigurator);
		return this.commands.pfadd(key, valuesConfigurator.getValues());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> pfcount(A key) {
		return this.commands.pfcount(key);
	}

	@Override
	public Mono<Long> pfcount(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.pfcount(keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<String> pfmerge(A destkey, A sourcekey) {
		return this.commands.pfmerge(destkey, sourcekey);
	}

	@Override
	public Mono<String> pfmerge(A destkey, Consumer<Keys<A>> sourcekeys) {
		KeysImpl<A> sourcekeysConfigurator = new KeysImpl<>(this.keyType);
		sourcekeys.accept(sourcekeysConfigurator);
		return this.commands.pfmerge(destkey, sourcekeysConfigurator.getKeys());
	}

	@Override
	public Mono<Boolean> copy(A source, A destination) {
		return this.commands.copy(source, destination);
	}

	@Override
	public KeyCopyBuilder<A> copy() {
		return new KeyCopyBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> del(A key) {
		return this.commands.del(key);
	}

	@Override
	public Mono<Long> del(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.del(keysConfigurator.getKeys());
	}

	@Override
	public Mono<byte[]> dump(A key) {
		return this.commands.dump(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> exists(A key) {
		return this.commands.exists(key);
	}

	@Override
	public Mono<Long> exists(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.exists(keysConfigurator.getKeys());
	}

	@Override
	public Mono<Boolean> expire(A key, long seconds) {
		return this.commands.expire(key, seconds);
	}

	@Override
	public Mono<Boolean> expire(A key, Duration duration) {
		return this.commands.expire(key, duration);
	}

	@Override
	public KeyExpireBuilder<A> expire() {
		return new KeyExpireBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Boolean> expireat(A key, long epochSeconds) {
		return this.commands.expireat(key, epochSeconds);
	}

	@Override
	public Mono<Boolean> expireat(A key, ZonedDateTime datetime) {
		return this.commands.expireat(key, datetime.toInstant());
	}

	@Override
	public Mono<Boolean> expireat(A key, Instant instant) {
		return this.commands.expireat(key, instant);
	}

	@Override
	public KeyExpireatBuilder<A> expireat() {
		return new KeyExpireatBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> expiretime(A key) {
		throw new UnsupportedOperationException("Implementation doesn't support EXPIRETIME key");
	}

	@Override
	public Flux<A> keys(A pattern) {
		return this.commands.keys(pattern);
	}

	@Override
	public Mono<String> migrate(String host, int port, A key, int db, long timeout) {
		return this.commands.migrate(host, port, key, db, timeout);
	}

	@Override
	public KeyMigrateBuilder<A> migrate() {
		return new KeyMigrateBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	public Mono<Boolean> move(A key, int db) {
		return this.commands.move(key, db);
	}

	@Override
	public Mono<String> objectEncoding(A key) {
		return this.commands.objectEncoding(key);
	}

	@Override
	public Mono<Long> objectFreq(A key) {
		return this.commands.objectFreq(key);
	}

	@Override
	public Mono<Long> objectIdletime(A key) {
		return this.commands.objectIdletime(key);
	}

	@Override
	public Mono<Long> objectRefcount(A key) {
		return this.commands.objectRefcount(key);
	}

	@Override
	public Mono<Boolean> persist(A key) {
		return this.commands.persist(key);
	}

	@Override
	public Mono<Boolean> pexpire(A key, long milliseconds) {
		return this.commands.pexpire(key, milliseconds);
	}

	@Override
	public Mono<Boolean> pexpire(A key, Duration duration) {
		return this.commands.pexpire(key, duration);
	}

	@Override
	public KeyPexpireBuilder<A> pexpire() {
		return new KeyPexpireBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Boolean> pexpireat(A key, long epochMilliseconds) {
		return this.commands.pexpireat(key, epochMilliseconds);
	}

	@Override
	public Mono<Boolean> pexpireat(A key, ZonedDateTime datetime) {
		return this.commands.pexpireat(key, datetime.toInstant());
	}

	@Override
	public Mono<Boolean> pexpireat(A key, Instant instant) {
		return this.commands.pexpireat(key, instant);
	}

	@Override
	public KeyPexpireatBuilder<A> pexpireat() {
		return new KeyPexpireatBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> pexpiretime(A key) {
		throw new UnsupportedOperationException("Implementation doesn't support PEXPIRETIME key");
	}

	@Override
	public Mono<Long> pttl(A key) {
		return this.commands.pttl(key);
	}

	@Override
	public Mono<A> randomkey() {
		return this.commands.randomkey();
	}

	@Override
	public Mono<String> rename(A key, A newkey) {
		return this.commands.rename(key, newkey);
	}

	@Override
	public Mono<Boolean> renamenx(A key, A newkey) {
		return this.commands.renamenx(key, newkey);
	}

	@Override
	public Mono<String> restore(A key, long ttl, byte[] serializedValue) {
		return this.commands.restore(key, ttl, serializedValue);
	}

	@Override
	public KeyRestoreBuilder<A> restore() {
		return new KeyRestoreBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<KeyScanResult<A>> scan(String cursor) {
		return this.commands.scan(ScanCursor.of(cursor)).map(KeyScanResultImpl::new);
	}

	@Override
	public KeyScanBuilder<A> scan() {
		return new KeyScanBuilderImpl<>(this.commands);
	}

	@Override
	public Flux<B> sort(A key) {
		return this.commands.sort(key);
	}

	@Override
	public KeySortBuilder<A, B> sort() {
		return new KeySortBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> sortStore(A key, A destination) {
		return this.commands.sortStore(key, SortArgs.Builder.limit(0, Long.MAX_VALUE), destination);
	}

	@Override
	public KeySortStoreBuilder<A> sortStore() {
		return new KeySortStoreBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> touch(A key) {
		return this.commands.touch(key);
	}

	@Override
	public Mono<Long> touch(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.touch(keysConfigurator.getKeys());
	}

	@Override
	public Mono<Long> ttl(A key) {
		return this.commands.ttl(key);
	}

	@Override
	public Mono<String> type(A key) {
		return this.commands.type(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> unlink(A key) {
		return this.commands.unlink(key);
	}

	@Override
	public Mono<Long> unlink(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.unlink(keysConfigurator.getKeys());
	}

	@Override
	public Mono<Long> waitForReplication(int replicas, long timeout) {
		return this.commands.waitForReplication(replicas, timeout);
	}

	@Override
	public ListBlmoveBuilder<A, B> blmove() {
		return new ListBlmoveBuilderImpl<>(this.commands);
	}

	@Override
	public ListBlmpopBuilder<A, B> blmpop() {
		return new ListBlmpopBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<EntryOptional<A, B>> blpop(A key, double timeout) {
		return this.commands.blpop(timeout, key).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	public Mono<EntryOptional<A, B>> blpop(Consumer<Keys<A>> keys, double timeout) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.blpop(timeout, keysConfigurator.getKeys()).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<EntryOptional<A, B>> brpop(A key, double timeout) {
		return this.commands.brpop(timeout, key).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	public Mono<EntryOptional<A, B>> brpop(Consumer<Keys<A>> keys, double timeout) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.brpop(timeout, keysConfigurator.getKeys()).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	public Mono<B> brpoplpush(A source, A destination, double timeout) {
		return this.commands.brpoplpush(timeout, source, destination);
	}

	@Override
	public Mono<B> lindex(A key, long index) {
		return this.commands.lindex(key, index);
	}

	@Override
	public Mono<Long> linsert(A key, boolean before, B pivot, B element) {
		return this.commands.linsert(key, before, pivot, element);
	}

	@Override
	public Mono<Long> llen(A key) {
		return this.commands.llen(key);
	}

	@Override
	public ListLmoveBuilder<A, B> lmove() {
		return new ListLmoveBuilderImpl<>(this.commands);
	}

	@Override
	public ListLmpopBuilder<A, B> lmpop() {
		return new ListLmpopBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<B> lpop(A key) {
		return this.commands.lpop(key);
	}

	@Override
	public Flux<B> lpop(A key, long count) {
		return this.commands.lpop(key, count);
	}

	@Override
	public Mono<Long> lpos(A key, B element) {
		return this.commands.lpos(key, element);
	}

	@Override
	public Flux<Long> lpos(A key, B element, long count) {
		return this.commands.lpos(key, element, (int)count);
	}

	@Override
	public ListLposBuilder<A, B> lpos() {
		return new ListLposBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> lpush(A key, B element) {
		return this.commands.lpush(key, element);
	}

	@Override
	public Mono<Long> lpush(A key, Consumer<Values<B>> elements) {
		ValuesImpl<B> elementsConfigurator = new ValuesImpl<>(this.valueType);
		elements.accept(elementsConfigurator);
		return this.commands.lpush(key, elementsConfigurator.getValues());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> lpushx(A key, B element) {
		return this.commands.lpushx(key, element);
	}

	@Override
	public Mono<Long> lpushx(A key, Consumer<Values<B>> elements) {
		ValuesImpl<B> elementsConfigurator = new ValuesImpl<>(this.valueType);
		elements.accept(elementsConfigurator);
		return this.commands.lpushx(key, elementsConfigurator.getValues());
	}

	@Override
	public Flux<B> lrange(A key, long start, long stop) {
		return this.commands.lrange(key, start, stop);
	}

	@Override
	public Mono<Long> lrem(A key, long count, B element) {
		return this.commands.lrem(key, count, element);
	}

	@Override
	public Mono<String> lset(A key, long index, B element) {
		return this.commands.lset(key, index, element);
	}

	@Override
	public Mono<String> ltrim(A key, long start, long stop) {
		return this.commands.ltrim(key, start, stop);
	}

	@Override
	public Mono<B> rpop(A key) {
		return this.commands.rpop(key);
	}

	@Override
	public Flux<B> rpop(A key, long count) {
		return this.commands.rpop(key, count);
	}

	@Override
	public Mono<B> rpoplpush(A source, A destination) {
		return this.commands.rpoplpush(source, destination);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> rpush(A key, B element) {
		return this.commands.rpush(key, element);
	}

	@Override
	public Mono<Long> rpush(A key, Consumer<Values<B>> elements) {
		ValuesImpl<B> elementsConfigurator = new ValuesImpl<>(this.valueType);
		elements.accept(elementsConfigurator);
		return this.commands.rpush(key, elementsConfigurator.getValues());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> rpushx(A key, B element) {
		return this.commands.rpushx(key, element);
	}

	@Override
	public Mono<Long> rpushx(A key, Consumer<Values<B>> elements) {
		ValuesImpl<B> elementsConfigurator = new ValuesImpl<>(this.valueType);
		elements.accept(elementsConfigurator);
		return this.commands.rpushx(key, elementsConfigurator.getValues());
	}

	@Override
	public String digest(byte[] script) {
		return this.commands.digest(script);
	}

	@Override
	public String digest(String script) {
		return this.commands.digest(script);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Flux<T> eval(String script, ScriptOutput output) {
		return this.commands.eval(script, ScriptUtils.convertOutput(output));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Flux<T> eval(byte[] script, ScriptOutput output) {
		return this.commands.eval(script, ScriptUtils.convertOutput(output));
	}

	@Override
	public <T> Flux<T> eval(String script, ScriptOutput output, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.eval(script, ScriptUtils.convertOutput(output), keysConfigurator.getKeys());
	}

	@Override
	public <T> Flux<T> eval(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.eval(script, ScriptUtils.convertOutput(output), keysConfigurator.getKeys());
	}

	@Override
	public <T> Flux<T> eval(String script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		
		ValuesImpl<B> argsConfigurator = new ValuesImpl<>(this.valueType);
		args.accept(argsConfigurator);
		
		return this.commands.eval(script, ScriptUtils.convertOutput(output), keysConfigurator.getKeys(), argsConfigurator.getValues());
	}

	@Override
	public <T> Flux<T> eval(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		
		ValuesImpl<B> argsConfigurator = new ValuesImpl<>(this.valueType);
		args.accept(argsConfigurator);
		
		return this.commands.eval(script, ScriptUtils.convertOutput(output), keysConfigurator.getKeys(), argsConfigurator.getValues());
	}

	@Override
	public <T> Flux<T> eval_ro(String script, ScriptOutput output) {
		throw new UnsupportedOperationException("Implementation doesn't support EVAL_RO script numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public <T> Flux<T> eval_ro(byte[] script, ScriptOutput output) {
		throw new UnsupportedOperationException("Implementation doesn't support EVAL_RO script numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public <T> Flux<T> eval_ro(String script, ScriptOutput output, Consumer<Keys<A>> keys) {
		throw new UnsupportedOperationException("Implementation doesn't support EVAL_RO script numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public <T> Flux<T> eval_ro(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys) {
		throw new UnsupportedOperationException("Implementation doesn't support EVAL_RO script numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public <T> Flux<T> eval_ro(String script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		throw new UnsupportedOperationException("Implementation doesn't support EVAL_RO script numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public <T> Flux<T> eval_ro(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		throw new UnsupportedOperationException("Implementation doesn't support EVAL_RO script numkeys key [key ...] arg [arg ...]");
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Flux<T> evalsha(String digest, ScriptOutput output) {
		return this.commands.evalsha(digest, ScriptUtils.convertOutput(output));
	}

	@Override
	public <T> Flux<T> evalsha(String digest, ScriptOutput output, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.evalsha(digest, ScriptUtils.convertOutput(output), keysConfigurator.getKeys());
	}

	@Override
	public <T> Flux<T> evalsha(String digest, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		
		ValuesImpl<B> argsConfigurator = new ValuesImpl<>(this.valueType);
		args.accept(argsConfigurator);
		
		return this.commands.evalsha(digest, ScriptUtils.convertOutput(output), keysConfigurator.getKeys(), argsConfigurator.getValues());
	}

	@Override
	public <T> Flux<T> evalsha_ro(String digest, ScriptOutput output) {
		throw new UnsupportedOperationException("Implementation doesn't support EVALSHA_RO sha1 numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public <T> Flux<T> evalsha_ro(String digest, ScriptOutput output, Consumer<Keys<A>> keys) {
		throw new UnsupportedOperationException("Implementation doesn't support EVALSHA_RO sha1 numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public <T> Flux<T> evalsha_ro(String digest, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args) {
		throw new UnsupportedOperationException("Implementation doesn't support EVALSHA_RO sha1 numkeys key [key ...] arg [arg ...]");
	}

	@Override
	public Flux<Boolean> scriptExists(String... digests) {
		return this.commands.scriptExists(digests);
	}

	@Override
	public Mono<String> scriptFlush() {
		return this.commands.scriptFlush();
	}

	@Override
	public Mono<String> scriptFlush(ScriptFlushMode flushMode) {
		return this.commands.scriptFlush(ScriptUtils.convertFlushMode(flushMode));
	}

	@Override
	public Mono<String> scriptKill() {
		return this.commands.scriptKill();
	}

	@Override
	public Mono<String> scriptLoad(String script) {
		return this.commands.scriptLoad(script);
	}

	@Override
	public Mono<String> scriptLoad(byte[] script) {
		return this.commands.scriptLoad(script);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> sadd(A key, B member) {
		return this.commands.sadd(key, member);
	}

	@Override
	public Mono<Long> sadd(A key, Consumer<Values<B>> members) {
		ValuesImpl<B> membersConfigurator = new ValuesImpl<>(this.valueType);
		members.accept(membersConfigurator);
		return this.commands.sadd(key, membersConfigurator.getValues());
	}

	@Override
	public Mono<Long> scard(A key) {
		return this.commands.scard(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<B> sdiff(A key) {
		return this.commands.sdiff(key);
	}

	@Override
	public Flux<B> sdiff(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.sdiff(keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> sdiffstore(A destination, A key) {
		return this.commands.sdiffstore(destination, key);
	}

	@Override
	public Mono<Long> sdiffstore(A destination, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.sdiffstore(destination, keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<B> sinter(A key) {
		return this.commands.sinter(key);
	}

	@Override
	public Flux<B> sinter(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.sinter(keysConfigurator.getKeys());
	}

	@Override
	public Mono<Long> sintercard(A key) {
		throw new UnsupportedOperationException("Implementation doesn't support SINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	public Mono<Long> sintercard(A key, long limit) {
		throw new UnsupportedOperationException("Implementation doesn't support SINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	public Mono<Long> sintercard(Consumer<Keys<A>> keys) {
		throw new UnsupportedOperationException("Implementation doesn't support SINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	public Mono<Long> sintercard(Consumer<Keys<A>> keys, long limit) {
		throw new UnsupportedOperationException("Implementation doesn't support SINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> sinterstore(A destination, A key) {
		return this.commands.sinterstore(destination, key);
	}

	@Override
	public Mono<Long> sinterstore(A destination, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.sinterstore(destination, keysConfigurator.getKeys());
	}

	@Override
	public Mono<Boolean> sismember(A key, B member) {
		return this.commands.sismember(key, member);
	}

	@Override
	public Flux<B> smembers(A key) {
		return this.commands.smembers(key);
	}

	@Override
	public Flux<Boolean> smismember(A key, Consumer<Values<B>> members) {
		ValuesImpl<B> membersConfigurator = new ValuesImpl<>(this.valueType);
		members.accept(membersConfigurator);
		return this.commands.smismember(key, membersConfigurator.getValues());
	}

	@Override
	public Mono<Boolean> smove(A source, A destination, B member) {
		return this.commands.smove(source, destination, member);
	}

	@Override
	public Mono<B> spop(A key) {
		return this.commands.spop(key);
	}

	@Override
	public Flux<B> spop(A key, long count) {
		return this.commands.spop(key, count);
	}

	@Override
	public Mono<B> srandmember(A key) {
		return this.commands.srandmember(key);
	}

	@Override
	public Flux<B> srandmember(A key, long count) {
		return this.commands.srandmember(key, count);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> srem(A key, B member) {
		return this.commands.srem(key, member);
	}

	@Override
	public Mono<Long> srem(A key, Consumer<Values<B>> members) {
		ValuesImpl<B> membersConfigurator = new ValuesImpl<>(this.valueType);
		members.accept(membersConfigurator);
		return this.commands.srem(key, membersConfigurator.getValues());
	}

	@Override
	public Mono<SetScanResult<B>> sscan(A key, String cursor) {
		return this.commands.sscan(key, ScanCursor.of(cursor)).map(SetScanResultImpl::new);
	}

	@Override
	public SetScanBuilder<A, B> sscan() {
		return new SetScanBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<B> sunion(A key) {
		return this.commands.sunion(key);
	}

	@Override
	public Flux<B> sunion(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.sunion(keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> sunionstore(A destination, A key) {
		return this.commands.sunionstore(destination, key);
	}

	@Override
	public Mono<Long> sunionstore(A destination, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.sunionstore(destination, keysConfigurator.getKeys());
	}

	@Override
	public SortedSetBzmpopBuilder<A, B> bzmpop() {
		return new SortedSetBzmpopBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmax(double timeout, A key) {
		return this.commands.bzpopmax(timeout, key).map(kv -> EntryOptional.ofNullable(kv.getKey(), (SortedSetScoredMember<B>)new SortedSetScoredMemberImpl<>(kv.getValue()))).single();
	}

	@Override
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmax(double timeout, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.bzpopmax(timeout, keysConfigurator.getKeys()).map(kv -> EntryOptional.ofNullable(kv.getKey(), (SortedSetScoredMember<B>)new SortedSetScoredMemberImpl<>(kv.getValue())));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmin(double timeout, A key) {
		return this.commands.bzpopmin(timeout, key).map(kv -> EntryOptional.ofNullable(kv.getKey(), (SortedSetScoredMember<B>)new SortedSetScoredMemberImpl<>(kv.getValue()))).single();
	}

	@Override
	public Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmin(double timeout, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.bzpopmin(timeout, keysConfigurator.getKeys()).map(kv -> EntryOptional.ofNullable(kv.getKey(), (SortedSetScoredMember<B>)new SortedSetScoredMemberImpl<>(kv.getValue())));
	}

	@Override
	public Mono<Long> zadd(A key, double score, B member) {
		return this.commands.zadd(key, score, member);
	}

	@Override
	public Mono<Long> zadd(A key, Consumer<SortedSetScoredMembers<B>> members) {
		SortedSetScoredMembersImpl<B> membersConfigurator = new SortedSetScoredMembersImpl<>();
		members.accept(membersConfigurator);
		return this.commands.zadd(key, membersConfigurator.getScoredValues());
	}

	@Override
	public SortedSetZaddBuilder<A, B> zadd() {
		return new SortedSetZaddBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Double> zaddIncr(A key, double score, B member) {
		return this.commands.zaddincr(key, score, member);
	}

	@Override
	public SortedSetZaddIncrBuilder<A, B> zaddIncr() {
		return new SortedSetZaddIncrBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> zcard(A key) {
		return this.commands.zcard(key);
	}

	@Override
	public Mono<Long> zcount(A key, Bound<? extends Number> min, Bound<? extends Number> max) {
		return this.commands.zcount(key, SortedSetUtils.convertRange(min, max));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<B> zdiff(A key) {
		return this.commands.zdiff(key);
	}

	@Override
	public Flux<B> zdiff(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zdiff(keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SortedSetScoredMember<B>> zdiffWithScores(A key) {
		return this.commands.zdiffWithScores(key).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zdiffWithScores(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zdiffWithScores(keysConfigurator.getKeys()).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> zdiffstore(A destination, A key) {
		return this.commands.zdiffstore(destination, key);
	}

	@Override
	public Mono<Long> zdiffstore(A destination, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zdiffstore(destination, keysConfigurator.getKeys());
	}

	@Override
	public Mono<Double> zincrby(A key, double increment, B member) {
		return this.commands.zincrby(key, increment, member);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<B> zinter(A key) {
		return this.commands.zinter(key);
	}

	@Override
	public Flux<B> zinter(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zinter(keysConfigurator.getKeys());
	}

	@Override
	public SortedSetZinterBuilder<A, B> zinter() {
		return new SortedSetZinterBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SortedSetScoredMember<B>> zinterWithScores(A key) {
		return this.commands.zinterWithScores(key).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zinterWithScores(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zinterWithScores(keysConfigurator.getKeys()).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public SortedSetZinterWithScoresBuilder<A, B> zinterWithScores() {
		return new SortedSetZinterWithScoresBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	public Mono<Long> zintercard(A key) {
		throw new UnsupportedOperationException("Implementation doesn't support ZINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	public Mono<Long> zintercard(A key, long limit) {
		throw new UnsupportedOperationException("Implementation doesn't support ZINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	public Mono<Long> zintercard(Consumer<Keys<A>> keys) {
		throw new UnsupportedOperationException("Implementation doesn't support ZINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	public Mono<Long> zintercard(Consumer<Keys<A>> keys, long limit) {
		throw new UnsupportedOperationException("Implementation doesn't support ZINTERCARD numkeys key [key ...] [LIMIT limit]");
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> zinterstore(A destination, A key) {
		return this.commands.zinterstore(destination, key);
	}

	@Override
	public Mono<Long> zinterstore(A destination, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zinterstore(destination, keysConfigurator.getKeys());
	}

	@Override
	public SortedSetZinterstoreBuilder<A, B> zinterstore() {
		return new SortedSetZinterstoreBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	public Mono<Long> zlexcount(A key, Bound<B> min, Bound<B> max) {
		return this.commands.zlexcount(key, SortedSetUtils.convertRange(min, max));
	}

	@Override
	public SortedSetZmpopBuilder<A, B> zmpop() {
		return new SortedSetZmpopBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<Optional<Double>> zmscore(A key, B member) {
		return this.commands.zmscore(key, member).flatMapMany(l -> Flux.fromStream(l.stream().map(Optional::ofNullable)));
	}

	@Override
	public Flux<Optional<Double>> zmscore(A key, Consumer<Values<B>> members) {
		ValuesImpl<B> membersConfigurator = new ValuesImpl<>(this.valueType);
		members.accept(membersConfigurator);
		return this.commands.zmscore(key, membersConfigurator.getValues()).flatMapMany(l -> Flux.fromStream(l.stream().map(Optional::ofNullable)));
	}

	@Override
	public Mono<SortedSetScoredMember<B>> zpopmax(A key) {
		return this.commands.zpopmax(key).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zpopmax(A key, long count) {
		return this.commands.zpopmax(key, count).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Mono<SortedSetScoredMember<B>> zpopmin(A key) {
		return this.commands.zpopmin(key).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zpopmin(A key, long count) {
		return this.commands.zpopmin(key, count).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Mono<B> zrandmember(A key) {
		return this.commands.zrandmember(key);
	}

	@Override
	public Flux<B> zrandmember(A key, long count) {
		return this.commands.zrandmember(key, count);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zrandmemberWithScores(A key, long count) {
		return this.commands.zrandmemberWithScores(key, count).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Flux<B> zrange(A key, long min, long max) {
		return this.commands.zrange(key, min, max);
	}

	@Override
	public SortedSetZrangeBuilder<A, B, Long> zrange() {
		return new SortedSetZrangeBuilderImpl<>(this.commands);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zrangeWithScores(A key, long min, long max) {
		return this.commands.zrangeWithScores(key, min, max).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public SortedSetZrangeWithScoresBuilder<A, B, Long> zrangeWithScores() {
		return new SortedSetZrangeWithScoresBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> zrangestore(A destination, A source, long min, long max) {
		throw new UnsupportedOperationException("Implementation doesn't support ZRANGESTORE dst src min max");
	}

	@Override
	public SortedSetZrangestoreBuilder<A, B, Long> zrangestore() {
		return new SortedSetZrangestoreBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> zrank(A key, B member) {
		return this.commands.zrank(key, member);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> zrem(A key, B member) {
		return this.commands.zrem(key, member);
	}

	@Override
	public Mono<Long> zrem(A key, Consumer<Values<B>> members) {
		ValuesImpl<B> membersConfigurator = new ValuesImpl<>(this.valueType);
		members.accept(membersConfigurator);
		return this.commands.zrem(key, membersConfigurator.getValues());
	}

	@Override
	public Mono<Long> zremrangebylex(A key, Bound<? extends B> min, Bound<? extends B> max) {
		return this.commands.zremrangebylex(key, SortedSetUtils.convertRange(min, max));
	}

	@Override
	public Mono<Long> zremrangebyrank(A key, long start, long stop) {
		return this.commands.zremrangebyrank(key, start, stop);
	}

	@Override
	public Mono<Long> zremrangebyscore(A key, Bound<? extends Number> min, Bound<? extends Number> max) {
		return this.commands.zremrangebyscore(key, SortedSetUtils.convertRange(min, max));
	}

	@Override
	public Mono<Long> zrevrank(A key, B member) {
		return this.commands.zrevrank(key, member);
	}

	@Override
	public Mono<SortedSetScanResult<B>> zscan(A key, String cursor) {
		return this.commands.zscan(key, ScanCursor.of(cursor)).map(SortedSetScanResultImpl::new);
	}

	@Override
	public SortedSetScanBuilder<A, B> zscan() {
		return new SortedSetScanBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Double> zscore(A key, B member) {
		return this.commands.zscore(key, member);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<B> zunion(A key) {
		return this.commands.zunion(key);
	}

	@Override
	public Flux<B> zunion(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zunion(keysConfigurator.getKeys());
	}

	@Override
	public SortedSetZunionBuilder<A, B> zunion() {
		return new SortedSetZunionBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<SortedSetScoredMember<B>> zunionWithScores(A key) {
		return this.commands.zunionWithScores(key).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public Flux<SortedSetScoredMember<B>> zunionWithScores(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zunionWithScores(keysConfigurator.getKeys()).map(SortedSetScoredMemberImpl::new);
	}

	@Override
	public SortedSetZunionWithScoresBuilder<A, B> zunionWithScores() {
		return new SortedSetZunionWithScoresBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> zunionstore(A destination, A key) {
		return this.commands.zunionstore(destination, key);
	}

	@Override
	public Mono<Long> zunionstore(A destination, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.zunionstore(destination, keysConfigurator.getKeys());
	}

	@Override
	public SortedSetZunionstoreBuilder<A, B> zunionstore() {
		return new SortedSetZunionstoreBuilderImpl<>(this.commands, this.keyType);
	}

	@Override
	public Mono<Long> xack(A key, A group, String messageId) {
		return this.commands.xack(key, group, messageId);
	}

	@Override
	public Mono<Long> xack(A key, A group, Consumer<StreamMessageIds> messageIds) {
		StreamMessageIdsImpl messageIdsConfigurator = new StreamMessageIdsImpl();
		messageIds.accept(messageIdsConfigurator);
		return this.commands.xack(key, group, messageIdsConfigurator.getIds());
	}

	@Override
	public Mono<String> xadd(A key, A field, B value) {
		return this.commands.xadd(key, field, value);
	}

	@Override
	public Mono<String> xadd(A key, Consumer<StreamEntries<A, B>> entries) {
		StreamEntriesImpl<A, B> entriesConfigurator = new StreamEntriesImpl<>();
		entries.accept(entriesConfigurator);
		return this.commands.xadd(key, entriesConfigurator.getEntries());
	}

	@Override
	public StreamXaddBuilder<A, B> xadd() {
		return new StreamXaddBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<StreamClaimedMessages<A, B>> xautoclaim(A key, A group, A consumer, long minIdleTime, String start) {
		Objects.requireNonNull(group);
		Objects.requireNonNull(consumer);
		Objects.requireNonNull(start);
		
		XAutoClaimArgs<A> xautoClaimArgs = new XAutoClaimArgs<>();
		xautoClaimArgs.consumer(io.lettuce.core.Consumer.from(group, consumer));
		xautoClaimArgs.minIdleTime(minIdleTime);
		xautoClaimArgs.startId(start);
		
		return this.commands.xautoclaim(key, xautoClaimArgs).map(StreamClaimedMessagesImpl::new);
	}

	@Override
	public StreamXautoclaimBuilder<A, B> xautoclaim() {
		return new StreamXautoclaimBuilderImpl<>(this.commands);
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A key, A group, A consumer, long minIdleTime, String messageId) {
		return this.commands.xclaim(key, io.lettuce.core.Consumer.from(group, consumer), minIdleTime, messageId).map(StreamMessageImpl::new);
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A key, A group, A consumer, long minIdleTime, Consumer<StreamMessageIds> messageIds) {
		StreamMessageIdsImpl messageIdsConfigurator = new StreamMessageIdsImpl();
		messageIds.accept(messageIdsConfigurator);
		return this.commands.xclaim(key, io.lettuce.core.Consumer.from(group, consumer), minIdleTime, messageIdsConfigurator.getIds()).map(StreamMessageImpl::new);
	}

	@Override
	public StreamXclaimBuilder<A, B> xclaim() {
		return new StreamXclaimBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> xdel(A key, String messageId) {
		return this.commands.xdel(key, messageId);
	}

	@Override
	public Mono<Long> xdel(A key, Consumer<StreamMessageIds> messageIds) {
		StreamMessageIdsImpl messageIdsConfigurator = new StreamMessageIdsImpl();
		messageIds.accept(messageIdsConfigurator);
		return this.commands.xdel(key, messageIdsConfigurator.getIds());
	}

	@Override
	public Mono<String> xgroupCreate(A key, A group, String id) {
		return this.commands.xgroupCreate(XReadArgs.StreamOffset.from(key, id), group);
	}

	@Override
	public StreamXgroupCreateBuilder<A> xgroupCreate() {
		return new StreamXgroupCreateBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Boolean> xgroupCreateconsumer(A key, A group, A consumer) {
		return this.commands.xgroupCreateconsumer(key, io.lettuce.core.Consumer.from(group, consumer));
	}

	@Override
	public Mono<Long> xgroupDelconsumer(A key, A group, A consumer) {
		return this.commands.xgroupDelconsumer(key, io.lettuce.core.Consumer.from(group, consumer));
	}

	@Override
	public Mono<Boolean> xgroupDestroy(A key, A group) {
		return this.commands.xgroupDestroy(key, group);
	}

	@Override
	public Mono<String> xgroupSetid(A key, A group, String id) {
		return this.commands.xgroupSetid(XReadArgs.StreamOffset.from(key, id), group);
	}

	@Override
	public Flux<Object> xinfoConsumers(A key, A group) {
		return this.commands.xinfoConsumers(key, group);
	}

	@Override
	public Flux<Object> xinfoGroups(A key) {
		return this.commands.xinfoGroups(key);
	}

	@Override
	public Flux<Object> xinfoStream(A key) {
		return this.commands.xinfoStream(key);
	}

	@Override
	public Flux<Object> xinfoStreamFull(A key) {
		throw new UnsupportedOperationException("Implementation doesn't support XINFO STREAM key FULL");
	}

	@Override
	public Flux<Object> xinfoStreamFull(A key, long count) {
		throw new UnsupportedOperationException("Implementation doesn't support XINFO STREAM key FULL COUNT count");
	}

	@Override
	public Mono<Long> xlen(A key) {
		return this.commands.xlen(key);
	}

	@Override
	public Mono<StreamPendingMessages> xpending(A key, A group) {
		return this.commands.xpending(key, group).map(StreamPendingMessagesImpl::new);
	}

	@Override
	public Flux<StreamPendingMessage> xpendingExtended(A key, A group, String start, String end, long count) {
		return this.commands.xpending(key, group, Range.create(start, end), Limit.from(count)).map(StreamPendingMessageImpl::new);
	}

	@Override
	public StreamXpendingExtendedBuilder<A> xpendingExtended() {
		return new StreamXpendingExtendedBuilderImpl<>(this.commands);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A key, String start, String end) {
		return this.commands.xrange(key, Range.create(start, end)).map(StreamMessageImpl::new);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A key, String start, String end, long count) {
		return this.commands.xrange(key, Range.create(start, end), Limit.from(count)).map(StreamMessageImpl::new);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<StreamMessage<A, B>> xread(A key, String messageId) {
		return this.commands.xread(XReadArgs.StreamOffset.from(key, messageId)).map(StreamMessageImpl::new);
	}

	@Override
	public Flux<StreamMessage<A, B>> xread(Consumer<StreamStreams<A>> streams) {
		StreamStreamsImpl<A> streamsConfigurator = new StreamStreamsImpl<>();
		streams.accept(streamsConfigurator);
		return this.commands.xread(streamsConfigurator.getStreams()).map(StreamMessageImpl::new);
	}

	@Override
	public StreamXreadBuilder<A, B> xread() {
		return new StreamXreadBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<StreamMessage<A, B>> xreadgroup(A group, A consumer, A key, String messageId) {
		return this.commands.xreadgroup(io.lettuce.core.Consumer.from(group, consumer), XReadArgs.StreamOffset.from(key, messageId)).map(StreamMessageImpl::new);
	}

	@Override
	public Flux<StreamMessage<A, B>> xreadgroup(A group, A consumer, Consumer<StreamStreams<A>> streams) {
		StreamStreamsImpl<A> streamsConfigurator = new StreamStreamsImpl<>();
		streams.accept(streamsConfigurator);
		return this.commands.xreadgroup(io.lettuce.core.Consumer.from(group, consumer), streamsConfigurator.getStreams()).map(StreamMessageImpl::new);
	}

	@Override
	public StreamXreadgroupBuilder<A, B> xreadgroup() {
		return new StreamXreadgroupBuilderImpl<>(this.commands);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A key, String start, String end) {
		return this.commands.xrevrange(key, Range.create(start, end)).map(StreamMessageImpl::new);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A key, String start, String end, long count) {
		return this.commands.xrevrange(key, Range.create(start, end), Limit.from(count)).map(StreamMessageImpl::new);
	}

	@Override
	public Mono<Long> xtrimMaxLen(A key, long threshold) {
		return this.commands.xtrim(key, XTrimArgs.Builder.maxlen(threshold));
	}

	@Override
	public Mono<Long> xtrimMaxLen(A key, long threshold, long count) {
		return this.commands.xtrim(key, XTrimArgs.Builder.maxlen(threshold).limit(count));
	}

	@Override
	public Mono<Long> xtrimMinId(A key, String streamId) {
		return this.commands.xtrim(key, XTrimArgs.Builder.minId(streamId));
	}

	@Override
	public Mono<Long> xtrimMinId(A key, String streamId, long count) {
		return this.commands.xtrim(key, XTrimArgs.Builder.minId(streamId).limit(count));
	}

	@Override
	public StreamXtrimBuilder<A> xtrim() {
		return new StreamXtrimBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<Long> append(A key, B value) {
		return this.commands.append(key, value);
	}

	@Override
	public Mono<Long> decr(A key) {
		return this.commands.decr(key);
	}

	@Override
	public Mono<Long> decrby(A key, long decrement) {
		return this.commands.decrby(key, decrement);
	}

	@Override
	public Mono<B> get(A key) {
		return this.commands.get(key);
	}

	@Override
	public Mono<B> getdel(A key) {
		return this.commands.getdel(key);
	}

	@Override
	public Mono<B> getex(A key) {
		return this.commands.getex(key, new GetExArgs());
	}

	@Override
	public StringGetexBuilder<A, B> getex() {
		return new StringGetexBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<B> getrange(A key, long start, long end) {
		return this.commands.getrange(key, start, end);
	}

	@Override
	public Mono<B> getset(A key, B value) {
		return this.commands.getset(key, value);
	}

	@Override
	public Mono<Long> incr(A key) {
		return this.commands.incr(key);
	}

	@Override
	public Mono<Long> incrby(A key, long increment) {
		return this.commands.incrby(key, increment);
	}

	@Override
	public Mono<Double> incrbyfloat(A key, double increment) {
		return this.commands.incrbyfloat(key, increment);
	}

	@Override
	public Flux<EntryOptional<A, B>> mget(Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.mget(keysConfigurator.getKeys()).map(kv -> EntryOptional.ofNullable(kv.getKey(), kv.getValue()));
	}

	@Override
	public Mono<String> mset(Consumer<Entries<A, B>> entries) {
		EntriesImpl<A, B> entriesConfigurator = new EntriesImpl<>();
		entries.accept(entriesConfigurator);
		return this.commands.mset(entriesConfigurator.getEntries());
	}

	@Override
	public Mono<Boolean> msetnx(Consumer<Entries<A, B>> entries) {
		EntriesImpl<A, B> entriesConfigurator = new EntriesImpl<>();
		entries.accept(entriesConfigurator);
		return this.commands.msetnx(entriesConfigurator.getEntries());
	}

	@Override
	public Mono<String> psetex(A key, long milliseconds, B value) {
		return this.commands.psetex(key, milliseconds, value);
	}

	@Override
	public Mono<String> set(A key, B value) {
		return this.commands.set(key, value);
	}

	@Override
	public StringSetBuilder<A, B> set() {
		return new StringSetBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<B> setGet(A key, B value) {
		return this.commands.setGet(key, value);
	}

	@Override
	public StringSetGetBuilder<A, B> setGet() {
		return new StringSetGetBuilderImpl<>(this.commands);
	}

	@Override
	public Mono<String> setex(A key, long seconds, B value) {
		return this.commands.setex(key, seconds, value);
	}

	@Override
	public Mono<Boolean> setnx(A key, B value) {
		return this.commands.setnx(key, value);
	}

	@Override
	public Mono<Long> setrange(A key, long offset, B value) {
		return this.commands.setrange(key, offset, value);
	}

	@Override
	public Mono<Long> strlen(A key) {
		return this.commands.strlen(key);
	}

	@Override
	public Mono<Long> bitcount(A key) {
		return this.commands.bitcount(key);
	}

	@Override
	public Mono<Long> bitcount(A key, long start, long end) {
		return this.commands.bitcount(key, start, end);
	}

	@Override
	public Flux<Optional<Long>> bitfield(A key) {
		return this.commands.bitfield(key, new BitFieldArgs()).map(v -> Optional.ofNullable(v.getValueOrElse(null)));
	}

	@Override
	public StringBitfieldBuilder<A, B> bitfield() {
		return new StringBitfieldBuilderImpl<>(this.commands);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> bitopAnd(A destKey, A key) {
		return this.commands.bitopAnd(destKey, key);
	}

	@Override
	public Mono<Long> bitopAnd(A destKey, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.bitopAnd(destKey, keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> bitopOr(A destKey, A key) {
		return this.commands.bitopOr(destKey, key);
	}

	@Override
	public Mono<Long> bitopOr(A destKey, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.bitopOr(destKey, keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> bitopXor(A destKey, A key) {
		return this.commands.bitopXor(destKey, key);
	}

	@Override
	public Mono<Long> bitopXor(A destKey, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.bitopXor(destKey, keysConfigurator.getKeys());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Long> bitopNot(A destKey, A key) {
		return this.commands.bitopXor(destKey, key);
	}

	@Override
	public Mono<Long> bitopNot(A destKey, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return this.commands.bitopXor(destKey, keysConfigurator.getKeys());
	}

	@Override
	public Mono<Long> bitpos(A key, boolean bit) {
		return this.commands.bitpos(key, bit);
	}

	@Override
	public Mono<Long> bitpos(A key, boolean bit, long start) {
		return this.commands.bitpos(key, bit, start);
	}

	@Override
	public Mono<Long> bitpos(A key, boolean bit, long start, long end) {
		return this.commands.bitpos(key, bit, start, end);
	}

	@Override
	public Mono<Long> getbit(A key, long offset) {
		return this.commands.getbit(key, offset);
	}

	@Override
	public Mono<Long> setbit(A key, long offset, int value) {
		return this.commands.setbit(key, offset, value);
	}
}

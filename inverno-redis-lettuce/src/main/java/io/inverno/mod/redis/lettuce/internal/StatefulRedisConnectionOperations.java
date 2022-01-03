/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.lettuce.internal;

import io.inverno.mod.redis.lettuce.RedisOperations;
import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.Consumer;
import io.lettuce.core.CopyArgs;
import io.lettuce.core.FlushMode;
import io.lettuce.core.GeoAddArgs;
import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.GeoSearch;
import io.lettuce.core.GeoValue;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.LMoveArgs;
import io.lettuce.core.LPosArgs;
import io.lettuce.core.Limit;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.MigrateArgs;
import io.lettuce.core.Range;
import io.lettuce.core.RestoreArgs;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.SortArgs;
import io.lettuce.core.StrAlgoArgs;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.StringMatchResult;
import io.lettuce.core.Value;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.XClaimArgs;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XPendingArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.XTrimArgs;
import io.lettuce.core.ZAddArgs;
import io.lettuce.core.ZAggregateArgs;
import io.lettuce.core.ZStoreArgs;
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
import io.lettuce.core.models.stream.ClaimedMessages;
import io.lettuce.core.models.stream.PendingMessage;
import io.lettuce.core.models.stream.PendingMessages;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.output.ScoredValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;
import io.lettuce.core.support.BoundedAsyncPool;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class StatefulRedisConnectionOperations<A, B, C extends StatefulConnection<A, B>, D extends BaseRedisReactiveCommands<A, B> & RedisGeoReactiveCommands<A, B> & RedisHashReactiveCommands<A, B> & RedisHLLReactiveCommands<A, B> & RedisKeyReactiveCommands<A, B> & RedisListReactiveCommands<A, B> & RedisScriptingReactiveCommands<A, B> & RedisSetReactiveCommands<A, B> & RedisSortedSetReactiveCommands<A, B> & RedisStreamReactiveCommands<A, B> & RedisStringReactiveCommands<A, B>> 
	implements RedisOperations<A, B> {
	
	protected final C connection;
	
	protected final D commands;
	
	protected final BoundedAsyncPool<C> pool;
	
	public StatefulRedisConnectionOperations(C connection, D commands, BoundedAsyncPool<C> pool) {
		this.connection = connection;
		this.commands = commands;
		this.pool = pool;
	}
	
	public Mono<Void> close() {
		return Mono.fromCompletionStage(this.pool.release(this.connection));
	}
	
	public D getCommands() {
		return this.commands;
	}
	
	@Override
	public Mono<Long> del(A... ks) {
		return this.commands.del(ks);
	}

	@Override
	public Flux<KeyValue<A, B>> mget(A... ks) {
		return this.commands.mget(ks);
	}

	@Override
	public Mono<String> mset(Map<A, B> map) {
		return this.commands.mset(map);
	}

	@Override
	public Mono<Boolean> msetnx(Map<A, B> map) {
		return this.commands.msetnx(map);
	}

	@Override
	public Mono<Long> geoadd(A k, double d, double d1, B v) {
		return this.commands.geoadd(k, d, d1, v);
	}

	@Override
	public Mono<Long> geoadd(A k, double d, double d1, B v, GeoAddArgs gaa) {
		return this.commands.geoadd(k, d, d1, v, gaa);
	}

	@Override
	public Mono<Long> geoadd(A k, Object... os) {
		return this.commands.geoadd(k, os);
	}

	@Override
	public Mono<Long> geoadd(A k, GeoValue<B>... gvs) {
		return this.commands.geoadd(k, gvs);
	}

	@Override
	public Mono<Long> geoadd(A k, GeoAddArgs gaa, Object... os) {
		return this.commands.geoadd(k, gaa, os);
	}

	@Override
	public Mono<Long> geoadd(A k, GeoAddArgs gaa, GeoValue<B>... gvs) {
		return this.commands.geoadd(k, gaa, gvs);
	}

	@Override
	public Mono<Double> geodist(A k, B v, B v1, GeoArgs.Unit unit) {
		return this.commands.geodist(k, v, v1, unit);
	}

	@Override
	public Flux<Value<String>> geohash(A k, B... vs) {
		return this.commands.geohash(k, vs);
	}

	@Override
	public Flux<Value<GeoCoordinates>> geopos(A k, B... vs) {
		return this.commands.geopos(k, vs);
	}

	@Override
	public Flux<B> georadius(A k, double d, double d1, double d2, GeoArgs.Unit unit) {
		return this.commands.georadius(k, d, d1, d2, unit);
	}

	@Override
	public Flux<GeoWithin<B>> georadius(A k, double d, double d1, double d2, GeoArgs.Unit unit, GeoArgs ga) {
		return this.commands.georadius(k, d, d1, d2, unit, ga);
	}

	@Override
	public Mono<Long> georadius(A k, double d, double d1, double d2, GeoArgs.Unit unit, GeoRadiusStoreArgs<A> grsa) {
		return this.commands.georadius(k, d, d1, d2, unit, grsa);
	}

	@Override
	public Flux<B> georadiusbymember(A k, B v, double d, GeoArgs.Unit unit) {
		return this.commands.georadiusbymember(k, v, d, unit);
	}

	@Override
	public Flux<GeoWithin<B>> georadiusbymember(A k, B v, double d, GeoArgs.Unit unit, GeoArgs ga) {
		return this.commands.georadiusbymember(k, v, d, unit, ga);
	}

	@Override
	public Mono<Long> georadiusbymember(A k, B v, double d, GeoArgs.Unit unit, GeoRadiusStoreArgs<A> grsa) {
		return this.commands.georadiusbymember(k, v, d, unit, grsa);
	}

	@Override
	public Flux<B> geosearch(A k, GeoSearch.GeoRef<A> georef, GeoSearch.GeoPredicate gp) {
		return this.commands.geosearch(k, georef, gp);
	}

	@Override
	public Flux<GeoWithin<B>> geosearch(A k, GeoSearch.GeoRef<A> georef, GeoSearch.GeoPredicate gp, GeoArgs ga) {
		return this.commands.geosearch(k, georef, gp, ga);
	}

	@Override
	public Mono<Long> geosearchstore(A k, A k1, GeoSearch.GeoRef<A> georef, GeoSearch.GeoPredicate gp, GeoArgs ga, boolean bln) {
		return this.commands.geosearchstore(k, k1, georef, gp, ga, bln);
	}

	@Override
	public Mono<Long> hdel(A k, A... ks) {
		return this.commands.hdel(k, ks);
	}

	@Override
	public Mono<Boolean> hexists(A k, A k1) {
		return this.commands.hexists(k, k1);
	}

	@Override
	public Mono<B> hget(A k, A k1) {
		return this.commands.hget(k, k1);
	}

	@Override
	public Mono<Long> hincrby(A k, A k1, long l) {
		return this.commands.hincrby(k, k1, l);
	}

	@Override
	public Mono<Double> hincrbyfloat(A k, A k1, double d) {
		return this.commands.hincrbyfloat(k, k1, d);
	}

	@Override
	public Flux<KeyValue<A, B>> hgetall(A k) {
		return this.commands.hgetall(k);
	}

	@Override
	public Mono<Long> hgetall(KeyValueStreamingChannel<A, B> kvsc, A k) {
		return this.commands.hgetall(kvsc, k);
	}

	@Override
	public Flux<A> hkeys(A k) {
		return this.commands.hkeys(k);
	}

	@Override
	public Mono<Long> hkeys(KeyStreamingChannel<A> ksc, A k) {
		return this.commands.hkeys(ksc, k);
	}

	@Override
	public Mono<Long> hlen(A k) {
		return this.commands.hlen(k);
	}

	@Override
	public Flux<KeyValue<A, B>> hmget(A k, A... ks) {
		return this.commands.hmget(k, ks);
	}

	@Override
	public Mono<Long> hmget(KeyValueStreamingChannel<A, B> kvsc, A k, A... ks) {
		return this.commands.hmget(kvsc, k, ks);
	}

	@Override
	public Mono<String> hmset(A k, Map<A, B> map) {
		return this.commands.hmset(k, map);
	}

	@Override
	public Mono<A> hrandfield(A k) {
		return this.commands.hrandfield(k);
	}

	@Override
	public Flux<A> hrandfield(A k, long l) {
		return this.commands.hrandfield(k, l);
	}

	@Override
	public Mono<KeyValue<A, B>> hrandfieldWithvalues(A k) {
		return this.commands.hrandfieldWithvalues(k);
	}

	@Override
	public Flux<KeyValue<A, B>> hrandfieldWithvalues(A k, long l) {
		return this.commands.hrandfieldWithvalues(k, l);
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k) {
		return this.commands.hscan(k);
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k, ScanArgs sa) {
		return this.commands.hscan(k, sa);
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k, ScanCursor sc, ScanArgs sa) {
		return this.commands.hscan(k, sc, sa);
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k, ScanCursor sc) {
		return this.commands.hscan(k, sc);
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k) {
		return this.commands.hscan(kvsc, k);
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k, ScanArgs sa) {
		return this.commands.hscan(kvsc, k, sa);
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k, ScanCursor sc, ScanArgs sa) {
		return this.commands.hscan(kvsc, k, sc, sa);
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k, ScanCursor sc) {
		return this.commands.hscan(kvsc, k, sc);
	}

	@Override
	public Mono<Boolean> hset(A k, A k1, B v) {
		return this.commands.hset(k, k1, v);
	}

	@Override
	public Mono<Long> hset(A k, Map<A, B> map) {
		return this.commands.hset(k, map);
	}

	@Override
	public Mono<Boolean> hsetnx(A k, A k1, B v) {
		return this.commands.hsetnx(k, k1, v);
	}

	@Override
	public Mono<Long> hstrlen(A k, A k1) {
		return this.commands.hstrlen(k, k1);
	}

	@Override
	public Flux<B> hvals(A k) {
		return this.commands.hvals(k);
	}

	@Override
	public Mono<Long> hvals(ValueStreamingChannel<B> vsc, A k) {
		return this.commands.hvals(vsc, k);
	}

	@Override
	public Mono<Long> pfadd(A k, B... vs) {
		return this.commands.pfadd(k, vs);
	}

	@Override
	public Mono<String> pfmerge(A k, A... ks) {
		return this.commands.pfmerge(k, ks);
	}

	@Override
	public Mono<Long> pfcount(A... ks) {
		return this.commands.pfcount(ks);
	}

	@Override
	public Mono<Boolean> copy(A k, A k1) {
		return this.commands.copy(k, k1);
	}

	@Override
	public Mono<Boolean> copy(A k, A k1, CopyArgs ca) {
		return this.commands.copy(k, k1, ca);
	}

	@Override
	public Mono<Long> unlink(A... ks) {
		return this.commands.unlink(ks);
	}

	@Override
	public Mono<byte[]> dump(A k) {
		return this.commands.dump(k);
	}

	@Override
	public Mono<Long> exists(A... ks) {
		return this.commands.exists(ks);
	}

	@Override
	public Mono<Boolean> expire(A k, long l) {
		return this.commands.expire(k, l);
	}

	@Override
	public Mono<Boolean> expire(A k, Duration drtn) {
		return this.commands.expire(k, drtn);
	}

	@Override
	public Mono<Boolean> expireat(A k, long l) {
		return this.commands.expireat(k, l);
	}

	@Override
	public Mono<Boolean> expireat(A k, Date date) {
		return this.commands.expireat(k, date);
	}

	@Override
	public Mono<Boolean> expireat(A k, Instant instnt) {
		return this.commands.expireat(k, instnt);
	}

	@Override
	public Flux<A> keys(A k) {
		return this.commands.keys(k);
	}

	@Override
	public Mono<Long> keys(KeyStreamingChannel<A> ksc, A k) {
		return this.commands.keys(ksc, k);
	}

	@Override
	public Mono<String> migrate(String string, int i, A k, int i1, long l) {
		return this.commands.migrate(string, i, k, i1, l);
	}

	@Override
	public Mono<String> migrate(String string, int i, int i1, long l, MigrateArgs<A> ma) {
		return this.commands.migrate(string, i, i1, l, ma);
	}

	@Override
	public Mono<Boolean> move(A k, int i) {
		return this.commands.move(k, i);
	}

	@Override
	public Mono<String> objectEncoding(A k) {
		return this.commands.objectEncoding(k);
	}

	@Override
	public Mono<Long> objectFreq(A k) {
		return this.commands.objectFreq(k);
	}

	@Override
	public Mono<Long> objectIdletime(A k) {
		return this.commands.objectIdletime(k);
	}

	@Override
	public Mono<Long> objectRefcount(A k) {
		return this.commands.objectRefcount(k);
	}

	@Override
	public Mono<Boolean> persist(A k) {
		return this.commands.persist(k);
	}

	@Override
	public Mono<Boolean> pexpire(A k, long l) {
		return this.commands.pexpire(k, l);
	}

	@Override
	public Mono<Boolean> pexpire(A k, Duration drtn) {
		return this.commands.pexpire(k, drtn);
	}

	@Override
	public Mono<Boolean> pexpireat(A k, long l) {
		return this.commands.pexpireat(k, l);
	}

	@Override
	public Mono<Boolean> pexpireat(A k, Date date) {
		return this.commands.pexpireat(k, date);
	}

	@Override
	public Mono<Boolean> pexpireat(A k, Instant instnt) {
		return this.commands.pexpireat(k, instnt);
	}

	@Override
	public Mono<Long> pttl(A k) {
		return this.commands.pttl(k);
	}

	@Override
	public Mono<A> randomkey() {
		return this.commands.randomkey();
	}

	@Override
	public Mono<String> rename(A k, A k1) {
		return this.commands.rename(k, k1);
	}

	@Override
	public Mono<Boolean> renamenx(A k, A k1) {
		return this.commands.renamenx(k, k1);
	}

	@Override
	public Mono<String> restore(A k, long l, byte[] bytes) {
		return this.commands.restore(k, l, bytes);
	}

	@Override
	public Mono<String> restore(A k, byte[] bytes, RestoreArgs ra) {
		return this.commands.restore(k, bytes, ra);
	}

	@Override
	public Flux<B> sort(A k) {
		return this.commands.sort(k);
	}

	@Override
	public Mono<Long> sort(ValueStreamingChannel<B> vsc, A k) {
		return this.commands.sort(vsc, k);
	}

	@Override
	public Flux<B> sort(A k, SortArgs sa) {
		return this.commands.sort(k, sa);
	}

	@Override
	public Mono<Long> sort(ValueStreamingChannel<B> vsc, A k, SortArgs sa) {
		return this.commands.sort(vsc, k, sa);
	}

	@Override
	public Mono<Long> sortStore(A k, SortArgs sa, A k1) {
		return this.commands.sortStore(k, sa, k1);
	}

	@Override
	public Mono<Long> touch(A... ks) {
		return this.commands.touch(ks);
	}

	@Override
	public Mono<Long> ttl(A k) {
		return this.commands.ttl(k);
	}

	@Override
	public Mono<String> type(A k) {
		return this.commands.type(k);
	}

	@Override
	public Mono<KeyScanCursor<A>> scan() {
		return this.commands.scan();
	}

	@Override
	public Mono<KeyScanCursor<A>> scan(ScanArgs sa) {
		return this.commands.scan(sa);
	}

	@Override
	public Mono<KeyScanCursor<A>> scan(ScanCursor sc, ScanArgs sa) {
		return this.commands.scan(sc, sa);
	}

	@Override
	public Mono<KeyScanCursor<A>> scan(ScanCursor sc) {
		return this.commands.scan(sc);
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc) {
		return this.commands.scan(ksc);
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc, ScanArgs sa) {
		return this.commands.scan(ksc, sa);
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc, ScanCursor sc, ScanArgs sa) {
		return this.commands.scan(ksc, sc, sa);
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc, ScanCursor sc) {
		return this.commands.scan(ksc, sc);
	}

	@Override
	public Mono<B> blmove(A k, A k1, LMoveArgs lma, long l) {
		return this.commands.blmove(k, k1, lma, l);
	}

	@Override
	public Mono<B> blmove(A k, A k1, LMoveArgs lma, double d) {
		return this.commands.blmove(k, k1, lma, d);
	}

	@Override
	public Mono<KeyValue<A, B>> blpop(long l, A... ks) {
		return this.commands.blpop(l, ks);
	}

	@Override
	public Mono<KeyValue<A, B>> blpop(double d, A... ks) {
		return this.commands.blpop(d, ks);
	}

	@Override
	public Mono<KeyValue<A, B>> brpop(long l, A... ks) {
		return this.commands.brpop(l, ks);
	}

	@Override
	public Mono<KeyValue<A, B>> brpop(double d, A... ks) {
		return this.commands.brpop(d, ks);
	}

	@Override
	public Mono<B> brpoplpush(long l, A k, A k1) {
		return this.commands.brpoplpush(l, k, k1);
	}

	@Override
	public Mono<B> brpoplpush(double d, A k, A k1) {
		return this.commands.brpoplpush(d, k, k1);
	}

	@Override
	public Mono<B> lindex(A k, long l) {
		return this.commands.lindex(k, l);
	}

	@Override
	public Mono<Long> linsert(A k, boolean bln, B v, B v1) {
		return this.commands.linsert(k, bln, v, v1);
	}

	@Override
	public Mono<Long> llen(A k) {
		return this.commands.llen(k);
	}

	@Override
	public Mono<B> lmove(A k, A k1, LMoveArgs lma) {
		return this.commands.lmove(k, k1, lma);
	}

	@Override
	public Mono<B> lpop(A k) {
		return this.commands.lpop(k);
	}

	@Override
	public Flux<B> lpop(A k, long l) {
		return this.commands.lpop(k, l);
	}

	@Override
	public Mono<Long> lpos(A k, B v) {
		return this.commands.lpos(k, v);
	}

	@Override
	public Mono<Long> lpos(A k, B v, LPosArgs lpa) {
		return this.commands.lpos(k, v, lpa);
	}

	@Override
	public Flux<Long> lpos(A k, B v, int i) {
		return this.commands.lpos(k, v, i);
	}

	@Override
	public Flux<Long> lpos(A k, B v, int i, LPosArgs lpa) {
		return this.commands.lpos(k, v, i, lpa);
	}

	@Override
	public Mono<Long> lpush(A k, B... vs) {
		return this.commands.lpush(k, vs);
	}

	@Override
	public Mono<Long> lpushx(A k, B... vs) {
		return this.commands.lpushx(k, vs);
	}

	@Override
	public Flux<B> lrange(A k, long l, long l1) {
		return this.commands.lrange(k, l, l1);
	}

	@Override
	public Mono<Long> lrange(ValueStreamingChannel<B> vsc, A k, long l, long l1) {
		return this.commands.lrange(vsc, k, l, l1);
	}

	@Override
	public Mono<Long> lrem(A k, long l, B v) {
		return this.commands.lrem(k, l, v);
	}

	@Override
	public Mono<String> lset(A k, long l, B v) {
		return this.commands.lset(k, l, v);
	}

	@Override
	public Mono<String> ltrim(A k, long l, long l1) {
		return this.commands.ltrim(k, l, l1);
	}

	@Override
	public Mono<B> rpop(A k) {
		return this.commands.rpop(k);
	}

	@Override
	public Flux<B> rpop(A k, long l) {
		return this.commands.rpop(k, l);
	}

	@Override
	public Mono<B> rpoplpush(A k, A k1) {
		return this.commands.rpoplpush(k, k1);
	}

	@Override
	public Mono<Long> rpush(A k, B... vs) {
		return this.commands.rpush(k, vs);
	}

	@Override
	public Mono<Long> rpushx(A k, B... vs) {
		return this.commands.rpushx(k, vs);
	}

	@Override
	public <T> Flux<T> eval(String string, ScriptOutputType sot, A... ks) {
		return this.commands.eval(string, sot, ks);
	}

	@Override
	public <T> Flux<T> eval(byte[] bytes, ScriptOutputType sot, A... ks) {
		return this.commands.eval(bytes, sot, ks);
	}

	@Override
	public <T> Flux<T> eval(String string, ScriptOutputType sot, A[] ks, B... vs) {
		return this.commands.eval(string, sot, ks, vs);
	}

	@Override
	public <T> Flux<T> eval(byte[] bytes, ScriptOutputType sot, A[] ks, B... vs) {
		return this.commands.eval(bytes, sot, ks, vs);
	}

	@Override
	public <T> Flux<T> evalsha(String string, ScriptOutputType sot, A... ks) {
		return this.commands.evalsha(string, sot, ks);
	}

	@Override
	public <T> Flux<T> evalsha(String string, ScriptOutputType sot, A[] ks, B... vs) {
		return this.commands.evalsha(string, sot, ks, vs);
	}

	@Override
	public Flux<Boolean> scriptExists(String... strings) {
		return this.commands.scriptExists(strings);
	}

	@Override
	public Mono<String> scriptFlush() {
		return this.commands.scriptFlush();
	}

	@Override
	public Mono<String> scriptFlush(FlushMode fm) {
		return this.commands.scriptFlush(fm);
	}

	@Override
	public Mono<String> scriptKill() {
		return this.commands.scriptKill();
	}

	@Override
	public Mono<String> scriptLoad(String string) {
		return this.commands.scriptLoad(string);
	}

	@Override
	public Mono<String> scriptLoad(byte[] bytes) {
		return this.commands.scriptLoad(bytes);
	}

	@Override
	public String digest(String script) {
		return this.commands.digest(script);
	}

	@Override
	public String digest(byte[] script) {
		return this.commands.digest(script);
	}

	@Override
	public Mono<Long> sadd(A k, B... vs) {
		return this.commands.sadd(k, vs);
	}

	@Override
	public Mono<Long> scard(A k) {
		return this.commands.scard(k);
	}

	@Override
	public Flux<B> sdiff(A... ks) {
		return this.commands.sdiff(ks);
	}

	@Override
	public Mono<Long> sdiff(ValueStreamingChannel<B> vsc, A... ks) {
		return this.commands.sdiff(vsc, ks);
	}

	@Override
	public Mono<Long> sdiffstore(A k, A... ks) {
		return this.commands.sdiffstore(k, ks);
	}

	@Override
	public Flux<B> sinter(A... ks) {
		return this.commands.sinter(ks);
	}

	@Override
	public Mono<Long> sinter(ValueStreamingChannel<B> vsc, A... ks) {
		return this.commands.sinter(vsc, ks);
	}

	@Override
	public Mono<Long> sinterstore(A k, A... ks) {
		return this.commands.sinterstore(k, ks);
	}

	@Override
	public Mono<Boolean> sismember(A k, B v) {
		return this.commands.sismember(k, v);
	}

	@Override
	public Flux<B> smembers(A k) {
		return this.commands.smembers(k);
	}

	@Override
	public Mono<Long> smembers(ValueStreamingChannel<B> vsc, A k) {
		return this.commands.smembers(vsc, k);
	}

	@Override
	public Flux<Boolean> smismember(A k, B... vs) {
		return this.commands.smismember(k, vs);
	}

	@Override
	public Mono<Boolean> smove(A k, A k1, B v) {
		return this.commands.smove(k, k1, v);
	}

	@Override
	public Mono<B> spop(A k) {
		return this.commands.spop(k);
	}

	@Override
	public Flux<B> spop(A k, long l) {
		return this.commands.spop(k, l);
	}

	@Override
	public Mono<B> srandmember(A k) {
		return this.commands.srandmember(k);
	}

	@Override
	public Flux<B> srandmember(A k, long l) {
		return this.commands.srandmember(k, l);
	}

	@Override
	public Mono<Long> srandmember(ValueStreamingChannel<B> vsc, A k, long l) {
		return this.commands.srandmember(vsc, k, l);
	}

	@Override
	public Mono<Long> srem(A k, B... vs) {
		return this.commands.srem(k, vs);
	}

	@Override
	public Flux<B> sunion(A... ks) {
		return this.commands.sunion(ks);
	}

	@Override
	public Mono<Long> sunion(ValueStreamingChannel<B> vsc, A... ks) {
		return this.commands.sunion(vsc, ks);
	}

	@Override
	public Mono<Long> sunionstore(A k, A... ks) {
		return this.commands.sunionstore(k, ks);
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k) {
		return this.commands.sscan(k);
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k, ScanArgs sa) {
		return this.commands.sscan(k, sa);
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k, ScanCursor sc, ScanArgs sa) {
		return this.commands.sscan(k, sc, sa);
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k, ScanCursor sc) {
		return this.commands.sscan(k, sc);
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k) {
		return this.commands.sscan(vsc, k);
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k, ScanArgs sa) {
		return this.commands.sscan(vsc, k, sa);
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k, ScanCursor sc, ScanArgs sa) {
		return this.commands.sscan(vsc, k, sc, sa);
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k, ScanCursor sc) {
		return this.commands.sscan(vsc, k, sc);
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmin(long l, A... ks) {
		return this.commands.bzpopmin(l, ks);
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmin(double d, A... ks) {
		return this.commands.bzpopmin(d, ks);
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmax(long l, A... ks) {
		return this.commands.bzpopmax(l, ks);
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmax(double d, A... ks) {
		return this.commands.bzpopmax(d, ks);
	}

	@Override
	public Mono<Long> zadd(A k, double d, B v) {
		return this.commands.zadd(k, d, v);
	}

	@Override
	public Mono<Long> zadd(A k, Object... os) {
		return this.commands.zadd(k, os);
	}

	@Override
	public Mono<Long> zadd(A k, ScoredValue<B>... svs) {
		return this.commands.zadd(k, svs);
	}

	@Override
	public Mono<Long> zadd(A k, ZAddArgs zaa, double d, B v) {
		return this.commands.zadd(k, zaa, d, v);
	}

	@Override
	public Mono<Long> zadd(A k, ZAddArgs zaa, Object... os) {
		return this.commands.zadd(k, zaa, os);
	}

	@Override
	public Mono<Long> zadd(A k, ZAddArgs zaa, ScoredValue<B>... svs) {
		return this.commands.zadd(k, zaa, svs);
	}

	@Override
	public Mono<Double> zaddincr(A k, double d, B v) {
		return this.commands.zaddincr(k, d, v);
	}

	@Override
	public Mono<Double> zaddincr(A k, ZAddArgs zaa, double d, B v) {
		return this.commands.zaddincr(k, zaa, d, v);
	}

	@Override
	public Mono<Long> zcard(A k) {
		return this.commands.zcard(k);
	}

	@Override
	public Mono<Long> zcount(A k, double d, double d1) {
		return this.commands.zcount(k, d, d1);
	}

	@Override
	public Mono<Long> zcount(A k, String string, String string1) {
		return this.commands.zcount(k, string, string1);
	}

	@Override
	public Mono<Long> zcount(A k, Range<? extends Number> range) {
		return this.commands.zcount(k, range);
	}

	@Override
	public Flux<B> zdiff(A... ks) {
		return this.commands.zdiff(ks);
	}

	@Override
	public Mono<Long> zdiffstore(A k, A... ks) {
		return this.commands.zdiffstore(k, ks);
	}

	@Override
	public Flux<ScoredValue<B>> zdiffWithScores(A... ks) {
		return this.commands.zdiffWithScores(ks);
	}

	@Override
	public Mono<Double> zincrby(A k, double d, B v) {
		return this.commands.zincrby(k, d, v);
	}

	@Override
	public Flux<B> zinter(A... ks) {
		return this.commands.zinter(ks);
	}

	@Override
	public Flux<B> zinter(ZAggregateArgs zaa, A... ks) {
		return this.commands.zinter(zaa, ks);
	}

	@Override
	public Flux<ScoredValue<B>> zinterWithScores(ZAggregateArgs zaa, A... ks) {
		return this.commands.zinterWithScores(zaa, ks);
	}

	@Override
	public Flux<ScoredValue<B>> zinterWithScores(A... ks) {
		return this.commands.zinterWithScores(ks);
	}

	@Override
	public Mono<Long> zinterstore(A k, A... ks) {
		return this.commands.zinterstore(k, ks);
	}

	@Override
	public Mono<Long> zinterstore(A k, ZStoreArgs zsa, A... ks) {
		return this.commands.zinterstore(k, zsa, ks);
	}

	@Override
	public Mono<Long> zlexcount(A k, String string, String string1) {
		return this.commands.zlexcount(k, string, string1);
	}

	@Override
	public Mono<Long> zlexcount(A k, Range<? extends B> range) {
		return this.commands.zlexcount(k, range);
	}

	@Override
	public Mono<List<Double>> zmscore(A k, B... vs) {
		return this.commands.zmscore(k, vs);
	}

	@Override
	public Mono<ScoredValue<B>> zpopmin(A k) {
		return this.commands.zpopmin(k);
	}

	@Override
	public Flux<ScoredValue<B>> zpopmin(A k, long l) {
		return this.commands.zpopmin(k, l);
	}

	@Override
	public Mono<ScoredValue<B>> zpopmax(A k) {
		return this.commands.zpopmax(k);
	}

	@Override
	public Flux<ScoredValue<B>> zpopmax(A k, long l) {
		return this.commands.zpopmax(k, l);
	}

	@Override
	public Mono<B> zrandmember(A k) {
		return this.commands.zrandmember(k);
	}

	@Override
	public Flux<B> zrandmember(A k, long l) {
		return this.commands.zrandmember(k, l);
	}

	@Override
	public Mono<ScoredValue<B>> zrandmemberWithScores(A k) {
		return this.commands.zrandmemberWithScores(k);
	}

	@Override
	public Flux<ScoredValue<B>> zrandmemberWithScores(A k, long l) {
		return this.commands.zrandmemberWithScores(k, l);
	}

	@Override
	public Flux<B> zrange(A k, long l, long l1) {
		return this.commands.zrange(k, l, l1);
	}

	@Override
	public Mono<Long> zrange(ValueStreamingChannel<B> vsc, A k, long l, long l1) {
		return this.commands.zrange(vsc, k, l, l1);
	}

	@Override
	public Flux<ScoredValue<B>> zrangeWithScores(A k, long l, long l1) {
		return this.commands.zrangeWithScores(k, l, l1);
	}

	@Override
	public Mono<Long> zrangeWithScores(ScoredValueStreamingChannel<B> svsc, A k, long l, long l1) {
		return this.commands.zrangeWithScores(svsc, k, l, l1);
	}

	@Override
	public Flux<B> zrangebylex(A k, String string, String string1) {
		return this.commands.zrangebylex(k, string, string1);
	}

	@Override
	public Flux<B> zrangebylex(A k, Range<? extends B> range) {
		return this.commands.zrangebylex(k, range);
	}

	@Override
	public Flux<B> zrangebylex(A k, String string, String string1, long l, long l1) {
		return this.commands.zrangebylex(k, string, string1, l, l1);
	}

	@Override
	public Flux<B> zrangebylex(A k, Range<? extends B> range, Limit limit) {
		return this.commands.zrangebylex(k, range, limit);
	}

	@Override
	public Flux<B> zrangebyscore(A k, double d, double d1) {
		return this.commands.zrangebyscore(k, d, d1);
	}

	@Override
	public Flux<B> zrangebyscore(A k, String string, String string1) {
		return this.commands.zrangebyscore(k, string, string1);
	}

	@Override
	public Flux<B> zrangebyscore(A k, Range<? extends Number> range) {
		return this.commands.zrangebyscore(k, range);
	}

	@Override
	public Flux<B> zrangebyscore(A k, double d, double d1, long l, long l1) {
		return this.commands.zrangebyscore(k, d, d1, l, l1);
	}

	@Override
	public Flux<B> zrangebyscore(A k, String string, String string1, long l, long l1) {
		return this.commands.zrangebyscore(k, string, string1, l, l1);
	}

	@Override
	public Flux<B> zrangebyscore(A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrangebyscore(k, range, limit);
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1) {
		return this.commands.zrangebyscore(vsc, k, d, d1);
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1) {
		return this.commands.zrangebyscore(vsc, k, string, string1);
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range) {
		return this.commands.zrangebyscore(vsc, k, range);
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1, long l, long l1) {
		return this.commands.zrangebyscore(vsc, k, d, d1, l, l1);
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1, long l, long l1) {
		return this.commands.zrangebyscore(vsc, k, string, string1, l, l1);
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrangebyscore(vsc, k, range, limit);
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, double d, double d1) {
		return this.commands.zrangebyscoreWithScores(k, d, d1);
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, String string, String string1) {
		return this.commands.zrangebyscoreWithScores(k, string, string1);
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, Range<? extends Number> range) {
		return this.commands.zrangebyscoreWithScores(k, range);
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, double d, double d1, long l, long l1) {
		return this.commands.zrangebyscoreWithScores(k, d, d1, l, l1);
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, String string, String string1, long l, long l1) {
		return this.commands.zrangebyscoreWithScores(k, string, string1, l, l1);
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrangebyscoreWithScores(k, range, limit);
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1) {
		return this.commands.zrangebyscoreWithScores(svsc, k, d, d1);
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1) {
		return this.commands.zrangebyscoreWithScores(svsc, k, string, string1);
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range) {
		return this.commands.zrangebyscoreWithScores(svsc, k, range);
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1, long l, long l1) {
		return this.commands.zrangebyscoreWithScores(svsc, k, d, d1, l, l1);
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1, long l, long l1) {
		return this.commands.zrangebyscoreWithScores(svsc, k, string, string1, l, l1);
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrangebyscoreWithScores(svsc, k, range, limit);
	}

	@Override
	public Mono<Long> zrangestorebylex(A k, A k1, Range<? extends B> range, Limit limit) {
		return this.commands.zrangestorebylex(k, k1, range, limit);
	}

	@Override
	public Mono<Long> zrangestorebyscore(A k, A k1, Range<? extends Number> range, Limit limit) {
		return this.commands.zrangestorebyscore(k, k1, range, limit);
	}

	@Override
	public Mono<Long> zrank(A k, B v) {
		return this.commands.zrank(k, v);
	}

	@Override
	public Mono<Long> zrem(A k, B... vs) {
		return this.commands.zrem(k, vs);
	}

	@Override
	public Mono<Long> zremrangebylex(A k, String string, String string1) {
		return this.commands.zremrangebylex(k, string, string1);
	}

	@Override
	public Mono<Long> zremrangebylex(A k, Range<? extends B> range) {
		return this.commands.zremrangebylex(k, range);
	}

	@Override
	public Mono<Long> zremrangebyrank(A k, long l, long l1) {
		return this.commands.zremrangebyrank(k, l, l1);
	}

	@Override
	public Mono<Long> zremrangebyscore(A k, double d, double d1) {
		return this.commands.zremrangebyscore(k, d, d1);
	}

	@Override
	public Mono<Long> zremrangebyscore(A k, String string, String string1) {
		return this.commands.zremrangebyscore(k, string, string1);
	}

	@Override
	public Mono<Long> zremrangebyscore(A k, Range<? extends Number> range) {
		return this.commands.zremrangebyscore(k, range);
	}

	@Override
	public Flux<B> zrevrange(A k, long l, long l1) {
		return this.commands.zrevrange(k, l, l1);
	}

	@Override
	public Mono<Long> zrevrange(ValueStreamingChannel<B> vsc, A k, long l, long l1) {
		return this.commands.zrevrange(vsc, k, l, l1);
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangeWithScores(A k, long l, long l1) {
		return this.commands.zrevrangeWithScores(k, l, l1);
	}

	@Override
	public Mono<Long> zrevrangeWithScores(ScoredValueStreamingChannel<B> svsc, A k, long l, long l1) {
		return this.commands.zrevrangeWithScores(svsc, k, l, l1);
	}

	@Override
	public Flux<B> zrevrangebylex(A k, Range<? extends B> range) {
		return this.commands.zrevrangebylex(k, range);
	}

	@Override
	public Flux<B> zrevrangebylex(A k, Range<? extends B> range, Limit limit) {
		return this.commands.zrevrangebylex(k, range, limit);
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, double d, double d1) {
		return this.commands.zrevrangebyscore(k, d, d1);
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, String string, String string1) {
		return this.commands.zrevrangebyscore(k, string, string1);
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, Range<? extends Number> range) {
		return this.commands.zrevrangebyscore(k, range);
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, double d, double d1, long l, long l1) {
		return this.commands.zrevrangebyscore(k, d, d1, l, l1);
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, String string, String string1, long l, long l1) {
		return this.commands.zrevrangebyscore(k, string, string1, l, l1);
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrevrangebyscore(k, range, limit);
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1) {
		return this.commands.zrevrangebyscore(vsc, k, d, d1);
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1) {
		return this.commands.zrevrangebyscore(vsc, k, string, string1);
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range) {
		return this.commands.zrevrangebyscore(vsc, k, range);
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1, long l, long l1) {
		return this.commands.zrevrangebyscore(vsc, k, d, d1, l, l1);
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1, long l, long l1) {
		return this.commands.zrevrangebyscore(vsc, k, string, string1, l, l1);
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrevrangebyscore(vsc, k, range, limit);
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, double d, double d1) {
		return this.commands.zrevrangebyscoreWithScores(k, d, d1);
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, String string, String string1) {
		return this.commands.zrevrangebyscoreWithScores(k, string, string1);
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, Range<? extends Number> range) {
		return this.commands.zrevrangebyscoreWithScores(k, range);
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, double d, double d1, long l, long l1) {
		return this.commands.zrevrangebyscoreWithScores(k, d, d1, l, l1);
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, String string, String string1, long l, long l1) {
		return this.commands.zrevrangebyscoreWithScores(k, string, string1, l, l1);
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrevrangebyscoreWithScores(k, range, limit);
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1) {
		return this.commands.zrevrangebyscoreWithScores(svsc, k, d, d1);
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1) {
		return this.commands.zrevrangebyscoreWithScores(svsc, k, string, string1);
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range) {
		return this.commands.zrevrangebyscoreWithScores(svsc, k, range);
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1, long l, long l1) {
		return this.commands.zrevrangebyscoreWithScores(svsc, k, d, d1, l, l1);
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1, long l, long l1) {
		return this.commands.zrevrangebyscoreWithScores(svsc, k, string, string1, l, l1);
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range, Limit limit) {
		return this.commands.zrevrangebyscoreWithScores(svsc, k, range, limit);
	}

	@Override
	public Mono<Long> zrevrangestorebylex(A k, A k1, Range<? extends B> range, Limit limit) {
		return this.commands.zrevrangestorebylex(k, k1, range, limit);
	}

	@Override
	public Mono<Long> zrevrangestorebyscore(A k, A k1, Range<? extends Number> range, Limit limit) {
		return this.commands.zrevrangestorebyscore(k, k1, range, limit);
	}

	@Override
	public Mono<Long> zrevrank(A k, B v) {
		return this.commands.zrevrank(k, v);
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k) {
		return this.commands.zscan(k);
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k, ScanArgs sa) {
		return this.commands.zscan(k, sa);
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k, ScanCursor sc, ScanArgs sa) {
		return this.commands.zscan(k, sc, sa);
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k, ScanCursor sc) {
		return this.commands.zscan(k, sc);
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k) {
		return this.commands.zscan(svsc, k);
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k, ScanArgs sa) {
		return this.commands.zscan(svsc, k, sa);
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k, ScanCursor sc, ScanArgs sa) {
		return this.commands.zscan(svsc, k, sc, sa);
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k, ScanCursor sc) {
		return this.commands.zscan(svsc, k, sc);
	}

	@Override
	public Mono<Double> zscore(A k, B v) {
		return this.commands.zscore(k, v);
	}

	@Override
	public Flux<B> zunion(A... ks) {
		return this.commands.zunion(ks);
	}

	@Override
	public Flux<B> zunion(ZAggregateArgs zaa, A... ks) {
		return this.commands.zunion(zaa, ks);
	}

	@Override
	public Flux<ScoredValue<B>> zunionWithScores(ZAggregateArgs zaa, A... ks) {
		return this.commands.zunionWithScores(zaa, ks);
	}

	@Override
	public Flux<ScoredValue<B>> zunionWithScores(A... ks) {
		return this.commands.zunionWithScores(ks);
	}

	@Override
	public Mono<Long> zunionstore(A k, A... ks) {
		return this.commands.zunionstore(k, ks);
	}

	@Override
	public Mono<Long> zunionstore(A k, ZStoreArgs zsa, A... ks) {
		return this.commands.zunionstore(k, zsa, ks);
	}

	@Override
	public Mono<Long> xack(A k, A k1, String... strings) {
		return this.commands.xack(k, k1, strings);
	}

	@Override
	public Mono<String> xadd(A k, Map<A, B> map) {
		return this.commands.xadd(k, map);
	}

	@Override
	public Mono<String> xadd(A k, XAddArgs xaa, Map<A, B> map) {
		return this.commands.xadd(k, xaa, map);
	}

	@Override
	public Mono<String> xadd(A k, Object... os) {
		return this.commands.xadd(k, os);
	}

	@Override
	public Mono<String> xadd(A k, XAddArgs xaa, Object... os) {
		return this.commands.xadd(k, xaa, os);
	}

	@Override
	public Mono<ClaimedMessages<A, B>> xautoclaim(A k, XAutoClaimArgs<A> xaca) {
		return this.commands.xautoclaim(k, xaca);
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A k, Consumer<A> cnsmr, long l, String... strings) {
		return this.commands.xclaim(k, cnsmr, l, strings);
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A k, Consumer<A> cnsmr, XClaimArgs xca, String... strings) {
		return this.commands.xclaim(k, cnsmr, xca, strings);
	}

	@Override
	public Mono<Long> xdel(A k, String... strings) {
		return this.commands.xdel(k, strings);
	}

	@Override
	public Mono<String> xgroupCreate(XReadArgs.StreamOffset<A> so, A k) {
		return this.commands.xgroupCreate(so, k);
	}

	@Override
	public Mono<String> xgroupCreate(XReadArgs.StreamOffset<A> so, A k, XGroupCreateArgs xgca) {
		return this.commands.xgroupCreate(so, k, xgca);
	}

	@Override
	public Mono<Boolean> xgroupCreateconsumer(A k, Consumer<A> cnsmr) {
		return this.commands.xgroupCreateconsumer(k, cnsmr);
	}

	@Override
	public Mono<Long> xgroupDelconsumer(A k, Consumer<A> cnsmr) {
		return this.commands.xgroupDelconsumer(k, cnsmr);
	}

	@Override
	public Mono<Boolean> xgroupDestroy(A k, A k1) {
		return this.commands.xgroupDestroy(k, k1);
	}

	@Override
	public Mono<String> xgroupSetid(XReadArgs.StreamOffset<A> so, A k) {
		return this.commands.xgroupSetid(so, k);
	}

	@Override
	public Flux<Object> xinfoStream(A k) {
		return this.commands.xinfoStream(k);
	}

	@Override
	public Flux<Object> xinfoGroups(A k) {
		return this.commands.xinfoGroups(k);
	}

	@Override
	public Flux<Object> xinfoConsumers(A k, A k1) {
		return this.commands.xinfoConsumers(k, k1);
	}

	@Override
	public Mono<Long> xlen(A k) {
		return this.commands.xlen(k);
	}

	@Override
	public Mono<PendingMessages> xpending(A k, A k1) {
		return this.commands.xpending(k, k1);
	}

	@Override
	public Flux<PendingMessage> xpending(A k, A k1, Range<String> range, Limit limit) {
		return this.commands.xpending(k, k1, range, limit);
	}

	@Override
	public Flux<PendingMessage> xpending(A k, Consumer<A> cnsmr, Range<String> range, Limit limit) {
		return this.commands.xpending(k, cnsmr, range, limit);
	}

	@Override
	public Flux<PendingMessage> xpending(A k, XPendingArgs<A> xpa) {
		return this.commands.xpending(k, xpa);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A k, Range<String> range) {
		return this.commands.xrange(k, range);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A k, Range<String> range, Limit limit) {
		return this.commands.xrange(k, range, limit);
	}

	@Override
	public Flux<StreamMessage<A, B>> xread(XReadArgs.StreamOffset<A>... sos) {
		return this.commands.xread(sos);
	}

	@Override
	public Flux<StreamMessage<A, B>> xread(XReadArgs xra, XReadArgs.StreamOffset<A>... sos) {
		return this.commands.xread(xra, sos);
	}

	@Override
	public Flux<StreamMessage<A, B>> xreadgroup(Consumer<A> cnsmr, XReadArgs.StreamOffset<A>... sos) {
		return this.commands.xreadgroup(cnsmr, sos);
	}

	@Override
	public Flux<StreamMessage<A, B>> xreadgroup(Consumer<A> cnsmr, XReadArgs xra, XReadArgs.StreamOffset<A>... sos) {
		return this.commands.xreadgroup(cnsmr, xra, sos);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A k, Range<String> range) {
		return this.commands.xrevrange(k, range);
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A k, Range<String> range, Limit limit) {
		return this.commands.xrevrange(k, range, limit);
	}

	@Override
	public Mono<Long> xtrim(A k, long l) {
		return this.commands.xtrim(k, l);
	}

	@Override
	public Mono<Long> xtrim(A k, boolean bln, long l) {
		return this.commands.xtrim(k, bln, l);
	}

	@Override
	public Mono<Long> xtrim(A k, XTrimArgs xta) {
		return this.commands.xtrim(k, xta);
	}

	@Override
	public Mono<Long> append(A k, B v) {
		return this.commands.append(k, v);
	}

	@Override
	public Mono<Long> bitcount(A k) {
		return this.commands.bitcount(k);
	}

	@Override
	public Mono<Long> bitcount(A k, long l, long l1) {
		return this.commands.bitcount(k, l, l1);
	}

	@Override
	public Flux<Value<Long>> bitfield(A k, BitFieldArgs bfa) {
		return this.commands.bitfield(k, bfa);
	}

	@Override
	public Mono<Long> bitpos(A k, boolean bln) {
		return this.commands.bitpos(k, bln);
	}

	@Override
	public Mono<Long> bitpos(A k, boolean bln, long l) {
		return this.commands.bitpos(k, bln, l);
	}

	@Override
	public Mono<Long> bitpos(A k, boolean bln, long l, long l1) {
		return this.commands.bitpos(k, bln, l, l1);
	}

	@Override
	public Mono<Long> bitopAnd(A k, A... ks) {
		return this.commands.bitopAnd(k, ks);
	}

	@Override
	public Mono<Long> bitopNot(A k, A k1) {
		return this.commands.bitopNot(k, k1);
	}

	@Override
	public Mono<Long> bitopOr(A k, A... ks) {
		return this.commands.bitopOr(k, ks);
	}

	@Override
	public Mono<Long> bitopXor(A k, A... ks) {
		return this.commands.bitopXor(k, ks);
	}

	@Override
	public Mono<Long> decr(A k) {
		return this.commands.decr(k);
	}

	@Override
	public Mono<Long> decrby(A k, long l) {
		return this.commands.decrby(k, l);
	}

	@Override
	public Mono<B> get(A k) {
		return this.commands.get(k);
	}

	@Override
	public Mono<Long> getbit(A k, long l) {
		return this.commands.getbit(k, l);
	}

	@Override
	public Mono<B> getdel(A k) {
		return this.commands.getdel(k);
	}

	@Override
	public Mono<B> getex(A k, GetExArgs gea) {
		return this.commands.getex(k, gea);
	}

	@Override
	public Mono<B> getrange(A k, long l, long l1) {
		return this.commands.getrange(k, l, l1);
	}

	@Override
	public Mono<B> getset(A k, B v) {
		return this.commands.getset(k, v);
	}

	@Override
	public Mono<Long> incr(A k) {
		return this.commands.incr(k);
	}

	@Override
	public Mono<Long> incrby(A k, long l) {
		return this.commands.incrby(k, l);
	}

	@Override
	public Mono<Double> incrbyfloat(A k, double d) {
		return this.commands.incrbyfloat(k, d);
	}

	@Override
	public Mono<Long> mget(KeyValueStreamingChannel<A, B> kvsc, A... ks) {
		return this.commands.mget(kvsc, ks);
	}

	@Override
	public Mono<String> set(A k, B v) {
		return this.commands.set(k, v);
	}

	@Override
	public Mono<String> set(A k, B v, SetArgs sa) {
		return this.commands.set(k, v, sa);
	}

	@Override
	public Mono<B> setGet(A k, B v) {
		return this.commands.setGet(k, v);
	}

	@Override
	public Mono<B> setGet(A k, B v, SetArgs sa) {
		return this.commands.setGet(k, v, sa);
	}

	@Override
	public Mono<Long> setbit(A k, long l, int i) {
		return this.commands.setbit(k, l, i);
	}

	@Override
	public Mono<String> setex(A k, long l, B v) {
		return this.commands.setex(k, l, v);
	}

	@Override
	public Mono<String> psetex(A k, long l, B v) {
		return this.commands.psetex(k, l, v);
	}

	@Override
	public Mono<Boolean> setnx(A k, B v) {
		return this.commands.setnx(k, v);
	}

	@Override
	public Mono<Long> setrange(A k, long l, B v) {
		return this.commands.setrange(k, l, v);
	}

	@Override
	public Mono<StringMatchResult> stralgoLcs(StrAlgoArgs saa) {
		return this.commands.stralgoLcs(saa);
	}

	@Override
	public Mono<Long> strlen(A k) {
		return this.commands.strlen(k);
	}
	
	
}

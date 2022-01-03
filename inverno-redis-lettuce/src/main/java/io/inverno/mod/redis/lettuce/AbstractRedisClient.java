/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.lettuce;

import io.inverno.mod.redis.lettuce.internal.StatefulRedisConnectionOperations;
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
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public abstract class AbstractRedisClient<A, B, C extends StatefulConnection<A, B>> implements RedisClient<A, B> {

	protected final BoundedAsyncPool<C> pool;
	
	public AbstractRedisClient(BoundedAsyncPool<C> pool) {
		this.pool = pool;
	}
	
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
	public Mono<Long> geoadd(A k, double d, double d1, B v) {
		return Mono.from(this.connection(o -> o.geoadd(k, d, d1, v)));
	}

	@Override
	public Mono<Long> geoadd(A k, double d, double d1, B v, GeoAddArgs gaa) {
		return Mono.from(this.connection(o -> o.geoadd(k, d, d1, v, gaa)));
	}

	@Override
	public Mono<Long> geoadd(A k, Object... os) {
		return Mono.from(this.connection(o -> o.geoadd(k, os)));
	}

	@Override
	public Mono<Long> geoadd(A k, GeoValue<B>... gvs) {
		return Mono.from(this.connection(o -> o.geoadd(k, gvs)));
	}

	@Override
	public Mono<Long> geoadd(A k, GeoAddArgs gaa, Object... os) {
		return Mono.from(this.connection(o -> o.geoadd(k, gaa, os)));
	}

	@Override
	public Mono<Long> geoadd(A k, GeoAddArgs gaa, GeoValue<B>... gvs) {
		return Mono.from(this.connection(o -> o.geoadd(k, gaa, gvs)));
	}

	@Override
	public Mono<Double> geodist(A k, B v, B v1, GeoArgs.Unit unit) {
		return Mono.from(this.connection(o -> o.geodist(k, v, v1, unit)));
	}

	@Override
	public Flux<Value<String>> geohash(A k, B... vs) {
		return Flux.from(this.connection(o -> o.geohash(k, vs)));
	}

	@Override
	public Flux<Value<GeoCoordinates>> geopos(A k, B... vs) {
		return Flux.from(this.connection(o -> o.geopos(k, vs)));
	}

	@Override
	public Flux<B> georadius(A k, double d, double d1, double d2, GeoArgs.Unit unit) {
		return Flux.from(this.connection(o -> o.georadius(k, d, d1, d2, unit)));
	}

	@Override
	public Flux<GeoWithin<B>> georadius(A k, double d, double d1, double d2, GeoArgs.Unit unit, GeoArgs ga) {
		return Flux.from(this.connection(o -> o.georadius(k, d, d1, d2, unit, ga)));
	}

	@Override
	public Mono<Long> georadius(A k, double d, double d1, double d2, GeoArgs.Unit unit, GeoRadiusStoreArgs<A> grsa) {
		return Mono.from(this.connection(o -> o.georadius(k, d, d1, d2, unit, grsa)));
	}

	@Override
	public Flux<B> georadiusbymember(A k, B v, double d, GeoArgs.Unit unit) {
		return Flux.from(this.connection(o -> o.georadiusbymember(k, v, d, unit)));
	}

	@Override
	public Flux<GeoWithin<B>> georadiusbymember(A k, B v, double d, GeoArgs.Unit unit, GeoArgs ga) {
		return Flux.from(this.connection(o -> o.georadiusbymember(k, v, d, unit, ga)));
	}

	@Override
	public Mono<Long> georadiusbymember(A k, B v, double d, GeoArgs.Unit unit, GeoRadiusStoreArgs<A> grsa) {
		return Mono.from(this.connection(o -> o.georadiusbymember(k, v, d, unit, grsa)));
	}

	@Override
	public Flux<B> geosearch(A k, GeoSearch.GeoRef<A> georef, GeoSearch.GeoPredicate gp) {
		return Flux.from(this.connection(o -> o.geosearch(k, georef, gp)));
	}

	@Override
	public Flux<GeoWithin<B>> geosearch(A k, GeoSearch.GeoRef<A> georef, GeoSearch.GeoPredicate gp, GeoArgs ga) {
		return Flux.from(this.connection(o -> o.geosearch(k, georef, gp, ga)));
	}

	@Override
	public Mono<Long> geosearchstore(A k, A k1, GeoSearch.GeoRef<A> georef, GeoSearch.GeoPredicate gp, GeoArgs ga, boolean bln) {
		return Mono.from(this.connection(o -> o.geosearchstore(k, k1, georef, gp, ga, bln)));
	}

	@Override
	public Mono<Long> hdel(A k, A... ks) {
		return Mono.from(this.connection(o -> o.hdel(k, ks)));
	}

	@Override
	public Mono<Boolean> hexists(A k, A k1) {
		return Mono.from(this.connection(o -> o.hexists(k, k1)));
	}

	@Override
	public Mono<B> hget(A k, A k1) {
		return Mono.from(this.connection(o -> o.hget(k, k1)));
	}

	@Override
	public Mono<Long> hincrby(A k, A k1, long l) {
		return Mono.from(this.connection(o -> o.hincrby(k, k1, l)));
	}

	@Override
	public Mono<Double> hincrbyfloat(A k, A k1, double d) {
		return Mono.from(this.connection(o -> o.hincrbyfloat(k, k1, d)));
	}

	@Override
	public Flux<KeyValue<A, B>> hgetall(A k) {
		return Flux.from(this.connection(o -> o.hgetall(k)));
	}

	@Override
	public Mono<Long> hgetall(KeyValueStreamingChannel<A, B> kvsc, A k) {
		return Mono.from(this.connection(o -> o.hgetall(kvsc, k)));
	}

	@Override
	public Flux<A> hkeys(A k) {
		return Flux.from(this.connection(o -> o.hkeys(k)));
	}

	@Override
	public Mono<Long> hkeys(KeyStreamingChannel<A> ksc, A k) {
		return Mono.from(this.connection(o -> o.hkeys(ksc, k)));
	}

	@Override
	public Mono<Long> hlen(A k) {
		return Mono.from(this.connection(o -> o.hlen(k)));
	}

	@Override
	public Flux<KeyValue<A, B>> hmget(A k, A... ks) {
		return Flux.from(this.connection(o -> o.hmget(k, ks)));
	}

	@Override
	public Mono<Long> hmget(KeyValueStreamingChannel<A, B> kvsc, A k, A... ks) {
		return Mono.from(this.connection(o -> o.hmget(kvsc, k, ks)));
	}

	@Override
	public Mono<String> hmset(A k, Map<A, B> map) {
		return Mono.from(this.connection(o -> o.hmset(k, map)));
	}

	@Override
	public Mono<A> hrandfield(A k) {
		return Mono.from(this.connection(o -> o.hrandfield(k)));
	}

	@Override
	public Flux<A> hrandfield(A k, long l) {
		return Flux.from(this.connection(o -> o.hrandfield(k, l)));
	}

	@Override
	public Mono<KeyValue<A, B>> hrandfieldWithvalues(A k) {
		return Mono.from(this.connection(o -> o.hrandfieldWithvalues(k)));
	}

	@Override
	public Flux<KeyValue<A, B>> hrandfieldWithvalues(A k, long l) {
		return Flux.from(this.connection(o -> o.hrandfieldWithvalues(k, l)));
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k) {
		return Mono.from(this.connection(o -> o.hscan(k)));
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.hscan(k, sa)));
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k, ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.hscan(k, sc, sa)));
	}

	@Override
	public Mono<MapScanCursor<A, B>> hscan(A k, ScanCursor sc) {
		return Mono.from(this.connection(o -> o.hscan(k, sc)));
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k) {
		return Mono.from(this.connection(o -> o.hscan(kvsc, k)));
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.hscan(kvsc, k, sa)));
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k, ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.hscan(kvsc, k, sc, sa)));
	}

	@Override
	public Mono<StreamScanCursor> hscan(KeyValueStreamingChannel<A, B> kvsc, A k, ScanCursor sc) {
		return Mono.from(this.connection(o -> o.hscan(kvsc, k, sc)));
	}

	@Override
	public Mono<Boolean> hset(A k, A k1, B v) {
		return Mono.from(this.connection(o -> o.hset(k, k1, v)));
	}

	@Override
	public Mono<Long> hset(A k, Map<A, B> map) {
		return Mono.from(this.connection(o -> o.hset(k, map)));
	}

	@Override
	public Mono<Boolean> hsetnx(A k, A k1, B v) {
		return Mono.from(this.connection(o -> o.hsetnx(k, k1, v)));
	}

	@Override
	public Mono<Long> hstrlen(A k, A k1) {
		return Mono.from(this.connection(o -> o.hstrlen(k, k1)));
	}

	@Override
	public Flux<B> hvals(A k) {
		return Flux.from(this.connection(o -> o.hvals(k)));
	}

	@Override
	public Mono<Long> hvals(ValueStreamingChannel<B> vsc, A k) {
		return Mono.from(this.connection(o -> o.hvals(vsc, k)));
	}

	@Override
	public Mono<Long> pfadd(A k, B... vs) {
		return Mono.from(this.connection(o -> o.pfadd(k, vs)));
	}

	@Override
	public Mono<String> pfmerge(A k, A... ks) {
		return Mono.from(this.connection(o -> o.pfmerge(k, ks)));
	}

	@Override
	public Mono<Long> pfcount(A... ks) {
		return Mono.from(this.connection(o -> o.pfcount(ks)));
	}

	@Override
	public Mono<Boolean> copy(A k, A k1) {
		return Mono.from(this.connection(o -> o.copy(k, k1)));
	}

	@Override
	public Mono<Boolean> copy(A k, A k1, CopyArgs ca) {
		return Mono.from(this.connection(o -> o.copy(k, k1, ca)));
	}

	@Override
	public Mono<Long> del(A... ks) {
		return Mono.from(this.connection(o -> o.del(ks)));
	}

	@Override
	public Mono<Long> unlink(A... ks) {
		return Mono.from(this.connection(o -> o.unlink(ks)));
	}

	@Override
	public Mono<byte[]> dump(A k) {
		return Mono.from(this.connection(o -> o.dump(k)));
	}

	@Override
	public Mono<Long> exists(A... ks) {
		return Mono.from(this.connection(o -> o.exists(ks)));
	}

	@Override
	public Mono<Boolean> expire(A k, long l) {
		return Mono.from(this.connection(o -> o.expire(k, l)));
	}

	@Override
	public Mono<Boolean> expire(A k, Duration drtn) {
		return Mono.from(this.connection(o -> o.expire(k, drtn)));
	}

	@Override
	public Mono<Boolean> expireat(A k, long l) {
		return Mono.from(this.connection(o -> o.expireat(k, l)));
	}

	@Override
	public Mono<Boolean> expireat(A k, Date date) {
		return Mono.from(this.connection(o -> o.expireat(k, date)));
	}

	@Override
	public Mono<Boolean> expireat(A k, Instant instnt) {
		return Mono.from(this.connection(o -> o.expireat(k, instnt)));
	}

	@Override
	public Flux<A> keys(A k) {
		return Flux.from(this.connection(o -> o.keys(k)));
	}

	@Override
	public Mono<Long> keys(KeyStreamingChannel<A> ksc, A k) {
		return Mono.from(this.connection(o -> o.keys(ksc, k)));
	}

	@Override
	public Mono<String> migrate(String string, int i, A k, int i1, long l) {
		return Mono.from(this.connection(o -> o.migrate(string, i, k, i1, l)));
	}

	@Override
	public Mono<String> migrate(String string, int i, int i1, long l, MigrateArgs<A> ma) {
		return Mono.from(this.connection(o -> o.migrate(string, i, i1, l, ma)));
	}

	@Override
	public Mono<Boolean> move(A k, int i) {
		return Mono.from(this.connection(o -> o.move(k, i)));
	}

	@Override
	public Mono<String> objectEncoding(A k) {
		return Mono.from(this.connection(o -> o.objectEncoding(k)));
	}

	@Override
	public Mono<Long> objectFreq(A k) {
		return Mono.from(this.connection(o -> o.objectFreq(k)));
	}

	@Override
	public Mono<Long> objectIdletime(A k) {
		return Mono.from(this.connection(o -> o.objectIdletime(k)));
	}

	@Override
	public Mono<Long> objectRefcount(A k) {
		return Mono.from(this.connection(o -> o.objectRefcount(k)));
	}

	@Override
	public Mono<Boolean> persist(A k) {
		return Mono.from(this.connection(o -> o.persist(k)));
	}

	@Override
	public Mono<Boolean> pexpire(A k, long l) {
		return Mono.from(this.connection(o -> o.pexpire(k, l)));
	}

	@Override
	public Mono<Boolean> pexpire(A k, Duration drtn) {
		return Mono.from(this.connection(o -> o.pexpire(k, drtn)));
	}

	@Override
	public Mono<Boolean> pexpireat(A k, long l) {
		return Mono.from(this.connection(o -> o.pexpireat(k, l)));
	}

	@Override
	public Mono<Boolean> pexpireat(A k, Date date) {
		return Mono.from(this.connection(o -> o.pexpireat(k, date)));
	}

	@Override
	public Mono<Boolean> pexpireat(A k, Instant instnt) {
		return Mono.from(this.connection(o -> o.pexpireat(k, instnt)));
	}

	@Override
	public Mono<Long> pttl(A k) {
		return Mono.from(this.connection(o -> o.pttl(k)));
	}

	@Override
	public Mono<A> randomkey() {
		return Mono.from(this.connection(o -> o.randomkey()));
	}

	@Override
	public Mono<String> rename(A k, A k1) {
		return Mono.from(this.connection(o -> o.rename(k, k1)));
	}

	@Override
	public Mono<Boolean> renamenx(A k, A k1) {
		return Mono.from(this.connection(o -> o.renamenx(k, k1)));
	}

	@Override
	public Mono<String> restore(A k, long l, byte[] bytes) {
		return Mono.from(this.connection(o -> o.restore(k, l, bytes)));
	}

	@Override
	public Mono<String> restore(A k, byte[] bytes, RestoreArgs ra) {
		return Mono.from(this.connection(o -> o.restore(k, bytes, ra)));
	}

	@Override
	public Flux<B> sort(A k) {
		return Flux.from(this.connection(o -> o.sort(k)));
	}

	@Override
	public Mono<Long> sort(ValueStreamingChannel<B> vsc, A k) {
		return Mono.from(this.connection(o -> o.sort(vsc, k)));
	}

	@Override
	public Flux<B> sort(A k, SortArgs sa) {
		return Flux.from(this.connection(o -> o.sort(k, sa)));
	}

	@Override
	public Mono<Long> sort(ValueStreamingChannel<B> vsc, A k, SortArgs sa) {
		return Mono.from(this.connection(o -> o.sort(vsc, k, sa)));
	}

	@Override
	public Mono<Long> sortStore(A k, SortArgs sa, A k1) {
		return Mono.from(this.connection(o -> o.sortStore(k, sa, k1)));
	}

	@Override
	public Mono<Long> touch(A... ks) {
		return Mono.from(this.connection(o -> o.touch(ks)));
	}

	@Override
	public Mono<Long> ttl(A k) {
		return Mono.from(this.connection(o -> o.ttl(k)));
	}

	@Override
	public Mono<String> type(A k) {
		return Mono.from(this.connection(o -> o.type(k)));
	}

	@Override
	public Mono<KeyScanCursor<A>> scan() {
		return Mono.from(this.connection(o -> o.scan()));
	}

	@Override
	public Mono<KeyScanCursor<A>> scan(ScanArgs sa) {
		return Mono.from(this.connection(o -> o.scan(sa)));
	}

	@Override
	public Mono<KeyScanCursor<A>> scan(ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.scan(sc, sa)));
	}

	@Override
	public Mono<KeyScanCursor<A>> scan(ScanCursor sc) {
		return Mono.from(this.connection(o -> o.scan(sc)));
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc) {
		return Mono.from(this.connection(o -> o.scan(ksc)));
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.scan(ksc, sa)));
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc, ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.scan(ksc, sc, sa)));
	}

	@Override
	public Mono<StreamScanCursor> scan(KeyStreamingChannel<A> ksc, ScanCursor sc) {
		return Mono.from(this.connection(o -> o.scan(ksc, sc)));
	}

	@Override
	public Mono<B> blmove(A k, A k1, LMoveArgs lma, long l) {
		return Mono.from(this.connection(o -> o.blmove(k, k1, lma, l)));
	}

	@Override
	public Mono<B> blmove(A k, A k1, LMoveArgs lma, double d) {
		return Mono.from(this.connection(o -> o.blmove(k, k1, lma, d)));
	}

	@Override
	public Mono<KeyValue<A, B>> blpop(long l, A... ks) {
		return Mono.from(this.connection(o -> o.blpop(l, ks)));
	}

	@Override
	public Mono<KeyValue<A, B>> blpop(double d, A... ks) {
		return Mono.from(this.connection(o -> o.blpop(d, ks)));
	}

	@Override
	public Mono<KeyValue<A, B>> brpop(long l, A... ks) {
		return Mono.from(this.connection(o -> o.brpop(l, ks)));
	}

	@Override
	public Mono<KeyValue<A, B>> brpop(double d, A... ks) {
		return Mono.from(this.connection(o -> o.brpop(d, ks)));
	}

	@Override
	public Mono<B> brpoplpush(long l, A k, A k1) {
		return Mono.from(this.connection(o -> o.brpoplpush(l, k, k1)));
	}

	@Override
	public Mono<B> brpoplpush(double d, A k, A k1) {
		return Mono.from(this.connection(o -> o.brpoplpush(d, k, k1)));
	}

	@Override
	public Mono<B> lindex(A k, long l) {
		return Mono.from(this.connection(o -> o.lindex(k, l)));
	}

	@Override
	public Mono<Long> linsert(A k, boolean bln, B v, B v1) {
		return Mono.from(this.connection(o -> o.linsert(k, bln, v, v1)));
	}

	@Override
	public Mono<Long> llen(A k) {
		return Mono.from(this.connection(o -> o.llen(k)));
	}

	@Override
	public Mono<B> lmove(A k, A k1, LMoveArgs lma) {
		return Mono.from(this.connection(o -> o.lmove(k, k1, lma)));
	}

	@Override
	public Mono<B> lpop(A k) {
		return Mono.from(this.connection(o -> o.lpop(k)));
	}

	@Override
	public Flux<B> lpop(A k, long l) {
		return Flux.from(this.connection(o -> o.lpop(k, l)));
	}

	@Override
	public Mono<Long> lpos(A k, B v) {
		return Mono.from(this.connection(o -> o.lpos(k, v)));
	}

	@Override
	public Mono<Long> lpos(A k, B v, LPosArgs lpa) {
		return Mono.from(this.connection(o -> o.lpos(k, v, lpa)));
	}

	@Override
	public Flux<Long> lpos(A k, B v, int i) {
		return Flux.from(this.connection(o -> o.lpos(k, v, i)));
	}

	@Override
	public Flux<Long> lpos(A k, B v, int i, LPosArgs lpa) {
		return Flux.from(this.connection(o -> o.lpos(k, v, i, lpa)));
	}

	@Override
	public Mono<Long> lpush(A k, B... vs) {
		return Mono.from(this.connection(o -> o.lpush(k, vs)));
	}

	@Override
	public Mono<Long> lpushx(A k, B... vs) {
		return Mono.from(this.connection(o -> o.lpushx(k, vs)));
	}

	@Override
	public Flux<B> lrange(A k, long l, long l1) {
		return Flux.from(this.connection(o -> o.lrange(k, l, l1)));
	}

	@Override
	public Mono<Long> lrange(ValueStreamingChannel<B> vsc, A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.lrange(vsc, k, l, l1)));
	}

	@Override
	public Mono<Long> lrem(A k, long l, B v) {
		return Mono.from(this.connection(o -> o.lrem(k, l, v)));
	}

	@Override
	public Mono<String> lset(A k, long l, B v) {
		return Mono.from(this.connection(o -> o.lset(k, l, v)));
	}

	@Override
	public Mono<String> ltrim(A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.ltrim(k, l, l1)));
	}

	@Override
	public Mono<B> rpop(A k) {
		return Mono.from(this.connection(o -> o.rpop(k)));
	}

	@Override
	public Flux<B> rpop(A k, long l) {
		return Flux.from(this.connection(o -> o.rpop(k, l)));
	}

	@Override
	public Mono<B> rpoplpush(A k, A k1) {
		return Mono.from(this.connection(o -> o.rpoplpush(k, k1)));
	}

	@Override
	public Mono<Long> rpush(A k, B... vs) {
		return Mono.from(this.connection(o -> o.rpush(k, vs)));
	}

	@Override
	public Mono<Long> rpushx(A k, B... vs) {
		return Mono.from(this.connection(o -> o.rpushx(k, vs)));
	}

	@Override
	public <T> Flux<T> eval(String string, ScriptOutputType sot, A... ks) {
		return Flux.from(this.connection(o -> o.eval(string, sot, ks)));
	}

	@Override
	public <T> Flux<T> eval(byte[] bytes, ScriptOutputType sot, A... ks) {
		return Flux.from(this.connection(o -> o.eval(bytes, sot, ks)));
	}

	@Override
	public <T> Flux<T> eval(String string, ScriptOutputType sot, A[] ks, B... vs) {
		return Flux.from(this.connection(o -> o.eval(string, sot, ks, vs)));
	}

	@Override
	public <T> Flux<T> eval(byte[] bytes, ScriptOutputType sot, A[] ks, B... vs) {
		return Flux.from(this.connection(o -> o.eval(bytes, sot, ks, vs)));
	}

	@Override
	public <T> Flux<T> evalsha(String string, ScriptOutputType sot, A... ks) {
		return Flux.from(this.connection(o -> o.evalsha(string, sot, ks)));
	}

	@Override
	public <T> Flux<T> evalsha(String string, ScriptOutputType sot, A[] ks, B... vs) {
		return Flux.from(this.connection(o -> o.evalsha(string, sot, ks, vs)));
	}

	@Override
	public Flux<Boolean> scriptExists(String... strings) {
		return Flux.from(this.connection(o -> o.scriptExists(strings)));
	}

	@Override
	public Mono<String> scriptFlush() {
		return Mono.from(this.connection(o -> o.scriptFlush()));
	}

	@Override
	public Mono<String> scriptFlush(FlushMode fm) {
		return Mono.from(this.connection(o -> o.scriptFlush(fm)));
	}

	@Override
	public Mono<String> scriptKill() {
		return Mono.from(this.connection(o -> o.scriptKill()));
	}

	@Override
	public Mono<String> scriptLoad(String string) {
		return Mono.from(this.connection(o -> o.scriptLoad(string)));
	}

	@Override
	public Mono<String> scriptLoad(byte[] bytes) {
		return Mono.from(this.connection(o -> o.scriptLoad(bytes)));
	}

	@Override
	public String digest(String script) {
		return Mono.from(this.connection(o -> Mono.just(o.digest(script)))).block();
	}

	@Override
	public String digest(byte[] script) {
		return Mono.from(this.connection(o -> Mono.just(o.digest(script)))).block();
	}

	@Override
	public Mono<Long> sadd(A k, B... vs) {
		return Mono.from(this.connection(o -> o.sadd(k, vs)));
	}

	@Override
	public Mono<Long> scard(A k) {
		return Mono.from(this.connection(o -> o.scard(k)));
	}

	@Override
	public Flux<B> sdiff(A... ks) {
		return Flux.from(this.connection(o -> o.sdiff(ks)));
	}

	@Override
	public Mono<Long> sdiff(ValueStreamingChannel<B> vsc, A... ks) {
		return Mono.from(this.connection(o -> o.sdiff(vsc, ks)));
	}

	@Override
	public Mono<Long> sdiffstore(A k, A... ks) {
		return Mono.from(this.connection(o -> o.sdiffstore(k, ks)));
	}

	@Override
	public Flux<B> sinter(A... ks) {
		return Flux.from(this.connection(o -> o.sinter(ks)));
	}

	@Override
	public Mono<Long> sinter(ValueStreamingChannel<B> vsc, A... ks) {
		return Mono.from(this.connection(o -> o.sinter(vsc, ks)));
	}

	@Override
	public Mono<Long> sinterstore(A k, A... ks) {
		return Mono.from(this.connection(o -> o.sinterstore(k, ks)));
	}

	@Override
	public Mono<Boolean> sismember(A k, B v) {
		return Mono.from(this.connection(o -> o.sismember(k, v)));
	}

	@Override
	public Flux<B> smembers(A k) {
		return Flux.from(this.connection(o -> o.smembers(k)));
	}

	@Override
	public Mono<Long> smembers(ValueStreamingChannel<B> vsc, A k) {
		return Mono.from(this.connection(o -> o.smembers(vsc, k)));
	}

	@Override
	public Flux<Boolean> smismember(A k, B... vs) {
		return Flux.from(this.connection(o -> o.smismember(k, vs)));
	}

	@Override
	public Mono<Boolean> smove(A k, A k1, B v) {
		return Mono.from(this.connection(o -> o.smove(k, k1, v)));
	}

	@Override
	public Mono<B> spop(A k) {
		return Mono.from(this.connection(o -> o.spop(k)));
	}

	@Override
	public Flux<B> spop(A k, long l) {
		return Flux.from(this.connection(o -> o.spop(k, l)));
	}

	@Override
	public Mono<B> srandmember(A k) {
		return Mono.from(this.connection(o -> o.srandmember(k)));
	}

	@Override
	public Flux<B> srandmember(A k, long l) {
		return Flux.from(this.connection(o -> o.srandmember(k, l)));
	}

	@Override
	public Mono<Long> srandmember(ValueStreamingChannel<B> vsc, A k, long l) {
		return Mono.from(this.connection(o -> o.srandmember(vsc, k, l)));
	}

	@Override
	public Mono<Long> srem(A k, B... vs) {
		return Mono.from(this.connection(o -> o.srem(k, vs)));
	}

	@Override
	public Flux<B> sunion(A... ks) {
		return Flux.from(this.connection(o -> o.sunion(ks)));
	}

	@Override
	public Mono<Long> sunion(ValueStreamingChannel<B> vsc, A... ks) {
		return Mono.from(this.connection(o -> o.sunion(vsc, ks)));
	}

	@Override
	public Mono<Long> sunionstore(A k, A... ks) {
		return Mono.from(this.connection(o -> o.sunionstore(k, ks)));
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k) {
		return Mono.from(this.connection(o -> o.sscan(k)));
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.sscan(k, sa)));
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k, ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.sscan(k, sc, sa)));
	}

	@Override
	public Mono<ValueScanCursor<B>> sscan(A k, ScanCursor sc) {
		return Mono.from(this.connection(o -> o.sscan(k, sc)));
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k) {
		return Mono.from(this.connection(o -> o.sscan(vsc, k)));
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.sscan(vsc, k, sa)));
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k, ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.sscan(vsc, k, sc, sa)));
	}

	@Override
	public Mono<StreamScanCursor> sscan(ValueStreamingChannel<B> vsc, A k, ScanCursor sc) {
		return Mono.from(this.connection(o -> o.sscan(vsc, k, sc)));
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmin(long l, A... ks) {
		return Mono.from(this.connection(o -> o.bzpopmin(l, ks)));
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmin(double d, A... ks) {
		return Mono.from(this.connection(o -> o.bzpopmin(d, ks)));
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmax(long l, A... ks) {
		return Mono.from(this.connection(o -> o.bzpopmax(l, ks)));
	}

	@Override
	public Mono<KeyValue<A, ScoredValue<B>>> bzpopmax(double d, A... ks) {
		return Mono.from(this.connection(o -> o.bzpopmax(d, ks)));
	}

	@Override
	public Mono<Long> zadd(A k, double d, B v) {
		return Mono.from(this.connection(o -> o.zadd(k, d, v)));
	}

	@Override
	public Mono<Long> zadd(A k, Object... os) {
		return Mono.from(this.connection(o -> o.zadd(k, os)));
	}

	@Override
	public Mono<Long> zadd(A k, ScoredValue<B>... svs) {
		return Mono.from(this.connection(o -> o.zadd(k, svs)));
	}

	@Override
	public Mono<Long> zadd(A k, ZAddArgs zaa, double d, B v) {
		return Mono.from(this.connection(o -> o.zadd(k, zaa, d, v)));
	}

	@Override
	public Mono<Long> zadd(A k, ZAddArgs zaa, Object... os) {
		return Mono.from(this.connection(o -> o.zadd(k, zaa, os)));
	}

	@Override
	public Mono<Long> zadd(A k, ZAddArgs zaa, ScoredValue<B>... svs) {
		return Mono.from(this.connection(o -> o.zadd(k, zaa, svs)));
	}

	@Override
	public Mono<Double> zaddincr(A k, double d, B v) {
		return Mono.from(this.connection(o -> o.zaddincr(k, d, v)));
	}

	@Override
	public Mono<Double> zaddincr(A k, ZAddArgs zaa, double d, B v) {
		return Mono.from(this.connection(o -> o.zaddincr(k, zaa, d, v)));
	}

	@Override
	public Mono<Long> zcard(A k) {
		return Mono.from(this.connection(o -> o.zcard(k)));
	}

	@Override
	public Mono<Long> zcount(A k, double d, double d1) {
		return Mono.from(this.connection(o -> o.zcount(k, d, d1)));
	}

	@Override
	public Mono<Long> zcount(A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zcount(k, string, string1)));
	}

	@Override
	public Mono<Long> zcount(A k, Range<? extends Number> range) {
		return Mono.from(this.connection(o -> o.zcount(k, range)));
	}

	@Override
	public Flux<B> zdiff(A... ks) {
		return Flux.from(this.connection(o -> o.zdiff(ks)));
	}

	@Override
	public Mono<Long> zdiffstore(A k, A... ks) {
		return Mono.from(this.connection(o -> o.zdiffstore(k, ks)));
	}

	@Override
	public Flux<ScoredValue<B>> zdiffWithScores(A... ks) {
		return Flux.from(this.connection(o -> o.zdiffWithScores(ks)));
	}

	@Override
	public Mono<Double> zincrby(A k, double d, B v) {
		return Mono.from(this.connection(o -> o.zincrby(k, d, v)));
	}

	@Override
	public Flux<B> zinter(A... ks) {
		return Flux.from(this.connection(o -> o.zinter(ks)));
	}

	@Override
	public Flux<B> zinter(ZAggregateArgs zaa, A... ks) {
		return Flux.from(this.connection(o -> o.zinter(zaa, ks)));
	}

	@Override
	public Flux<ScoredValue<B>> zinterWithScores(ZAggregateArgs zaa, A... ks) {
		return Flux.from(this.connection(o -> o.zinterWithScores(zaa, ks)));
	}

	@Override
	public Flux<ScoredValue<B>> zinterWithScores(A... ks) {
		return Flux.from(this.connection(o -> o.zinterWithScores(ks)));
	}

	@Override
	public Mono<Long> zinterstore(A k, A... ks) {
		return Mono.from(this.connection(o -> o.zinterstore(k, ks)));
	}

	@Override
	public Mono<Long> zinterstore(A k, ZStoreArgs zsa, A... ks) {
		return Mono.from(this.connection(o -> o.zinterstore(k, zsa, ks)));
	}

	@Override
	public Mono<Long> zlexcount(A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zlexcount(k, string, string1)));
	}

	@Override
	public Mono<Long> zlexcount(A k, Range<? extends B> range) {
		return Mono.from(this.connection(o -> o.zlexcount(k, range)));
	}

	@Override
	public Mono<List<Double>> zmscore(A k, B... vs) {
		return Mono.from(this.connection(o -> o.zmscore(k, vs)));
	}

	@Override
	public Mono<ScoredValue<B>> zpopmin(A k) {
		return Mono.from(this.connection(o -> o.zpopmin(k)));
	}

	@Override
	public Flux<ScoredValue<B>> zpopmin(A k, long l) {
		return Flux.from(this.connection(o -> o.zpopmin(k, l)));
	}

	@Override
	public Mono<ScoredValue<B>> zpopmax(A k) {
		return Mono.from(this.connection(o -> o.zpopmax(k)));
	}

	@Override
	public Flux<ScoredValue<B>> zpopmax(A k, long l) {
		return Flux.from(this.connection(o -> o.zpopmax(k, l)));
	}

	@Override
	public Mono<B> zrandmember(A k) {
		return Mono.from(this.connection(o -> o.zrandmember(k)));
	}

	@Override
	public Flux<B> zrandmember(A k, long l) {
		return Flux.from(this.connection(o -> o.zrandmember(k, l)));
	}

	@Override
	public Mono<ScoredValue<B>> zrandmemberWithScores(A k) {
		return Mono.from(this.connection(o -> o.zrandmemberWithScores(k)));
	}

	@Override
	public Flux<ScoredValue<B>> zrandmemberWithScores(A k, long l) {
		return Flux.from(this.connection(o -> o.zrandmemberWithScores(k, l)));
	}

	@Override
	public Flux<B> zrange(A k, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrange(k, l, l1)));
	}

	@Override
	public Mono<Long> zrange(ValueStreamingChannel<B> vsc, A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrange(vsc, k, l, l1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrangeWithScores(A k, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrangeWithScores(k, l, l1)));
	}

	@Override
	public Mono<Long> zrangeWithScores(ScoredValueStreamingChannel<B> svsc, A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrangeWithScores(svsc, k, l, l1)));
	}

	@Override
	public Flux<B> zrangebylex(A k, String string, String string1) {
		return Flux.from(this.connection(o -> o.zrangebylex(k, string, string1)));
	}

	@Override
	public Flux<B> zrangebylex(A k, Range<? extends B> range) {
		return Flux.from(this.connection(o -> o.zrangebylex(k, range)));
	}

	@Override
	public Flux<B> zrangebylex(A k, String string, String string1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrangebylex(k, string, string1, l, l1)));
	}

	@Override
	public Flux<B> zrangebylex(A k, Range<? extends B> range, Limit limit) {
		return Flux.from(this.connection(o -> o.zrangebylex(k, range, limit)));
	}

	@Override
	public Flux<B> zrangebyscore(A k, double d, double d1) {
		return Flux.from(this.connection(o -> o.zrangebyscore(k, d, d1)));
	}

	@Override
	public Flux<B> zrangebyscore(A k, String string, String string1) {
		return Flux.from(this.connection(o -> o.zrangebyscore(k, string, string1)));
	}

	@Override
	public Flux<B> zrangebyscore(A k, Range<? extends Number> range) {
		return Flux.from(this.connection(o -> o.zrangebyscore(k, range)));
	}

	@Override
	public Flux<B> zrangebyscore(A k, double d, double d1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrangebyscore(k, d, d1, l, l1)));
	}

	@Override
	public Flux<B> zrangebyscore(A k, String string, String string1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrangebyscore(k, string, string1, l, l1)));
	}

	@Override
	public Flux<B> zrangebyscore(A k, Range<? extends Number> range, Limit limit) {
		return Flux.from(this.connection(o -> o.zrangebyscore(k, range, limit)));
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1) {
		return Mono.from(this.connection(o -> o.zrangebyscore(vsc, k, d, d1)));
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zrangebyscore(vsc, k, string, string1)));
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range) {
		return Mono.from(this.connection(o -> o.zrangebyscore(vsc, k, range)));
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrangebyscore(vsc, k, d, d1, l, l1)));
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrangebyscore(vsc, k, string, string1, l, l1)));
	}

	@Override
	public Mono<Long> zrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrangebyscore(vsc, k, range, limit)));
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, double d, double d1) {
		return Flux.from(this.connection(o -> o.zrangebyscoreWithScores(k, d, d1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, String string, String string1) {
		return Flux.from(this.connection(o -> o.zrangebyscoreWithScores(k, string, string1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, Range<? extends Number> range) {
		return Flux.from(this.connection(o -> o.zrangebyscoreWithScores(k, range)));
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, double d, double d1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrangebyscoreWithScores(k, d, d1, l, l1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, String string, String string1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrangebyscoreWithScores(k, string, string1, l, l1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrangebyscoreWithScores(A k, Range<? extends Number> range, Limit limit) {
		return Flux.from(this.connection(o -> o.zrangebyscoreWithScores(k, range, limit)));
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1) {
		return Mono.from(this.connection(o -> o.zrangebyscoreWithScores(svsc, k, d, d1)));
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zrangebyscoreWithScores(svsc, k, string, string1)));
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range) {
		return Mono.from(this.connection(o -> o.zrangebyscoreWithScores(svsc, k, range)));
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrangebyscoreWithScores(svsc, k, d, d1, l, l1)));
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrangebyscoreWithScores(svsc, k, string, string1, l, l1)));
	}

	@Override
	public Mono<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrangebyscoreWithScores(svsc, k, range, limit)));
	}

	@Override
	public Mono<Long> zrangestorebylex(A k, A k1, Range<? extends B> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrangestorebylex(k, k1, range, limit)));
	}

	@Override
	public Mono<Long> zrangestorebyscore(A k, A k1, Range<? extends Number> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrangestorebyscore(k, k1, range, limit)));
	}

	@Override
	public Mono<Long> zrank(A k, B v) {
		return Mono.from(this.connection(o -> o.zrank(k, v)));
	}

	@Override
	public Mono<Long> zrem(A k, B... vs) {
		return Mono.from(this.connection(o -> o.zrem(k, vs)));
	}

	@Override
	public Mono<Long> zremrangebylex(A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zremrangebylex(k, string, string1)));
	}

	@Override
	public Mono<Long> zremrangebylex(A k, Range<? extends B> range) {
		return Mono.from(this.connection(o -> o.zremrangebylex(k, range)));
	}

	@Override
	public Mono<Long> zremrangebyrank(A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.zremrangebyrank(k, l, l1)));
	}

	@Override
	public Mono<Long> zremrangebyscore(A k, double d, double d1) {
		return Mono.from(this.connection(o -> o.zremrangebyscore(k, d, d1)));
	}

	@Override
	public Mono<Long> zremrangebyscore(A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zremrangebyscore(k, string, string1)));
	}

	@Override
	public Mono<Long> zremrangebyscore(A k, Range<? extends Number> range) {
		return Mono.from(this.connection(o -> o.zremrangebyscore(k, range)));
	}

	@Override
	public Flux<B> zrevrange(A k, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrevrange(k, l, l1)));
	}

	@Override
	public Mono<Long> zrevrange(ValueStreamingChannel<B> vsc, A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrevrange(vsc, k, l, l1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangeWithScores(A k, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrevrangeWithScores(k, l, l1)));
	}

	@Override
	public Mono<Long> zrevrangeWithScores(ScoredValueStreamingChannel<B> svsc, A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrevrangeWithScores(svsc, k, l, l1)));
	}

	@Override
	public Flux<B> zrevrangebylex(A k, Range<? extends B> range) {
		return Flux.from(this.connection(o -> o.zrevrangebylex(k, range)));
	}

	@Override
	public Flux<B> zrevrangebylex(A k, Range<? extends B> range, Limit limit) {
		return Flux.from(this.connection(o -> o.zrevrangebylex(k, range, limit)));
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, double d, double d1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscore(k, d, d1)));
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, String string, String string1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscore(k, string, string1)));
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, Range<? extends Number> range) {
		return Flux.from(this.connection(o -> o.zrevrangebyscore(k, range)));
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, double d, double d1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscore(k, d, d1, l, l1)));
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, String string, String string1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscore(k, string, string1, l, l1)));
	}

	@Override
	public Flux<B> zrevrangebyscore(A k, Range<? extends Number> range, Limit limit) {
		return Flux.from(this.connection(o -> o.zrevrangebyscore(k, range, limit)));
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscore(vsc, k, d, d1)));
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscore(vsc, k, string, string1)));
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range) {
		return Mono.from(this.connection(o -> o.zrevrangebyscore(vsc, k, range)));
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, double d, double d1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscore(vsc, k, d, d1, l, l1)));
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, String string, String string1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscore(vsc, k, string, string1, l, l1)));
	}

	@Override
	public Mono<Long> zrevrangebyscore(ValueStreamingChannel<B> vsc, A k, Range<? extends Number> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrevrangebyscore(vsc, k, range, limit)));
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, double d, double d1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscoreWithScores(k, d, d1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, String string, String string1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscoreWithScores(k, string, string1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, Range<? extends Number> range) {
		return Flux.from(this.connection(o -> o.zrevrangebyscoreWithScores(k, range)));
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, double d, double d1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscoreWithScores(k, d, d1, l, l1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, String string, String string1, long l, long l1) {
		return Flux.from(this.connection(o -> o.zrevrangebyscoreWithScores(k, string, string1, l, l1)));
	}

	@Override
	public Flux<ScoredValue<B>> zrevrangebyscoreWithScores(A k, Range<? extends Number> range, Limit limit) {
		return Flux.from(this.connection(o -> o.zrevrangebyscoreWithScores(k, range, limit)));
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscoreWithScores(svsc, k, d, d1)));
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscoreWithScores(svsc, k, string, string1)));
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range) {
		return Mono.from(this.connection(o -> o.zrevrangebyscoreWithScores(svsc, k, range)));
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, double d, double d1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscoreWithScores(svsc, k, d, d1, l, l1)));
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, String string, String string1, long l, long l1) {
		return Mono.from(this.connection(o -> o.zrevrangebyscoreWithScores(svsc, k, string, string1, l, l1)));
	}

	@Override
	public Mono<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<B> svsc, A k, Range<? extends Number> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrevrangebyscoreWithScores(svsc, k, range, limit)));
	}

	@Override
	public Mono<Long> zrevrangestorebylex(A k, A k1, Range<? extends B> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrevrangestorebylex(k, k1, range, limit)));
	}

	@Override
	public Mono<Long> zrevrangestorebyscore(A k, A k1, Range<? extends Number> range, Limit limit) {
		return Mono.from(this.connection(o -> o.zrevrangestorebyscore(k, k1, range, limit)));
	}

	@Override
	public Mono<Long> zrevrank(A k, B v) {
		return Mono.from(this.connection(o -> o.zrevrank(k, v)));
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k) {
		return Mono.from(this.connection(o -> o.zscan(k)));
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.zscan(k, sa)));
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k, ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.zscan(k, sc, sa)));
	}

	@Override
	public Mono<ScoredValueScanCursor<B>> zscan(A k, ScanCursor sc) {
		return Mono.from(this.connection(o -> o.zscan(k, sc)));
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k) {
		return Mono.from(this.connection(o -> o.zscan(svsc, k)));
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.zscan(svsc, k, sa)));
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k, ScanCursor sc, ScanArgs sa) {
		return Mono.from(this.connection(o -> o.zscan(svsc, k, sc, sa)));
	}

	@Override
	public Mono<StreamScanCursor> zscan(ScoredValueStreamingChannel<B> svsc, A k, ScanCursor sc) {
		return Mono.from(this.connection(o -> o.zscan(svsc, k, sc)));
	}

	@Override
	public Mono<Double> zscore(A k, B v) {
		return Mono.from(this.connection(o -> o.zscore(k, v)));
	}

	@Override
	public Flux<B> zunion(A... ks) {
		return Flux.from(this.connection(o -> o.zunion(ks)));
	}

	@Override
	public Flux<B> zunion(ZAggregateArgs zaa, A... ks) {
		return Flux.from(this.connection(o -> o.zunion(zaa, ks)));
	}

	@Override
	public Flux<ScoredValue<B>> zunionWithScores(ZAggregateArgs zaa, A... ks) {
		return Flux.from(this.connection(o -> o.zunionWithScores(zaa, ks)));
	}

	@Override
	public Flux<ScoredValue<B>> zunionWithScores(A... ks) {
		return Flux.from(this.connection(o -> o.zunionWithScores(ks)));
	}

	@Override
	public Mono<Long> zunionstore(A k, A... ks) {
		return Mono.from(this.connection(o -> o.zunionstore(k, ks)));
	}

	@Override
	public Mono<Long> zunionstore(A k, ZStoreArgs zsa, A... ks) {
		return Mono.from(this.connection(o -> o.zunionstore(k, zsa, ks)));
	}

	@Override
	public Mono<Long> xack(A k, A k1, String... strings) {
		return Mono.from(this.connection(o -> o.xack(k, k1, strings)));
	}

	@Override
	public Mono<String> xadd(A k, Map<A, B> map) {
		return Mono.from(this.connection(o -> o.xadd(k, map)));
	}

	@Override
	public Mono<String> xadd(A k, XAddArgs xaa, Map<A, B> map) {
		return Mono.from(this.connection(o -> o.xadd(k, xaa, map)));
	}

	@Override
	public Mono<String> xadd(A k, Object... os) {
		return Mono.from(this.connection(o -> o.xadd(k, os)));
	}

	@Override
	public Mono<String> xadd(A k, XAddArgs xaa, Object... os) {
		return Mono.from(this.connection(o -> o.xadd(k, xaa, os)));
	}

	@Override
	public Mono<ClaimedMessages<A, B>> xautoclaim(A k, XAutoClaimArgs<A> xaca) {
		return Mono.from(this.connection(o -> o.xautoclaim(k, xaca)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A k, Consumer<A> cnsmr, long l, String... strings) {
		return Flux.from(this.connection(o -> o.xclaim(k, cnsmr, l, strings)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xclaim(A k, Consumer<A> cnsmr, XClaimArgs xca, String... strings) {
		return Flux.from(this.connection(o -> o.xclaim(k, cnsmr, xca, strings)));
	}

	@Override
	public Mono<Long> xdel(A k, String... strings) {
		return Mono.from(this.connection(o -> o.xdel(k, strings)));
	}

	@Override
	public Mono<String> xgroupCreate(XReadArgs.StreamOffset<A> so, A k) {
		return Mono.from(this.connection(o -> o.xgroupCreate(so, k)));
	}

	@Override
	public Mono<String> xgroupCreate(XReadArgs.StreamOffset<A> so, A k, XGroupCreateArgs xgca) {
		return Mono.from(this.connection(o -> o.xgroupCreate(so, k, xgca)));
	}

	@Override
	public Mono<Boolean> xgroupCreateconsumer(A k, Consumer<A> cnsmr) {
		return Mono.from(this.connection(o -> o.xgroupCreateconsumer(k, cnsmr)));
	}

	@Override
	public Mono<Long> xgroupDelconsumer(A k, Consumer<A> cnsmr) {
		return Mono.from(this.connection(o -> o.xgroupDelconsumer(k, cnsmr)));
	}

	@Override
	public Mono<Boolean> xgroupDestroy(A k, A k1) {
		return Mono.from(this.connection(o -> o.xgroupDestroy(k, k1)));
	}

	@Override
	public Mono<String> xgroupSetid(XReadArgs.StreamOffset<A> so, A k) {
		return Mono.from(this.connection(o -> o.xgroupSetid(so, k)));
	}

	@Override
	public Flux<Object> xinfoStream(A k) {
		return Flux.from(this.connection(o -> o.xinfoStream(k)));
	}

	@Override
	public Flux<Object> xinfoGroups(A k) {
		return Flux.from(this.connection(o -> o.xinfoGroups(k)));
	}

	@Override
	public Flux<Object> xinfoConsumers(A k, A k1) {
		return Flux.from(this.connection(o -> o.xinfoConsumers(k, k1)));
	}

	@Override
	public Mono<Long> xlen(A k) {
		return Mono.from(this.connection(o -> o.xlen(k)));
	}

	@Override
	public Mono<PendingMessages> xpending(A k, A k1) {
		return Mono.from(this.connection(o -> o.xpending(k, k1)));
	}

	@Override
	public Flux<PendingMessage> xpending(A k, A k1, Range<String> range, Limit limit) {
		return Flux.from(this.connection(o -> o.xpending(k, k1, range, limit)));
	}

	@Override
	public Flux<PendingMessage> xpending(A k, Consumer<A> cnsmr, Range<String> range, Limit limit) {
		return Flux.from(this.connection(o -> o.xpending(k, cnsmr, range, limit)));
	}

	@Override
	public Flux<PendingMessage> xpending(A k, XPendingArgs<A> xpa) {
		return Flux.from(this.connection(o -> o.xpending(k, xpa)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A k, Range<String> range) {
		return Flux.from(this.connection(o -> o.xrange(k, range)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xrange(A k, Range<String> range, Limit limit) {
		return Flux.from(this.connection(o -> o.xrange(k, range, limit)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xread(XReadArgs.StreamOffset<A>... sos) {
		return Flux.from(this.connection(o -> o.xread(sos)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xread(XReadArgs xra, XReadArgs.StreamOffset<A>... sos) {
		return Flux.from(this.connection(o -> o.xread(xra, sos)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xreadgroup(Consumer<A> cnsmr, XReadArgs.StreamOffset<A>... sos) {
		return Flux.from(this.connection(o -> o.xreadgroup(cnsmr, sos)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xreadgroup(Consumer<A> cnsmr, XReadArgs xra, XReadArgs.StreamOffset<A>... sos) {
		return Flux.from(this.connection(o -> o.xreadgroup(cnsmr, xra, sos)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A k, Range<String> range) {
		return Flux.from(this.connection(o -> o.xrevrange(k, range)));
	}

	@Override
	public Flux<StreamMessage<A, B>> xrevrange(A k, Range<String> range, Limit limit) {
		return Flux.from(this.connection(o -> o.xrevrange(k, range, limit)));
	}

	@Override
	public Mono<Long> xtrim(A k, long l) {
		return Mono.from(this.connection(o -> o.xtrim(k, l)));
	}

	@Override
	public Mono<Long> xtrim(A k, boolean bln, long l) {
		return Mono.from(this.connection(o -> o.xtrim(k, bln, l)));
	}

	@Override
	public Mono<Long> xtrim(A k, XTrimArgs xta) {
		return Mono.from(this.connection(o -> o.xtrim(k, xta)));
	}

	@Override
	public Mono<Long> append(A k, B v) {
		return Mono.from(this.connection(o -> o.append(k, v)));
	}

	@Override
	public Mono<Long> bitcount(A k) {
		return Mono.from(this.connection(o -> o.bitcount(k)));
	}

	@Override
	public Mono<Long> bitcount(A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.bitcount(k, l, l1)));
	}

	@Override
	public Flux<Value<Long>> bitfield(A k, BitFieldArgs bfa) {
		return Flux.from(this.connection(o -> o.bitfield(k, bfa)));
	}

	@Override
	public Mono<Long> bitpos(A k, boolean bln) {
		return Mono.from(this.connection(o -> o.bitpos(k, bln)));
	}

	@Override
	public Mono<Long> bitpos(A k, boolean bln, long l) {
		return Mono.from(this.connection(o -> o.bitpos(k, bln, l)));
	}

	@Override
	public Mono<Long> bitpos(A k, boolean bln, long l, long l1) {
		return Mono.from(this.connection(o -> o.bitpos(k, bln, l, l1)));
	}

	@Override
	public Mono<Long> bitopAnd(A k, A... ks) {
		return Mono.from(this.connection(o -> o.bitopAnd(k, ks)));
	}

	@Override
	public Mono<Long> bitopNot(A k, A k1) {
		return Mono.from(this.connection(o -> o.bitopNot(k, k1)));
	}

	@Override
	public Mono<Long> bitopOr(A k, A... ks) {
		return Mono.from(this.connection(o -> o.bitopOr(k, ks)));
	}

	@Override
	public Mono<Long> bitopXor(A k, A... ks) {
		return Mono.from(this.connection(o -> o.bitopXor(k, ks)));
	}

	@Override
	public Mono<Long> decr(A k) {
		return Mono.from(this.connection(o -> o.decr(k)));
	}

	@Override
	public Mono<Long> decrby(A k, long l) {
		return Mono.from(this.connection(o -> o.decrby(k, l)));
	}

	@Override
	public Mono<B> get(A k) {
		return Mono.from(this.connection(o -> o.get(k)));
	}

	@Override
	public Mono<Long> getbit(A k, long l) {
		return Mono.from(this.connection(o -> o.getbit(k, l)));
	}

	@Override
	public Mono<B> getdel(A k) {
		return Mono.from(this.connection(o -> o.getdel(k)));
	}

	@Override
	public Mono<B> getex(A k, GetExArgs gea) {
		return Mono.from(this.connection(o -> o.getex(k, gea)));
	}

	@Override
	public Mono<B> getrange(A k, long l, long l1) {
		return Mono.from(this.connection(o -> o.getrange(k, l, l1)));
	}

	@Override
	public Mono<B> getset(A k, B v) {
		return Mono.from(this.connection(o -> o.getset(k, v)));
	}

	@Override
	public Mono<Long> incr(A k) {
		return Mono.from(this.connection(o -> o.incr(k)));
	}

	@Override
	public Mono<Long> incrby(A k, long l) {
		return Mono.from(this.connection(o -> o.incrby(k, l)));
	}

	@Override
	public Mono<Double> incrbyfloat(A k, double d) {
		return Mono.from(this.connection(o -> o.incrbyfloat(k, d)));
	}

	@Override
	public Flux<KeyValue<A, B>> mget(A... ks) {
		return Flux.from(this.connection(o -> o.mget(ks)));
	}

	@Override
	public Mono<Long> mget(KeyValueStreamingChannel<A, B> kvsc, A... ks) {
		return Mono.from(this.connection(o -> o.mget(kvsc, ks)));
	}

	@Override
	public Mono<String> mset(Map<A, B> map) {
		return Mono.from(this.connection(o -> o.mset(map)));
	}

	@Override
	public Mono<Boolean> msetnx(Map<A, B> map) {
		return Mono.from(this.connection(o -> o.msetnx(map)));
	}

	@Override
	public Mono<String> set(A k, B v) {
		return Mono.from(this.connection(o -> o.set(k, v)));
	}

	@Override
	public Mono<String> set(A k, B v, SetArgs sa) {
		return Mono.from(this.connection(o -> o.set(k, v, sa)));
	}

	@Override
	public Mono<B> setGet(A k, B v) {
		return Mono.from(this.connection(o -> o.setGet(k, v)));
	}

	@Override
	public Mono<B> setGet(A k, B v, SetArgs sa) {
		return Mono.from(this.connection(o -> o.setGet(k, v, sa)));
	}

	@Override
	public Mono<Long> setbit(A k, long l, int i) {
		return Mono.from(this.connection(o -> o.setbit(k, l, i)));
	}

	@Override
	public Mono<String> setex(A k, long l, B v) {
		return Mono.from(this.connection(o -> o.setex(k, l, v)));
	}

	@Override
	public Mono<String> psetex(A k, long l, B v) {
		return Mono.from(this.connection(o -> o.psetex(k, l, v)));
	}

	@Override
	public Mono<Boolean> setnx(A k, B v) {
		return Mono.from(this.connection(o -> o.setnx(k, v)));
	}

	@Override
	public Mono<Long> setrange(A k, long l, B v) {
		return Mono.from(this.connection(o -> o.setrange(k, l, v)));
	}

	@Override
	public Mono<StringMatchResult> stralgoLcs(StrAlgoArgs saa) {
		return Mono.from(this.connection(o -> o.stralgoLcs(saa)));
	}

	@Override
	public Mono<Long> strlen(A k) {
		return Mono.from(this.connection(o -> o.strlen(k)));
	}
}

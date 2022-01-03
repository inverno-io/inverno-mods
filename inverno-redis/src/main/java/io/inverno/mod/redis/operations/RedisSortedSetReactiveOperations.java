/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.operations;

import io.inverno.mod.redis.util.AbstractScanBuilder;
import io.inverno.mod.redis.util.AbstractScanResult;
import io.inverno.mod.redis.util.Bound;
import io.inverno.mod.redis.util.EntryOptional;
import io.inverno.mod.redis.util.Keys;
import io.inverno.mod.redis.util.Values;
import java.util.List;
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
public interface RedisSortedSetReactiveOperations<A, B> /*extends RedisSortedSetReactiveCommands<A, B>*/ {

	/**
	 * <a href="https://redis.io/commands/bzmpop">BZMPOP</a> timeout numkeys key [key ...] MIN|MAX [COUNT count]
	 * 
	 * @return 
	 */
	SortedSetBzmpopBuilder<A, B> bzmpop();
	
	/**
	 * <a href="https://redis.io/commands/bzpopmax">BZPOPMAX</a> key timeout 
	 * 
	 * @param timeout
	 * @param key
	 * @return 
	 */
	Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmax(double timeout, A key);
	
	/**
	 * <a href="https://redis.io/commands/bzpopmax">BZPOPMAX</a> key [key ...] timeout 
	 * 
	 * @param timeout
	 * @param keys
	 * @return 
	 */
	Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmax(double timeout, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/bzpopmin">BZPOPMIN</a> key timeout 
	 * 
	 * @param timeout
	 * @param key
	 * @return 
	 */
	Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmin(double timeout, A key);
	
	/**
	 * <a href="https://redis.io/commands/bzpopmin">BZPOPMIN</a> key [key ...] timeout 
	 * 
	 * @param timeout
	 * @param keys
	 * @return 
	 */
	Mono<EntryOptional<A, SortedSetScoredMember<B>>> bzpopmin(double timeout, Consumer<Keys<A>> keys);

	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] [INCR] score member [score member ...] 
	 * 
	 * @param key
	 * @param score
	 * @param member
	 * @return 
	 */
	Mono<Long> zadd(A key, double score, B member);
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] [INCR] score member [score member ...] 
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Mono<Long> zadd(A key, Consumer<SortedSetScoredMembers<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] [INCR] score member [score member ...]
	 * 
	 * @return 
	 */
	SortedSetZaddBuilder<A, B> zadd();
	
	/**
	 * <a href="https://redis.io/commands/zcard">ZCARD</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> zcard(A key);
	
	/**
	 * <a href="https://redis.io/commands/zcount">ZCOUNT</a> key min max
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return 
	 */
	Mono<Long> zcount(A key, Bound<? extends Number> min, Bound<? extends Number> max);
	
	/**
	 * <a href="https://redis.io/commands/zdiff">ZDIFF</a> 1 key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> zdiff(A key);
	
	/**
	 * <a href="https://redis.io/commands/zdiff">ZDIFF</a> numkeys key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<B> zdiff(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zdiff">ZDIFF</a> 1 key [WITHSCORES]
	 * 
	 * @param key
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zdiffWithScores(A key);

	/**
	 * <a href="https://redis.io/commands/zdiff">ZDIFF</a> numkeys key [key ...] [WITHSCORES]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zdiffWithScores(Consumer<Keys<A>> keys);

	
	/**
	 * <a href="https://redis.io/commands/zdiffstore">ZDIFFSTORE</a> destination 1 key
	 * 
	 * @param destination
	 * @param key
	 * @return 
	 */
	Mono<Long> zdiffstore(A destination, A key);
	
	/**
	 * <a href="https://redis.io/commands/zdiffstore">ZDIFFSTORE</a> destination numkeys key [key ...]
	 * 
	 * @param destination
	 * @param keys
	 * @return 
	 */
	Mono<Long> zdiffstore(A destination, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zincrby">ZINCRBY</a> key increment member 
	 * 
	 * @param key
	 * @param increment
	 * @param member
	 * @return 
	 */
	Mono<Double> zincrby(A key, double increment, B member);
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> zinter(A key);
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<B> zinter(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @return 
	 */
	SortedSetZinterBuilder<A, B> zinter();
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key WITHSCORES
	 * 
	 * @param key
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zinterWithScores(A key);
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] WITHSCORES
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zinterWithScores(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] WITHSCORES
	 * 
	 * @return 
	 */
	SortedSetZinterWithScoresBuilder<A, B> zinterWithScores();

	/**
	 * <a href="https://redis.io/commands/zintercard">ZINTERCARD</a> numkeys key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> zintercard(A key);
	
	/**
	 * <a href="https://redis.io/commands/zintercard">ZINTERCARD</a> numkeys key [LIMIT limit] 
	 * 
	 * @param key
	 * @param limit
	 * @return 
	 */
	Mono<Long> zintercard(A key, long limit);
	
	/**
	 * <a href="https://redis.io/commands/zintercard">ZINTERCARD</a> numkeys key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> zintercard(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zintercard">ZINTERCARD</a> numkeys key [key ...] [LIMIT limit] 
	 * 
	 * @param keys
	 * @param limit
	 * @return 
	 */
	Mono<Long> zintercard(Consumer<Keys<A>> keys, long limit);
	
	/**
	 * <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a> destination numkeys key
	 * 
	 * @param destination
	 * @param key
	 * @return 
	 */
	Mono<Long> zinterstore(A destination, A key);
	
	/**
	 * <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a> destination numkeys key [key ...]
	 * 
	 * @param destination
	 * @param keys
	 * @return 
	 */
	Mono<Long> zinterstore(A destination, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @return 
	 */
	SortedSetZinterstoreBuilder<A, B> zinterstore();
	
	/**
	 * <a href="https://redis.io/commands/zlexcount">ZLEXCOUNT</a> key min max
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return 
	 */
	Mono<Long> zlexcount(A key, Bound<? extends Number> min, Bound<? extends Number> max);
	
	/**
	 * <a href="https://redis.io/commands/zmpop">ZMPOP</a> numkeys key [key ...] MIN|MAX [COUNT count]
	 * 
	 * @return 
	 */
	SortedSetZmpopBuilder<A, B> zmpop();
	
	/**
	 * <a href="https://redis.io/commands/zmscore">ZMSCORE</a> key member
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Flux<Optional<Double>> zmscore(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/zmscore">ZMSCORE</a> key member [member ...] 
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Flux<Optional<Double>> zmscore(A key, Consumer<Keys<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/zpopmax">ZPOPMAX</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<SortedSetScoredMember<B>> zpopmax(A key);
	
	/**
	 * <a href="https://redis.io/commands/zpopmax">ZPOPMAX</a> key [count] 
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zpopmax(A key, long count);
	
	/**
	 * <a href="https://redis.io/commands/zpopmin">ZPOPMIN</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<SortedSetScoredMember<B>> zpopmin(A key);
	
	/**
	 * <a href="https://redis.io/commands/zpopmin">ZPOPMIN</a> key [count] 
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zpopmin(A key, long count);

	/**
	 * <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> zrandmember(A key);
	
	/**
	 * <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a> key [count]
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<B> zrandmember(A key, long count);
	
	/**
	 * <a href="https://redis.io/commands/zrandmember">ZRANDMEMBER</a> key [count [WITHSCORES]]
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zrandmemberWithScores(A key, long count);
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count]
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return 
	 */
	Flux<B> zrange(A key, long min, long max);
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] 
	 * 
	 * @return 
	 */
	SortedSetZrangeBuilder<A, B, Long> zrange();
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zrangeWithScores(A key, long min, long max);

	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
	 * 
	 * @return 
	 */
	SortedSetZrangeWithScoresBuilder<A, B, Long> zrangeWithScores();

	/**
	 * <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a> dst src min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] 
	 * 
	 * @param destination
	 * @param source
	 * @param min
	 * @param max
	 * @return 
	 */
	Mono<Long> zrangestore(A destination, A source, long min, long max);
	
	/**
	 * <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a> dst src min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] 
	 * 
	 * @return 
	 */
	SortedSetZrangestoreBuilder<A, B, Long> zrangestore();

	/**
	 * <a href="https://redis.io/commands/zrank">ZRANK</a> key member 
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Long> zrank(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/zrem">ZREM</a> key member
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Long> zrem(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/zrem">ZREM</a> key member [member ...] 
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Mono<Long> zrem(A key, Consumer<Values<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/zremrangebylex">ZREMRANGEBYLEX</a> key min max 
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return 
	 */
	Mono<Long> zremrangebylex(A key, Bound<? extends B> min, Bound<? extends B> max);
	
	/**
	 * <a href="https://redis.io/commands/zremrangebyrank">ZREMRANGEBYRANK</a> key start stop
	 * 
	 * @param key
	 * @param start
	 * @param stop
	 * @return 
	 */
	Mono<Long> zremrangebyrank(A key, long start, long stop);
	
	/**
	 * <a href="https://redis.io/commands/zremrangebyscore">ZREMRANGEBYSCORE</a> key min max
	 * 
	 * @param key
	 * @param min
	 * @param max
	 * @return 
	 */
	Mono<Long> zremrangebyscore(A key, Bound<? extends Number> min, Bound<? extends Number> max);


	/**
	 * <a href="https://redis.io/commands/zrevrank">ZREVRANK</a> key member 
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Long> zrevrank(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/zscan">ZSCAN</a> key cursor
	 * 
	 * @param key
	 * @param cursor
	 * @return 
	 */
	Mono<SortedSetScanResult<B>> zscan(A key, String cursor);

	/**
	 * <a href="https://redis.io/commands/zscan">ZSCAN</a> key cursor [MATCH pattern] [COUNT count]
	 * 
	 * @return 
	 */
	SortedSetScanBuilder<A, B> zscan();
	
	/**
	 * <a href="https://redis.io/commands/zscan">ZSCORE</a> key member 
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Double> zscore(A key, B member);
	
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> zunion(A key);
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<B> zunion(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @return 
	 */
	SortedSetZunionBuilder<A, B> zunion();
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key WITHSCORES
	 * 
	 * @param key
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zunionWithScores(A key);
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] WITHSCORES
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<SortedSetScoredMember<B>> zunionWithScores(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] WITHSCORES
	 * 
	 * @return 
	 */
	SortedSetZunionWithScoresBuilder<A, B> zunionWithScores();
	
	/**
	 * <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a> destination numkeys key
	 * 
	 * @param destination
	 * @param key
	 * @return 
	 */
	Mono<Long> zunionstore(A destination, A key);
	
	/**
	 * <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a> destination numkeys key [key ...]
	 * 
	 * @param destination
	 * @param keys
	 * @return 
	 */
	Mono<Long> zunionstore(A destination, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @return 
	 */
	SortedSetZunionstoreBuilder<A, B> zunionstore();
	
	/**
	 * <a href="https://redis.io/commands/bzmpop">BZMPOP</a> timeout numkeys key [key ...] MIN|MAX [COUNT count]
	 * <a href="https://redis.io/commands/zmpop">ZMPOP</a> numkeys key [key ...] MIN|MAX [COUNT count] 
	 * 
	 * @param <A>
	 * @param <B> 
	 * @param <C> 
	 */
	interface AbstractSortedSetZmpopBuilder<A, B, C extends AbstractSortedSetZmpopBuilder<A, B, C>> {
		C min();
		C max();
		C count(long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/bzmpop">BZMPOP</a> timeout numkeys key [key ...] MIN|MAX [COUNT count]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface SortedSetBzmpopBuilder<A, B> extends AbstractSortedSetZmpopBuilder<A, B, SortedSetBzmpopBuilder<A, B>> {
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(double timeout, A key);
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(double timeout, Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/bzmpop">BZMPOP</a> timeout numkeys key [key ...] MIN|MAX [COUNT count]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface SortedSetZmpopBuilder<A, B> extends AbstractSortedSetZmpopBuilder<A, B, SortedSetZmpopBuilder<A, B>> {
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(A key);
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] [INCR] score member [score member ...]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface SortedSetZaddBuilder<A, B> {
		
		SortedSetZaddBuilder<A, B> nx();
		SortedSetZaddBuilder<A, B> xx();
		SortedSetZaddBuilder<A, B> ch();
		SortedSetZaddBuilder<A, B> incr();
		
		Mono<Long> build(A key, double score, B member);
		Mono<Long> build(A key, Consumer<SortedSetScoredMembers<B>> members);
	}
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES]
	 * <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @param <A>
	 * @param <B>
	 * @param <C> 
	 */
	interface AbstractSortedSetZinterBuilder<A, B, C extends AbstractSortedSetZinterBuilder<A, B, C>> {
		
		C weight(double weight);
		
		C sum();
		C min();
		C max();
	}
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface SortedSetZinterBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZinterBuilder<A, B>> {
		
		Flux<B> build(A key);
		Flux<B> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES]
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface SortedSetZinterWithScoresBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZinterWithScoresBuilder<A, B>> {
		
		Flux<SortedSetScoredMember<B>> build(A key);
		Flux<SortedSetScoredMember<B>> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface SortedSetZinterstoreBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZinterBuilder<A, B>> {
		
		Mono<Long> build(A destination, A key);
		Mono<Long> build(A destination, Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
	 * <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a> dst src min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] 
	 * 
	 * @param <A>
	 * @param <B>
	 * @param <C> 
	 */
	interface AbstractSortedSetZrangeBuilder<A, B, C extends AbstractSortedSetZrangeBuilder<A, B, C>> {

		C reverse();
		C limit(long offset, long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
	 * 
	 * @param <A>
	 * @param <B> 
	 * @param <C> 
	 */
	interface SortedSetZrangeBuilder<A, B, C> extends AbstractSortedSetZrangeBuilder<A, B, SortedSetZrangeBuilder<A, B, C>> {
		
		SortedSetZrangeBuilder<A, B, Bound<? extends Number>> byScore();
		SortedSetZrangeBuilder<A, B, Bound<? extends B>> byLex();
		
		Flux<B> build(A key, C min, C max);
	}
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
	 * 
	 * @param <A>
	 * @param <B> 
	 * @param <C> 
	 */
	interface SortedSetZrangeWithScoresBuilder<A, B, C> extends AbstractSortedSetZrangeBuilder<A, B, SortedSetZrangeWithScoresBuilder<A, B, C>> {
		
		SortedSetZrangeWithScoresBuilder<A, B, Bound<? extends Number>> byScore();
		SortedSetZrangeWithScoresBuilder<A, B, Bound<? extends B>> byLex();
		
		Flux<SortedSetScoredMember<B>> build(A key, C min, C max);
	}
	
	/**
	 * <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a> dst src min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] 
	 * 
	 * @param <A>
	 * @param <B> 
	 * @param <C> 
	 */
	interface SortedSetZrangestoreBuilder<A, B, C> extends AbstractSortedSetZrangeBuilder<A, B, SortedSetZrangestoreBuilder<A, B, C>> {
		
		SortedSetZrangestoreBuilder<A, B, Bound<? extends Number>> byScore();
		SortedSetZrangestoreBuilder<A, B, Bound<? extends B>> byLex();
		
		Mono<Long> build(A destination, A source, C min, C max);
	}
	
	/**
	 * <a href="https://redis.io/commands/zscan">ZSCAN</a> key cursor [MATCH pattern] [COUNT count]
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface SortedSetScanBuilder<A, B> extends AbstractScanBuilder<SortedSetScanBuilder<A, B>> {
		Mono<SortedSetScanResult<B>> build(A key, String cursor);
	}

	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES] 
	 * <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] 
	 *
	 * @param <A> 
	 * @param <B> 
	 * @param <C> 
	 */
	interface AbstractSortedSetZunionBuilder<A, B, C extends AbstractSortedSetZunionBuilder<A, B, C>> {
		
		C weight(double weight);
		
		C sum();
		C min();
		C max();
	}
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES] 
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface SortedSetZunionBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZunionBuilder<A, B>> {
		
		Flux<B> build(A key);
		Flux<B> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES] 
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface SortedSetZunionWithScoresBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZunionWithScoresBuilder<A, B>> {
		
		Flux<SortedSetScoredMember<B>> build(A key);
		Flux<SortedSetScoredMember<B>> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] 
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface SortedSetZunionstoreBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZunionstoreBuilder<A, B>> {
		
		Mono<Long> build(A destination, A key);
		Mono<Long> build(A destination, Consumer<Keys<A>> keys);
	}
	
	
	interface SortedSetScanResult<B> extends AbstractScanResult {
		List<SortedSetScoredMember<B>> getMembers();
	}
	
	interface SortedSetScoredMember<B> {
		
		B getValue();
		
		double getScore();
	}
	
	interface SortedSetScoredMembers<B> {
		SortedSetScoredMembers<B> entry(double score, B value);
	}
}


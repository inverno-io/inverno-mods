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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Sorted Sets reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisSortedSetReactiveOperations<A, B> {

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
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] score member
	 * 
	 * @param key
	 * @param score
	 * @param member
	 * @return 
	 */
	Mono<Long> zadd(A key, double score, B member);
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] score member [score member ...] 
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Mono<Long> zadd(A key, Consumer<SortedSetScoredMembers<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] score member [score member ...]
	 * 
	 * @return 
	 */
	SortedSetZaddBuilder<A, B> zadd();
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] INCR score member
	 * 
	 * @param key
	 * @param score
	 * @param member
	 * @return 
	 */
	Mono<Double> zaddIncr(A key, double score, B member);
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] INCR score member [score member ...]
	 * 
	 * @return 
	 */
	SortedSetZaddIncrBuilder<A, B> zaddIncr();
	
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
	Mono<Long> zlexcount(A key, Bound<B> min, Bound<B> max);
	
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
	Flux<Optional<Double>> zmscore(A key, Consumer<Values<B>> members);
	
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
	 * <ul>
	 * <li><a href="https://redis.io/commands/bzmpop">BZMPOP</a> timeout numkeys key [key ...] MIN|MAX [COUNT count]</li>
	 * <li><a href="https://redis.io/commands/zmpop">ZMPOP</a> numkeys key [key ...] MIN|MAX [COUNT count]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractSortedSetZmpopBuilder<A, B, C extends AbstractSortedSetZmpopBuilder<A, B, C>> {
		
		/**
		 * 
		 * @return 
		 */
		C min();
		
		/**
		 * 
		 * @return 
		 */
		C max();
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		C count(long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/bzmpop">BZMPOP</a> timeout numkeys key [key ...] MIN|MAX [COUNT count]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetBzmpopBuilder<A, B> extends AbstractSortedSetZmpopBuilder<A, B, SortedSetBzmpopBuilder<A, B>> {
		
		/**
		 * 
		 * @param timeout
		 * @param key
		 * @return 
		 */
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(double timeout, A key);
		
		/**
		 * 
		 * @param timeout
		 * @param keys
		 * @return 
		 */
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(double timeout, Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/bzmpop">BZMPOP</a> timeout numkeys key [key ...] MIN|MAX [COUNT count]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZmpopBuilder<A, B> extends AbstractSortedSetZmpopBuilder<A, B, SortedSetZmpopBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(A key);
		
		/**
		 * 
		 * @param keys
		 * @return 
		 */
		Flux<EntryOptional<A, SortedSetScoredMember<B>>> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] score member [score member ...]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZaddBuilder<A, B> {
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddBuilder<A, B> nx();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddBuilder<A, B> xx();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddBuilder<A, B> gt();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddBuilder<A, B> lt();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddBuilder<A, B> ch();
		
		/**
		 * 
		 * @param key
		 * @param score
		 * @param member
		 * @return 
		 */
		Mono<Long> build(A key, double score, B member);
		
		/**
		 * 
		 * @param key
		 * @param members
		 * @return 
		 */
		Mono<Long> build(A key, Consumer<SortedSetScoredMembers<B>> members);
	}
	
	/**
	 * <a href="https://redis.io/commands/zadd">ZADD</a> key [NX|XX] [GT|LT] [CH] INCR score member
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZaddIncrBuilder<A, B> {
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddIncrBuilder<A, B> nx();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddIncrBuilder<A, B> xx();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddIncrBuilder<A, B> gt();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddIncrBuilder<A, B> lt();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZaddIncrBuilder<A, B> ch();
		
		/**
		 * 
		 * @param key
		 * @param score
		 * @param member
		 * @return 
		 */
		Mono<Double> build(A key, double score, B member);
	}
	
	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES]</li>
	 * <li><a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractSortedSetZinterBuilder<A, B, C extends AbstractSortedSetZinterBuilder<A, B, C>> {
		
		/**
		 * 
		 * @param weight
		 * @return 
		 */
		C weight(double weight);
		
		/**
		 * 
		 * @return 
		 */
		C sum();
		
		/**
		 * 
		 * @return 
		 */
		C min();
		
		/**
		 * 
		 * @return 
		 */
		C max();
	}
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZinterBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZinterBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<B> build(A key);
		
		/**
		 * 
		 * @param keys
		 * @return 
		 */
		Flux<B> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zinter">ZINTER</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZinterWithScoresBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZinterWithScoresBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<SortedSetScoredMember<B>> build(A key);
		
		/**
		 * 
		 * @param keys
		 * @return 
		 */
		Flux<SortedSetScoredMember<B>> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zinterstore">ZINTERSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZinterstoreBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZinterstoreBuilder<A, B>> {
		
		/**
		 * 
		 * @param destination
		 * @param key
		 * @return 
		 */
		Mono<Long> build(A destination, A key);
		
		/**
		 * 
		 * @param destination
		 * @param keys
		 * @return 
		 */
		Mono<Long> build(A destination, Consumer<Keys<A>> keys);
	}
	
	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES]</li>
	 * <li><a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a> dst src min max [BYSCORE|BYLEX] [REV] [LIMIT offset count]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractSortedSetZrangeBuilder<A, B, C extends AbstractSortedSetZrangeBuilder<A, B, C>> {

		/**
		 * 
		 * @return 
		 */
		C reverse();
		
		/**
		 * 
		 * @param offset
		 * @param count
		 * @return 
		 */
		C limit(long offset, long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface SortedSetZrangeBuilder<A, B, C> extends AbstractSortedSetZrangeBuilder<A, B, SortedSetZrangeBuilder<A, B, C>> {
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZrangeBuilder<A, B, ? extends Number> byScore();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZrangeBuilder<A, B, ? extends B> byLex();
		
		/**
		 * 
		 * @param key
		 * @param min
		 * @param max
		 * @return 
		 */
		Flux<B> build(A key, Bound<C> min, Bound<C> max);
	}
	
	/**
	 * <a href="https://redis.io/commands/zrange">ZRANGE</a> key min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] [WITHSCORES] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface SortedSetZrangeWithScoresBuilder<A, B, C> extends AbstractSortedSetZrangeBuilder<A, B, SortedSetZrangeWithScoresBuilder<A, B, C>> {
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZrangeWithScoresBuilder<A, B, ? extends Number> byScore();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZrangeWithScoresBuilder<A, B, ? extends B> byLex();
		
		/**
		 * 
		 * @param key
		 * @param min
		 * @param max
		 * @return 
		 */
		Flux<SortedSetScoredMember<B>> build(A key, Bound<C> min, Bound<C> max);
	}
	
	/**
	 * <a href="https://redis.io/commands/zrangestore">ZRANGESTORE</a> dst src min max [BYSCORE|BYLEX] [REV] [LIMIT offset count] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface SortedSetZrangestoreBuilder<A, B, C> extends AbstractSortedSetZrangeBuilder<A, B, SortedSetZrangestoreBuilder<A, B, C>> {
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZrangestoreBuilder<A, B, ? extends Number> byScore();
		
		/**
		 * 
		 * @return 
		 */
		SortedSetZrangestoreBuilder<A, B, ? extends B> byLex();
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @param min
		 * @param max
		 * @return 
		 */
		Mono<Long> build(A source, A destination, Bound<C> min, Bound<C> max);
	}
	
	/**
	 * <a href="https://redis.io/commands/zscan">ZSCAN</a> key cursor [MATCH pattern] [COUNT count]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetScanBuilder<A, B> extends AbstractScanBuilder<SortedSetScanBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param cursor
		 * @return 
		 */
		Mono<SortedSetScanResult<B>> build(A key, String cursor);
	}

	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES]</li>
	 * <li><a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractSortedSetZunionBuilder<A, B, C extends AbstractSortedSetZunionBuilder<A, B, C>> {
		
		/**
		 * 
		 * @param weight
		 * @return 
		 */
		C weight(double weight);
		
		/**
		 * 
		 * @return 
		 */
		C sum();
		
		/**
		 * 
		 * @return 
		 */
		C min();
		
		/**
		 * 
		 * @return 
		 */
		C max();
	}
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZunionBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZunionBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<B> build(A key);
		
		/**
		 * 
		 * @param keys
		 * @return 
		 */
		Flux<B> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zunion">ZUNION</a> numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] [WITHSCORES] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZunionWithScoresBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZunionWithScoresBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<SortedSetScoredMember<B>> build(A key);
		
		/**
		 * 
		 * @param keys
		 * @return 
		 */
		Flux<SortedSetScoredMember<B>> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/zunionstore">ZUNIONSTORE</a> destination numkeys key [key ...] [WEIGHTS weight [weight ...]] [AGGREGATE SUM|MIN|MAX] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SortedSetZunionstoreBuilder<A, B> extends AbstractSortedSetZinterBuilder<A, B, SortedSetZunionstoreBuilder<A, B>> {
		
		/**
		 * 
		 * @param destination
		 * @param key
		 * @return 
		 */
		Mono<Long> build(A destination, A key);
		
		/**
		 * 
		 * @param destination
		 * @param keys
		 * @return 
		 */
		Mono<Long> build(A destination, Consumer<Keys<A>> keys);
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <B> value type
	 */
	interface SortedSetScanResult<B> extends AbstractScanResult {
		
		/**
		 * 
		 * @return 
		 */
		List<SortedSetScoredMember<B>> getMembers();
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <B> value type
	 */
	interface SortedSetScoredMember<B> {
		
		/**
		 * 
		 * @return 
		 */
		B getValue();
		
		/**
		 * 
		 * @return 
		 */
		double getScore();
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <B> value type
	 */
	interface SortedSetScoredMembers<B> {
		
		/**
		 * 
		 * @param score
		 * @param value
		 * @return 
		 */
		SortedSetScoredMembers<B> entry(double score, B value);
	}
}


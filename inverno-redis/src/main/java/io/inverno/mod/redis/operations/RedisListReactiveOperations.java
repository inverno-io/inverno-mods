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

import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Lists reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisListReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/blmove">BLMOVE</a> source destination LEFT|RIGHT LEFT|RIGHT timeout 
	 * 
	 * @return 
	 */
	ListBlmoveBuilder<A, B> blmove();
	
	/**
	 * <a href="https://redis.io/commands/blmpop">BLMPOP</a> timeout numkeys key [key ...] LEFT|RIGHT [COUNT count] 
	 * 
	 * @return
	 */
	ListBlmpopBuilder<A, B> blmpop();
	
	/**
	 * <a href="https://redis.io/commands/blpop">BLPOP</a> key timeout 
	 * 
	 * @param key
	 * @param timeout
	 * @return 
	 */
	Mono<EntryOptional<A, B>> blpop(A key, double timeout);
	
	/**
	 * <a href="https://redis.io/commands/blpop">BLPOP</a> key [key ...] timeout 
	 * 
	 * @param keys
	 * @param timeout
	 * @return 
	 */
	Mono<EntryOptional<A, B>> blpop(Consumer<Keys<A>> keys, double timeout);
	
	/**
	 * <a href="https://redis.io/commands/brpop">BRPOP</a> key timeout 
	 * 
	 * @param key
	 * @param timeout
	 * @return 
	 */
	Mono<EntryOptional<A, B>> brpop(A key, double timeout);

	/**
	 * <a href="https://redis.io/commands/brpop">BRPOP</a> key [key ...] timeout 
	 * 
	 * @param keys
	 * @param timeout
	 * @return 
	 */
	Mono<EntryOptional<A, B>> brpop(Consumer<Keys<A>> keys, double timeout);
	
	/**
	 * <a href="https://redis.io/commands/brpoplpush">BRPOPLPUSH</a> source destination timeout
	 * 
	 * @param source
	 * @param destination
	 * @param timeout
	 * @return 
	 */
	Mono<B> brpoplpush(A source, A destination, double timeout);
	
	/**
	 * <a href="https://redis.io/commands/lindex">LINDEX</a> key index
	 * 
	 * @param key
	 * @param index
	 * @return 
	 */
	Mono<B> lindex(A key, long index);
	
	/**
	 * <a href="https://redis.io/commands/linsert">LINSERT</a> key BEFORE|AFTER pivot element 
	 * 
	 * @param key
	 * @param before
	 * @param pivot
	 * @param element
	 * @return 
	 */
	Mono<Long> linsert(A key, boolean before, B pivot, B element);
	
	/**
	 * <a href="https://redis.io/commands/llen">LLEN</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> llen(A key);
	
	/**
	 * <a href="https://redis.io/commands/lmove">LMOVE</a> source destination LEFT|RIGHT LEFT|RIGHT 
	 * 
	 * @return 
	 */
	ListLmoveBuilder<A, B> lmove();
	
	/**
	 * <a href="https://redis.io/commands/lmpop">LMPOP</a> numkeys key [key ...] LEFT|RIGHT [COUNT count] 
	 * 
	 * @return
	 */
	ListLmpopBuilder<A, B> lmpop();
	
	/**
	 * <a href="https://redis.io/commands/blmpop">LPOP</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> lpop(A key);
	
	/**
	 * <a href="https://redis.io/commands/blmpop">LPOP</a> key [count]
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<B> lpop(A key, long count);

	/**
	 * <a href="https://redis.io/commands/lpos">LPOS</a> key element
	 * 
	 * @param key
	 * @param element
	 * @return 
	 */
	Mono<Long> lpos(A key, B element);
	
	/**
	 * <a href="https://redis.io/commands/lpos">LPOS</a> key element [COUNT num-matches]
	 * 
	 * @param key
	 * @param element
	 * @param count
	 * @return 
	 */
	Flux<Long> lpos(A key, B element, long count);
	
	/**
	 * <a href="https://redis.io/commands/lpos">LPOS</a> key element [RANK rank] [COUNT num-matches] [MAXLEN len] 
	 * 
	 * @return 
	 */
	ListLposBuilder<A, B> lpos();

	/**
	 * <a href="https://redis.io/commands/lpush">LPUSH</a> key element
	 * 
	 * @param key
	 * @param element
	 * @return 
	 */
	Mono<Long> lpush(A key, B element);
	
	/**
	 * <a href="https://redis.io/commands/lpush">LPUSH</a> key element [element ...]
	 * 
	 * @param key
	 * @param elements
	 * @return 
	 */
	Mono<Long> lpush(A key, Consumer<Values<B>> elements);
	
	/**
	 * <a href="https://redis.io/commands/lpushx">LPUSHX</a> key element
	 * 
	 * @param key
	 * @param element
	 * @return 
	 */
	Mono<Long> lpushx(A key, B element);
	
	/**
	 * <a href="https://redis.io/commands/lpushx">LPUSHX</a> key element [element ...]
	 * 
	 * @param key
	 * @param elements
	 * @return 
	 */
	Mono<Long> lpushx(A key, Consumer<Values<B>> elements);

	
	/**
	 * <a href="https://redis.io/commands/lrange">LRANGE</a> key start stop
	 * 
	 * @param key
	 * @param start
	 * @param stop
	 * @return 
	 */
	Flux<B> lrange(A key, long start, long stop);
	
	/**
	 * <a href="https://redis.io/commands/lrem">LREM</a> key count element
	 * 
	 * @param key
	 * @param count
	 * @param element
	 * @return 
	 */
	Mono<Long> lrem(A key, long count, B element);
	
	/**
	 * <a href="https://redis.io/commands/lset">LSET</a> key index element
	 * 
	 * @param key
	 * @param index
	 * @param element
	 * @return 
	 */
	Mono<String> lset(A key, long index, B element);
	
	/**
	 * <a href="https://redis.io/commands/ltrim">LTRIM</a> key start stop
	 * 
	 * @param key
	 * @param start
	 * @param stop
	 * @return 
	 */
	Mono<String> ltrim(A key, long start, long stop);
	
	/**
	 * <a href="https://redis.io/commands/rpop">RPOP</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> rpop(A key);
	
	/**
	 * <a href="https://redis.io/commands/rpop">RPOP</a> key [count] 
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<B> rpop(A key, long count);

	/**
	 * <a href="https://redis.io/commands/rpoplpush">RPOPLPUSH</a> source destination
	 * 
	 * @param source
	 * @param destination
	 * @return 
	 */
	Mono<B> rpoplpush(A source, A destination);
	
	/**
	 * <a href="https://redis.io/commands/rpush">RPUSH</a> key element [element ...]
	 * 
	 * @param key
	 * @param element
	 * @return 
	 */
	Mono<Long> rpush(A key, B element);
	
	/**
	 * <a href="https://redis.io/commands/rpush">RPUSH</a> key element [element ...]
	 * 
	 * @param key
	 * @param elements
	 * @return 
	 */
	Mono<Long> rpush(A key, Consumer<Values<B>> elements);
	
	/**
	 * <a href="https://redis.io/commands/rpushx">RPUSHX</a> key element [element ...]
	 * 
	 * @param key
	 * @param element
	 * @return 
	 */
	Mono<Long> rpushx(A key, B element);
	
	/**
	 * <a href="https://redis.io/commands/rpushx">RPUSHX</a> key element [element ...]
	 * 
	 * @param key
	 * @param elements
	 * @return 
	 */
	Mono<Long> rpushx(A key, Consumer<Values<B>> elements);

	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/blmove">BLMOVE</a> source destination LEFT|RIGHT LEFT|RIGHT timeout</li>
	 * <li><a href="https://redis.io/commands/lmove">LMOVE</a> source destination LEFT|RIGHT LEFT|RIGHT</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractListLmoveBuilder<A, B, C extends AbstractListLmoveBuilder<A, B, C>> {
		
		/**
		 * 
		 * @return 
		 */
		C leftLeft();
		
		/**
		 * 
		 * @return 
		 */
		C leftRight();
		
		/**
		 * 
		 * @return 
		 */
		C rightLeft();
		
		/**
		 * 
		 * @return 
		 */
		C rightRight();
	}
	
	/**
	 * <ul>
	 * <li><a href="https://redis.io/commands/blmpop">BLMPOP</a> timeout numkeys key [key ...] LEFT|RIGHT [COUNT count]</li>
	 * <li><a href="https://redis.io/commands/lmpop">LMPOP</a> numkeys key [key ...] LEFT|RIGHT [COUNT count]</li>
	 * </ul>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractListLmpopBuilder<A, B, C extends AbstractListLmpopBuilder<A, B, C>> {
		
		/**
		 * 
		 * @return 
		 */
		C left();
		
		/**
		 * 
		 * @return 
		 */
		C right();
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		C count(long count);
	}

	/**
	 * <a href="https://redis.io/commands/blmove">BLMOVE</a> source destination LEFT|RIGHT LEFT|RIGHT timeout 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface ListBlmoveBuilder<A, B> extends AbstractListLmoveBuilder<A, B, ListBlmoveBuilder<A, B>> {
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @param timeout
		 * @return 
		 */
		Mono<B> build(A source, A destination, double timeout);
	}
	
	/**
	 * <a href="https://redis.io/commands/blmpop">BLMPOP</a> timeout numkeys key [key ...] LEFT|RIGHT [COUNT count] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface ListBlmpopBuilder<A, B> extends AbstractListLmpopBuilder<A, B, ListBlmpopBuilder<A, B>> {
		
		/**
		 * 
		 * @param timeout
		 * @param key
		 * @return 
		 */
		Flux<EntryOptional<A, B>> build(double timeout, A key);
		
		/**
		 * 
		 * @param timeout
		 * @param keys
		 * @return 
		 */
		Flux<EntryOptional<A, B>> build(double timeout, Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/lmove">LMOVE</a> source destination LEFT|RIGHT LEFT|RIGHT 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface ListLmoveBuilder<A, B> extends AbstractListLmoveBuilder<A, B, ListLmoveBuilder<A, B>> {
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @return 
		 */
		Mono<B> build(A source, A destination);
	}
	
	/**
	 * <a href="https://redis.io/commands/lmpop">LMPOP</a> numkeys key [key ...] LEFT|RIGHT [COUNT count] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface ListLmpopBuilder<A, B> extends AbstractListLmpopBuilder<A, B, ListLmpopBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<EntryOptional<A, B>> build(A key);
		
		/**
		 * 
		 * @param keys
		 * @return 
		 */
		Flux<EntryOptional<A, B>> build(Consumer<Keys<A>> keys);
	}
	
	/**
	 * <a href="https://redis.io/commands/lpos">LPOS</a> key element [RANK rank] [COUNT num-matches] [MAXLEN len] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface ListLposBuilder<A, B> {
		
		/**
		 * 
		 * @param rank
		 * @return 
		 */
		ListLposBuilder<A, B> rank(long rank);
		
		/**
		 * 
		 * @param maxlen
		 * @return 
		 */
		ListLposBuilder<A, B> maxlen(long maxlen);
		
		/**
		 * 
		 * @param key
		 * @param element
		 * @return 
		 */
		Mono<Long> build(A key, B element);
		
		/**
		 * 
		 * @param key
		 * @param element
		 * @param count
		 * @return 
		 */
		Flux<Long> build(A key, B element, long count);
	}
}

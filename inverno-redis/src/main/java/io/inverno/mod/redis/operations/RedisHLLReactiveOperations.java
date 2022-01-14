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
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Hyper Log Log reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisHLLReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/pfadd">PFADD</a> key element
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	Mono<Long> pfadd(A key, B value);
	
	/**
	 * <a href="https://redis.io/commands/pfadd">PFADD</a> key [element [element ...]] 
	 * 
	 * @param key
	 * @param values
	 * @return 
	 */
	Mono<Long> pfadd(A key, Consumer<Values<B>> values);
	
	/**
	 * <a href="https://redis.io/commands/pfcount">PFCOUNT</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> pfcount(A key);
	
	/**
	 * <a href="https://redis.io/commands/pfcount">PFCOUNT</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> pfcount(Consumer<Keys<A>> keys);

	/**
	 * <a href="https://redis.io/commands/pfmerge">PFMERGE</a> destkey sourcekey
	 * 
	 * @param destkey
	 * @param sourcekey
	 * @return 
	 */
	Mono<String> pfmerge(A destkey, A sourcekey);
	
	/**
	 * <a href="https://redis.io/commands/pfmerge">PFMERGE</a> destkey sourcekey [sourcekey ...] 
	 * 
	 * @param destkey
	 * @param sourcekeys
	 * @return 
	 */
	Mono<String> pfmerge(A destkey, Consumer<Keys<A>> sourcekeys);
}

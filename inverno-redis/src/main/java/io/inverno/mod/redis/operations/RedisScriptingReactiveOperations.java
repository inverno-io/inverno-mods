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
 * Redis Scripting reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisScriptingReactiveOperations<A, B> {

	/**
	 * 
	 * @param script
	 * @return 
	 */
	String digest(byte[] script);
	
	/**
	 * 
	 * @param script
	 * @return 
	 */
	String digest(String script);

	/**
	 * <a href="https://redis.io/commands/eval">EVAL</a> script 0
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @return 
	 */
	<T> Flux<T> eval(String script, ScriptOutput output);
	
	/**
	 * <a href="https://redis.io/commands/eval">EVAL</a> script 0
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @return 
	 */
	<T> Flux<T> eval(byte[] script, ScriptOutput output);
	
	/**
	 * <a href="https://redis.io/commands/eval">EVAL</a> script numkeys [key [key ...]]
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @return 
	 */
	<T> Flux<T> eval(String script, ScriptOutput output, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/eval">EVAL</a> script numkeys [key [key ...]]
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @return 
	 */
	<T> Flux<T> eval(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/eval">EVAL</a> script numkeys [key [key ...]] [arg [arg ...]] 
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @param args
	 * @return 
	 */
	<T> Flux<T> eval(String script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args);
	
	/**
	 * <a href="https://redis.io/commands/eval">EVAL</a> script numkeys [key [key ...]] [arg [arg ...]] 
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @param args
	 * @return 
	 */
	<T> Flux<T> eval(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args);
	
	/**
	 * <a href="https://redis.io/commands/eval_ro">EVAL_RO</a> script 0
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @return 
	 */
	<T> Flux<T> eval_ro(String script, ScriptOutput output);
	
	/**
	 * <a href="https://redis.io/commands/eval_ro">EVAL_RO</a> script 0
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @return 
	 */
	<T> Flux<T> eval_ro(byte[] script, ScriptOutput output);
	
	/**
	 * <a href="https://redis.io/commands/eval_ro">EVAL_RO</a> script numkeys [key [key ...]]
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @return 
	 */
	<T> Flux<T> eval_ro(String script, ScriptOutput output, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/eval_ro">EVAL_RO</a> script numkeys [key [key ...]]
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @return 
	 */
	<T> Flux<T> eval_ro(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/eval_ro">EVAL_RO</a> script numkeys [key [key ...]] [arg [arg ...]] 
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @param args
	 * @return 
	 */
	<T> Flux<T> eval_ro(String script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args);
	
	/**
	 * <a href="https://redis.io/commands/eval_ro">EVAL_RO</a> script numkeys [key [key ...]] [arg [arg ...]] 
	 * 
	 * @param <T>
	 * @param script
	 * @param output
	 * @param keys
	 * @param args
	 * @return 
	 */
	<T> Flux<T> eval_ro(byte[] script, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args);
	
	/**
	 * <a href="https://redis.io/commands/evalsha">EVALSHA</a> script 0
	 * 
	 * @param <T>
	 * @param digest
	 * @param output
	 * @return 
	 */
	<T> Flux<T> evalsha(String digest, ScriptOutput output);
	
	/**
	 * <a href="https://redis.io/commands/evalsha">EVALSHA</a> script numkeys [key [key ...]]
	 * 
	 * @param <T>
	 * @param digest
	 * @param output
	 * @param keys
	 * @return 
	 */
	<T> Flux<T> evalsha(String digest, ScriptOutput output, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/evalsha">EVALSHA</a> script numkeys [key [key ...]] [arg [arg ...]] 
	 * 
	 * @param <T>
	 * @param digest
	 * @param output
	 * @param keys
	 * @param args
	 * @return 
	 */
	<T> Flux<T> evalsha(String digest, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args);
	
	/**
	 * <a href="https://redis.io/commands/evalsha_ro">EVALSHA_RO</a> script 0
	 * 
	 * @param <T>
	 * @param digest
	 * @param output
	 * @return 
	 */
	<T> Flux<T> evalsha_ro(String digest, ScriptOutput output);
	
	/**
	 * <a href="https://redis.io/commands/evalsha_ro">EVALSHA_RO</a> script numkeys [key [key ...]]
	 * 
	 * @param <T>
	 * @param digest
	 * @param output
	 * @param keys
	 * @return 
	 */
	<T> Flux<T> evalsha_ro(String digest, ScriptOutput output, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/evalsha_ro">EVALSHA_RO</a> script numkeys [key [key ...]] [arg [arg ...]] 
	 * 
	 * @param <T>
	 * @param digest
	 * @param output
	 * @param keys
	 * @param args
	 * @return 
	 */
	<T> Flux<T> evalsha_ro(String digest, ScriptOutput output, Consumer<Keys<A>> keys, Consumer<Values<B>> args);
	
	/**
	 * <a href="https://redis.io/commands/script-exists">SCRIPT EXISTS</a> sha1 [sha1 ...]
	 * 
	 * @param digests
	 * @return 
	 */
	Flux<Boolean> scriptExists(String... digests);
	
	/**
	 * <a href="https://redis.io/commands/script-flush">SCRIPT FLUSH</a> 
	 * 
	 * @return 
	 */
	Mono<String> scriptFlush();
	
	/**
	 * <a href="https://redis.io/commands/script-flush">SCRIPT FLUSH</a> [ASYNC|SYNC] 
	 * 
	 * @param flushMode
	 * @return 
	 */
	Mono<String> scriptFlush(ScriptFlushMode flushMode);

	/**
	 * <a href="https://redis.io/commands/script-kill">SCRIPT KILL</a>
	 * 
	 * @return 
	 */
	Mono<String> scriptKill();
	
	/**
	 * <a href="https://redis.io/commands/script-load">SCRIPT LOAD</a> script
	 * 
	 * @param script
	 * @return 
	 */
	Mono<String> scriptLoad(String script);
	
	/**
	 * <a href="https://redis.io/commands/script-load">SCRIPT LOAD</a> script
	 * 
	 * @param script
	 * @return 
	 */
	Mono<String> scriptLoad(byte[] script);

	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	enum ScriptOutput {
		/**
		 * 
		 */
		BOOLEAN, 
		/**
		 * 
		 */
		INTEGER,
		/**
		 * 
		 */
		MULTI, 
		/**
		 * 
		 */
		STATUS, 
		/**
		 * 
		 */
		VALUE;
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	enum ScriptFlushMode {
		/**
		 * 
		 */
		SYNC,
		/**
		 * 
		 */
		ASYNC;
	}
}

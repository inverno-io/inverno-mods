/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.operations;

import io.inverno.mod.redis.util.Keys;
import io.inverno.mod.redis.util.Values;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public interface RedisScriptingReactiveOperations<A, B> /*extends RedisScriptingReactiveCommands<A, B>*/ {

	String digest(byte[] script);

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

	
	enum ScriptOutput {
		BOOLEAN, 
		INTEGER,
		MULTI, 
		STATUS, 
		VALUE;
	}
	
	enum ScriptFlushMode {
		SYNC,
		ASYNC;
	}
}

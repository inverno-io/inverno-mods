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

import io.inverno.mod.redis.operations.RedisSortedSetReactiveOperations;
import io.inverno.mod.redis.operations.Keys;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
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
public class SortedSetZunionstoreBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisSortedSetReactiveOperations.SortedSetZunionstoreBuilder<A, B> {

	private final RedisSortedSetReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	private final Class<A> keyType;

	private List<Double> weights;
	private boolean sum;
	private boolean min;
	private boolean max;
	
	/**
	 * 
	 * @param commands
	 * @param keyType 
	 */
	public SortedSetZunionstoreBuilderImpl(RedisSortedSetReactiveCommands<A, B> commands, Class<A> keyType) {
		this.commands = commands;
		this.connection = null;
		this.keyType = keyType;
	}
	
	/**
	 * 
	 * @param connection
	 * @param keyType 
	 */
	public SortedSetZunionstoreBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection, Class<A> keyType) {
		this.commands = null;
		this.connection = connection;
		this.keyType = keyType;
	}

	@Override
	public SortedSetZunionstoreBuilderImpl<A, B, C> weight(double weight) {
		if(this.weights == null) {
			this.weights = new LinkedList<>();
		}
		this.weights.add(weight);
		return this;
	}

	@Override
	public SortedSetZunionstoreBuilderImpl<A, B, C> sum() {
		this.sum = true;
		this.min = false;
		this.max = false;
		return this;
	}

	@Override
	public SortedSetZunionstoreBuilderImpl<A, B, C> min() {
		this.sum = false;
		this.min = true;
		this.max = false;
		return this;
	}

	@Override
	public SortedSetZunionstoreBuilderImpl<A, B, C> max() {
		this.sum = false;
		this.min = false;
		this.max = true;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected ZStoreArgs buildZStoreArgs() {
		ZStoreArgs zstoreArgs = new ZStoreArgs();

		if(this.weights != null) {
			zstoreArgs.weights(this.weights.stream().mapToDouble(d -> d).toArray());
		}
		
		if(this.sum) {
			zstoreArgs.sum();
		}
		else if(this.min) {
			zstoreArgs.min();
		}
		else if(this.max) {
			zstoreArgs.max();
		}
		
		return zstoreArgs;
	}
	
	@Override
	public Mono<Long> build(A destination, A key) {
		if(this.commands != null) {
			return this.build(this.commands, destination, key);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), destination, key), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param destination
	 * @param key
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	private Mono<Long> build(RedisSortedSetReactiveCommands<A, B> localCommands, A destination, A key) {
		return localCommands.zunionstore(destination, this.buildZStoreArgs(), key);
	}

	@Override
	public Mono<Long> build(A destination, Consumer<Keys<A>> keys) {
		if(this.commands != null) {
			return this.build(this.commands, destination, keys);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), destination, keys), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param destination
	 * @param keys
	 * @return 
	 */
	private Mono<Long> build(RedisSortedSetReactiveCommands<A, B> localCommands, A destination, Consumer<Keys<A>> keys) {
		KeysImpl<A> keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(keysConfigurator);
		return localCommands.zunionstore(destination, this.buildZStoreArgs(), keysConfigurator.getKeys());
	}
}

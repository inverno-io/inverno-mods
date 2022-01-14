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
import io.lettuce.core.ZAddArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
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
public class SortedSetZaddBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisSortedSetReactiveOperations.SortedSetZaddBuilder<A, B> {

	private final RedisSortedSetReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean nx;
	private boolean xx;
	private boolean gt;
	private boolean lt;
	private boolean ch;
	
	/**
	 * 
	 * @param commands 
	 */
	public SortedSetZaddBuilderImpl(RedisSortedSetReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public SortedSetZaddBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public SortedSetZaddBuilderImpl<A, B, C> nx() {
		this.nx = true;
		this.xx = false;
		return this;
	}

	@Override
	public SortedSetZaddBuilderImpl<A, B, C> xx() {
		this.nx = false;
		this.xx = true;
		return this;
	}

	@Override
	public SortedSetZaddBuilderImpl<A, B, C> gt() {
		this.gt = true;
		this.lt = false;
		return this;
	}
	
	@Override
	public SortedSetZaddBuilderImpl<A, B, C> lt() {
		this.gt = false;
		this.lt = true;
		return this;
	}

	@Override
	public SortedSetZaddBuilderImpl<A, B, C> ch() {
		this.ch = true;
		return this;
	}

	/**
	 * 
	 * @return 
	 */
	protected ZAddArgs buildZAddArgs() {
		ZAddArgs zaddArgs = new ZAddArgs();
		
		if(this.nx) {
			zaddArgs.nx();
		}
		else if(this.xx) {
			zaddArgs.xx();
		}
		
		if(this.gt) {
			zaddArgs.gt();
		}
		else if(this.lt) {
			zaddArgs.lt();
		}
		
		if(this.ch) {
			zaddArgs.ch();
		}
		
		return zaddArgs;
	}

	@Override
	public Mono<Long> build(A key, double score, B member) {
		if(this.commands != null) {
			return this.build(this.commands, key, score, member);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, score, member), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param score
	 * @param member
	 * @return 
	 */
	private Mono<Long> build(RedisSortedSetReactiveCommands<A, B> localCommands, A key, double score, B member) {
		ZAddArgs zaddArgs = this.buildZAddArgs();
		if(zaddArgs != null) {
			return localCommands.zadd(key, zaddArgs, score, member);
		}
		else {
			return localCommands.zadd(key, score, member);
		}
	}

	@Override
	public Mono<Long> build(A key, Consumer<RedisSortedSetReactiveOperations.SortedSetScoredMembers<B>> members) {
		if(this.commands != null) {
			return this.build(this.commands, key, members);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, members), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param members
	 * @return 
	 */
	private Mono<Long> build(RedisSortedSetReactiveCommands<A, B> localCommands, A key, Consumer<RedisSortedSetReactiveOperations.SortedSetScoredMembers<B>> members) {
		SortedSetScoredMembersImpl<B> membersConfigurator = new SortedSetScoredMembersImpl<>();
		members.accept(membersConfigurator);
		ZAddArgs zaddArgs = this.buildZAddArgs();
		if(zaddArgs != null) {
			return localCommands.zadd(key, zaddArgs, membersConfigurator.getScoredValues());
		}
		else {
			return localCommands.zadd(key, membersConfigurator.getScoredValues());
		}
	}
}

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

import io.inverno.mod.redis.operations.RedisKeyReactiveOperations;
import io.inverno.mod.redis.operations.Keys;
import io.lettuce.core.MigrateArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import java.util.Objects;
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
public class KeyMigrateBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisKeyReactiveOperations.KeyMigrateBuilder<A> {

	private final RedisKeyReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	private final Class<A> keyType;
	
	private boolean copy;
	private boolean replace;
	private String username;
	private String password;
	private KeysImpl<A> keysConfigurator;
	
	/**
	 * 
	 * @param commands
	 * @param keyType 
	 */
	public KeyMigrateBuilderImpl(RedisKeyReactiveCommands<A, B> commands, Class<A> keyType) {
		this.commands = commands;
		this.connection = null;
		this.keyType = keyType;
	}
	
	/**
	 * 
	 * @param connection
	 * @param keyType 
	 */
	public KeyMigrateBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection, Class<A> keyType) {
		this.commands = null;
		this.connection = connection;
		this.keyType = keyType;
	}

	@Override
	public KeyMigrateBuilderImpl<A, B, C> copy() {
		this.copy = true;
		return this;
	}

	@Override
	public KeyMigrateBuilderImpl<A, B, C> replace() {
		this.replace = true;
		return this;
	}

	@Override
	public KeyMigrateBuilderImpl<A, B, C> auth(String password) {
		Objects.requireNonNull(password, "password");
		this.username = null;
		this.password = password;
		return this;
	}

	@Override
	public KeyMigrateBuilderImpl<A, B, C> auth(String username, String password) {
		Objects.requireNonNull(username, "username");
		Objects.requireNonNull(password, "password");
		this.username = username;
		this.password = password;
		return this;
	}

	@Override
	public KeyMigrateBuilderImpl<A, B, C> keys(Consumer<Keys<A>> keys) {
		Objects.requireNonNull(keys, "keys");
		this.keysConfigurator = new KeysImpl<>(this.keyType);
		keys.accept(this.keysConfigurator);
		return this;
	}

	/**
	 * 
	 * @param key
	 * @return 
	 */
	protected MigrateArgs<A> buildMigrateArgs(A key) {
		MigrateArgs<A> migrateArgs;
		if(this.keysConfigurator != null) {
			migrateArgs = MigrateArgs.Builder.keys(this.keysConfigurator.getKeys());
		}
		else {
			migrateArgs = MigrateArgs.Builder.key(key);
		}
		
		if(this.copy) {
			migrateArgs.copy();
		}
		if(this.replace) {
			migrateArgs.replace();
		}
		
		if(this.username != null) {
			migrateArgs.auth2(this.username, this.password);
		}
		else if(this.password != null) {
			migrateArgs.auth(this.password);
		}
		
		return migrateArgs;
	}
	
	@Override
	public Mono<String> build(String host, int port, A key, int db, long timeout) {
		if(this.commands != null) {
			return this.build(this.commands, host, port, key, db, timeout);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), host, port, key, db, timeout), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param host
	 * @param port
	 * @param key
	 * @param db
	 * @param timeout
	 * @return 
	 */
	private Mono<String> build(RedisKeyReactiveCommands<A, B> localCommands, String host, int port, A key, int db, long timeout) {
		return localCommands.migrate(host, port, db, timeout, this.buildMigrateArgs(key));
	}
}

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

/**
 * <p>
 * The Inverno Redis Client module which provides a Redis client based on Lettuce.
 * </p>
 *
 * <p>
 * This module exposes a pool based Redis client which is automatically created using the module's configuration which provides connection and pooling options. This client can then be used within
 * an application to execute Redis commands on a Redis datastore.
 * </p>
 *
 * <p>
 * It defines the following sockets:
 * </p>
 *
 * <dl>
 * <dt><b>reactor</b></dt>
 * <dd>the Inverno reactor</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the Lettuce Redis client module configuration</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 *
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the Lettuce Redis client module configuration</dd>
 * <dt><b>lettuceRedisClient</b></dt>
 * <dd>the Lettuce pool Redis client to execute Redis commands on a Redis datastore</dd>
 * </dl>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.redis.lettuce {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...

	requires io.inverno.mod.base;
	requires transitive io.inverno.mod.configuration;
	requires transitive io.inverno.mod.redis;
	
	requires jdk.unsupported; // required by netty for low level API for accessing direct buffers
	requires lettuce.core;
	requires org.apache.commons.lang3;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;

	exports io.inverno.mod.redis.lettuce;
}

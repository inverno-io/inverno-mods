/*
 * Copyright 2021 Jeremy KUHN
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
 * Defines the foundational APIs of the Inverno framework modules.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
module io.inverno.mod.base {
	requires io.netty.common;
	requires transitive io.netty.buffer;
	requires io.netty.resolver;
	requires transitive io.netty.transport;
	requires static io.vertx.core;
	requires org.apache.commons.text;
	requires org.apache.commons.lang3;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;

	exports io.inverno.mod.base;
	exports io.inverno.mod.base.concurrent;
	exports io.inverno.mod.base.converter;
	exports io.inverno.mod.base.net;
	exports io.inverno.mod.base.reflect;
	exports io.inverno.mod.base.resource;
}
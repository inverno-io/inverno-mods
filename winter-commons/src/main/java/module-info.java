/*
 * Copyright 2020 Jeremy KUHN
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
 * 
 * @author jkuhn
 *
 */
@io.winterframework.core.annotation.Module
module io.winterframework.mod.commons {
	requires io.winterframework.core;
	requires io.winterframework.mod.configuration;
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	
	requires transitive io.netty.buffer;
	requires transitive io.netty.transport;
	requires io.netty.common;
	
	requires static io.netty.transport.epoll;
	requires static io.netty.transport.unix.common;
	requires static io.netty.transport.kqueue;
	
	exports io.winterframework.mod.commons;
	exports io.winterframework.mod.commons.net;
	exports io.winterframework.mod.commons.resource;
}
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
@io.winterframework.core.annotation.Module(excludes = {"io.winterframework.mod.commons"})
module io.winterframework.mod.web {
	requires io.winterframework.core;
	requires transitive io.winterframework.mod.commons;
	requires io.winterframework.mod.configuration;
	
	requires org.apache.logging.log4j;
	requires com.fasterxml.jackson.databind;
	
	requires jdk.unsupported;
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.transport;
	requires io.netty.transport.epoll;
	requires io.netty.transport.unix.common;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.codec.http2;
	requires io.netty.handler;
	
	exports io.winterframework.mod.web;
	exports io.winterframework.mod.web.handler;
	exports io.winterframework.mod.web.router;
}
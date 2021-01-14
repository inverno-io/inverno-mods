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
 * 
 * @author jkuhn
 *
 */
module io.winterframework.mod.base {
	requires org.apache.commons.text;
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	
	requires io.netty.common;
	requires transitive io.netty.buffer;
	requires transitive io.netty.transport;
	
	exports io.winterframework.mod.base.net;
	exports io.winterframework.mod.base.resource;
	exports io.winterframework.mod.base.converter;
}
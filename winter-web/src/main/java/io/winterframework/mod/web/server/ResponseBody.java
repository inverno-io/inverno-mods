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
package io.winterframework.mod.web.server;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;

/**
 * @author jkuhn
 *
 */
public interface ResponseBody {

	void empty();
	
	// We can only call one of these once
	ResponseData<ByteBuf> raw();
	
	ResponseBody.Resource resource();
	
	public static interface Resource {
		
		void value(io.winterframework.mod.base.resource.Resource resource);
	}
	
	ResponseBody.Sse<ByteBuf, ResponseBody.Sse.Event<ByteBuf>, ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>>> sse();
	
	public interface Sse<A, B extends ResponseBody.Sse.Event<A>, C extends ResponseBody.Sse.EventFactory<A, B>> {
		
		void from(BiConsumer<C, ResponseData<B>> data);
		
		public static interface Event<A> extends ResponseData<A> {
			
			ResponseBody.Sse.Event<A> id(String id);
				
			ResponseBody.Sse.Event<A> comment(String comment);
			
			ResponseBody.Sse.Event<A> event(String event);
		}

		@FunctionalInterface
		public static interface EventFactory<A, B extends ResponseBody.Sse.Event<A>> {
			
			B create(Consumer<B> configurer);
			
			default B create() {
				return this.create(event -> {});
			}
		}
	}
}

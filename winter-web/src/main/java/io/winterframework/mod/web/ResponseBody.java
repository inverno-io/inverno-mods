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
package io.winterframework.mod.web;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;

/**
 * @author jkuhn
 *
 */
public interface ResponseBody {

	Response<Void> empty();
	
	// We can only call one of data().data and events once
	ResponseBody.Data data();
	
	ResponseBody.Sse<ByteBuf> sse();
	
	public static interface Data {
		Response<ResponseBody.Data> data(Publisher<ByteBuf> data);
	}
	
	public static interface Sse<T> {
		Response<ResponseBody.Sse<T>> events(Publisher<ServerSentEvent<T>> events);
		
		ServerSentEvent<T> create(Consumer<ServerSentEvent.Configurator<T>> configurer);
	}
}

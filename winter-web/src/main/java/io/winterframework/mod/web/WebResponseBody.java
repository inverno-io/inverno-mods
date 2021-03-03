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
package io.winterframework.mod.web;

import java.lang.reflect.Type;

import io.winterframework.mod.http.server.ResponseBody;

/**
 * @author jkuhn
 *
 */
public interface WebResponseBody extends ResponseBody {
	
	<T> ResponseDataEncoder<T> encoder();
	
	<T> ResponseDataEncoder<T> encoder(Class<T> type);
	
	<T> ResponseDataEncoder<T> encoder(Type type);
	
	<T> WebResponseBody.SseEncoder<T> sseEncoder(String mediaType);
	
	<T> WebResponseBody.SseEncoder<T> sseEncoder(String mediaType, Class<T> type);
	
	<T> WebResponseBody.SseEncoder<T> sseEncoder(String mediaType, Type type);
	
	public interface SseEncoder<A> extends ResponseBody.Sse<A, WebResponseBody.SseEncoder.Event<A>, WebResponseBody.SseEncoder.EventFactory<A>> {
		
		public static interface Event<A> extends ResponseBody.Sse.Event<A>, ResponseDataEncoder<A> {

			@Override
			WebResponseBody.SseEncoder.Event<A> id(String id);
			
			@Override
			WebResponseBody.SseEncoder.Event<A> comment(String comment);
			
			@Override
			WebResponseBody.SseEncoder.Event<A> event(String event);
		}

		public static interface EventFactory<A> extends ResponseBody.Sse.EventFactory<A, WebResponseBody.SseEncoder.Event<A>> {
			
		}
	}
}
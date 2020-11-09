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
package io.winterframework.mod.web.internal;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.Headers;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public interface RequestBodyDecoder<A> {

	Flux<A> decode(Flux<ByteBuf> data, Headers.ContentType contentType);
	
	public static class MalformedBodyException extends RootException {

		private static final long serialVersionUID = 7514879115021753110L;

		public MalformedBodyException() {
			super();
		}

		public MalformedBodyException(String message, Throwable cause, boolean enableSuppression,
				boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public MalformedBodyException(String message, Throwable cause) {
			super(message, cause);
		}

		public MalformedBodyException(String message) {
			super(message);
		}

		public MalformedBodyException(Throwable cause) {
			super(cause);
		}
	}
}

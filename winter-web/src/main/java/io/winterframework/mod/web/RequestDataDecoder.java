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

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.converter.MediaTypeConverter;
import io.winterframework.mod.http.server.RequestData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A request payload consumer used to decode the payload to a single object or
 * many objects.
 * </p>
 * 
 * <p>
 * Implementors should rely on a {@link MediaTypeConverter} to decode a raw
 * payload as a publisher of {@link ByteBuf} to a publisher of decoded objects.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see MediaTypeConverter
 * 
 * @param <A> the type of the decoded object
 */
public interface RequestDataDecoder<A> extends RequestData<A> {
	
	/**
	 * <p>
	 * Decodes the payload into one single object.
	 * </p>
	 * 
	 * @return a mono emitting the decoded object
	 */
	Mono<A> one();
	
	/**
	 * <p>
	 * Decodes the payload into many objects.
	 * </p>
	 * 
	 * @return a flux emitting the decoded objects
	 */
	Flux<A> many();
}

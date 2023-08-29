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
package io.inverno.mod.http.client;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 * <p>
 * An interceptable request body allows to intercept and transform the actual request body sent to the endpoint.
 * </p>
 * 
 * <p>
 * It is exposed in the {@link InterceptableRequest} when the request method allows it, namely {@code POST}, {@code PUT} or {@code DELETE}
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see InterceptableRequest
 */
public interface InterceptableRequestBody {
	
	/**
	 * <p>
	 * Transforms the request payload publisher.
	 * </p>
	 *
	 * <p>
	 * This can be used in an exchange interceptor in order to decorate the request data publisher.
	 * </p>
	 *
	 * @param transformer a payload publisher transformer
	 *
	 * @return the request body
	 * 
	 * @throw IllegalStateException if the request payload publisher has already been subscribed
	 */
	InterceptableRequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException;
}

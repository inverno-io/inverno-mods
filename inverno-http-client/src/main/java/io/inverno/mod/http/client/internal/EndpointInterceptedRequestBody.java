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
package io.inverno.mod.http.client.internal;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import io.inverno.mod.http.client.InterceptedRequestBody;

/**
 * <p>
 * An {@link InterceptedRequestBody} implementation that wraps the {@link EndpointRequestBody} so it can be intercepted in an {@link io.inverno.mod.http.client.ExchangeInterceptor}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointInterceptedRequestBody implements InterceptedRequestBody {

	private final EndpointRequestBody endpointRequestBody;
	
	/**
	 * <p>
	 * Creates an intercepted endpoint request body.
	 * </p>
	 * 
	 * @param endpointRequestBody the endpoint request body
	 */
	public EndpointInterceptedRequestBody(EndpointRequestBody endpointRequestBody) {
		this.endpointRequestBody = endpointRequestBody;
	}

	@Override
	public InterceptedRequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException {
		if(this.endpointRequestBody != null) {
			this.endpointRequestBody.transform(transformer);
		}
		return this;
	}
}

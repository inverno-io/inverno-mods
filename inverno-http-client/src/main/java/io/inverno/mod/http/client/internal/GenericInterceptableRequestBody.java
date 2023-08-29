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
import io.inverno.mod.http.client.InterceptableRequestBody;

/**
 * <p>
 * Generic {@link InterceptableRequestBody}
 * </p>
 * 
 * <p>
 * This is the request body exposed to the interceptors in the interceptable exchange.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class GenericInterceptableRequestBody implements InterceptableRequestBody {

	private Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer;
	
	private GenericRequestBody sentRequestBody;

	@Override
	public InterceptableRequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) throws IllegalStateException {
		if(this.sentRequestBody != null) {
			this.sentRequestBody.transform(transformer);
		}
		else {
			if(this.transformer == null) {
				this.transformer = transformer;
			}
			else {
				this.transformer = this.transformer.andThen(transformer);
			}
		}
		return this;
	}

	/**
	 * <p>
	 * Injects the request body actually sent with the request sent to the endpoint.
	 * </p>
	 * 
	 * @param sentRequestBody the request body sent to the endpoint
	 */
	public void setSentRequestBody(GenericRequestBody sentRequestBody) {
		this.sentRequestBody = sentRequestBody;
	}
	
	/**
	 * <p>
	 * Returns the payload publisher transformer set on the request body.
	 * </p>
	 * 
	 * @return a payload publisher transformer or null
	 */
	public Function<Publisher<ByteBuf>, Publisher<ByteBuf>> getTransformer() {
		return transformer;
	}
}

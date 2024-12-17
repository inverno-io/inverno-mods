/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.http2;

import io.inverno.mod.http.client.internal.EndpointRequestBody;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Http/2 request body.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class Http2RequestBody {
	
	private final Publisher<ByteBuf> data;

	/**
	 * <p>
	 * Creates an Http/2 request body.
	 * </p>
	 * 
	 * @param endpointRequestBody the original endpoint request body
	 */
	public Http2RequestBody(EndpointRequestBody endpointRequestBody) {
		this.data = endpointRequestBody.getData();
	}

	/**
	 * <p>
	 * Returns the response body data publisher.
	 * </p>
	 * 
	 * <p>
	 * The data publisher MUST be subscribed to produce a request body.
	 * </p>
	 * 
	 * @return the response body data publisher
	 */
	public Publisher<ByteBuf> getData() {
		return data;
	}
}

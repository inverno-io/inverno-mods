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
package io.inverno.mod.http.client.internal.v2.http2;

import io.inverno.mod.http.client.internal.EndpointRequestBody;
import io.inverno.mod.http.client.internal.HttpConnectionRequestBody;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class Http2RequestBodyV2 extends HttpConnectionRequestBody { // TODO remove extends
	
	private final Publisher<ByteBuf> data;

	public Http2RequestBodyV2(EndpointRequestBody endpointRequestBody) {
		super(Flux.empty()); // TODO remove
		this.data = endpointRequestBody.getData();
	}

	public Publisher<ByteBuf> getData() {
		return data;
	}
}

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

import io.inverno.mod.http.base.OutboundData;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface PreResponseBody {

	PreResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer);
	
	void empty();
	
	OutboundData<ByteBuf> raw();
	
	<T extends CharSequence> OutboundData<T> string();
	
	PreResponseBody.ResourceData resource();
	
	interface ResourceData {
		
		/**
		 * <p>
		 * Sets the specified resource in the response payload.
		 * </p>
		 * 
		 * <p>
		 * This method tries to determine the content type of the specified resource in
		 * which case the content type header is set in the response.
		 * </p>
		 * 
		 * @param resource a resource
		 * 
		 * @throws IllegalStateException if the payload has already been set
		 */
		void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException;
	}
}

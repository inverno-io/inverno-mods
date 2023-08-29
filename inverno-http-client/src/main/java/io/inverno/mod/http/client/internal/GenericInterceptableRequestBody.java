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
import io.inverno.mod.http.client.PreRequestBody;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericPreRequestBody implements PreRequestBody {

	private Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer;
	
	@Override
	public PreRequestBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		if(this.transformer == null) {
			this.transformer = transformer;
		}
		else {
			this.transformer = this.transformer.andThen(transformer);
		}
		return this;
	}

	public Function<Publisher<ByteBuf>, Publisher<ByteBuf>> getTransformer() {
		return transformer;
	}
}

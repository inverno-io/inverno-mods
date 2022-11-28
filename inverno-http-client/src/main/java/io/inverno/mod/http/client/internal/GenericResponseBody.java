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

import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.client.ResponseBody;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericResponseBody implements ResponseBody {

	private Publisher<ByteBuf> data;
	
	private GenericResponseBody.RawInboundData rawData;
	private GenericResponseBody.StringInboundData stringData;

	public GenericResponseBody(Publisher<ByteBuf> data) {
		this.data = data;
	}

	@Override
	public ResponseBody transform(Function<Publisher<ByteBuf>, Publisher<ByteBuf>> transformer) {
		this.data = transformer.apply(this.data);
		return this;
	}
	
	@Override
	public InboundData<ByteBuf> raw() throws IllegalStateException {
		if(this.rawData == null) {
			this.rawData = new RawInboundData();
		}
		return this.rawData;
	}

	@Override
	public InboundData<CharSequence> string() throws IllegalStateException {
		if(this.stringData == null) {
			this.stringData = new StringInboundData();
		}
		return this.stringData;
	}
	
	private class RawInboundData implements InboundData<ByteBuf> {

		@Override
		public Publisher<ByteBuf> stream() {
			return GenericResponseBody.this.data;
		}
	}
	
	private class StringInboundData implements InboundData<CharSequence> {

		@Override
		public Publisher<CharSequence> stream() {
			return Flux.from(GenericResponseBody.this.data).map(buf -> {
				try {
					return buf.toString(Charsets.DEFAULT);
				}
				finally {
					buf.release();
				}
			});
		}
	}
}

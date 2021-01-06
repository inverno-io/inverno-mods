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
package io.winterframework.mod.web.internal.server.http1x;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestHeaders;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.server.AbstractRequest;
import reactor.core.publisher.Sinks.Many;

/**
 * @author jkuhn
 *
 */
public class Http1xRequest extends AbstractRequest {

	public Http1xRequest(ChannelHandlerContext context, RequestHeaders requestHeaders, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder) {
		super(context, requestHeaders, urlEncodedBodyDecoder, multipartBodyDecoder);
	}
	
	@Override
	public Optional<Many<ByteBuf>> data() {
		// In order to support pipelining we must always create the data sink even if
		// it might not be consumed by the exchange handler
		// This comes from the fact that the exchange is only started after the previous
		// exchange has completed, which means we can receive data before we actually
		// invoke the exchange handler which is supposed to create the body
		this.body();
		return super.data();
	}
}

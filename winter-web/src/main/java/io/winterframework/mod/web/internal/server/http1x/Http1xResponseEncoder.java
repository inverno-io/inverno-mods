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

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.winterframework.mod.web.internal.netty.FlatHttpResponse;
import io.winterframework.mod.web.internal.netty.LinkedHttpHeaders;
import io.winterframework.mod.web.internal.server.WebServerByteBufAllocator;

/**
 * @author jkuhn
 *
 */
public class Http1xResponseEncoder extends HttpResponseEncoder {

	private ChannelHandlerContext context;

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
		super.encode(this.context, msg, out);
	}

	@Override
	protected void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
		if(headers instanceof LinkedHttpHeaders) {
			((LinkedHttpHeaders)headers).encode(buf);
		}
		else {
			super.encodeHeaders(headers, buf);
		}
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.context = WebServerByteBufAllocator.forceDirectAllocator(ctx);
		super.handlerAdded(ctx);
	}

	@Override
	protected boolean isContentAlwaysEmpty(HttpResponse msg) {
//		return super.isContentAlwaysEmpty(msg);
		// TODO can I know when the content is empty?
//		return super.isContentAlwaysEmpty(msg);
		// In HttpServerCodec this is tracked via a FIFO queue of HttpMethod
		// here we track it in the assembled response as we don't use HttpServerCodec
//		return (msg instanceof AssembledHttpResponse && ((AssembledHttpResponse) msg).head())
//				|| super.isContentAlwaysEmpty(msg);
		
		return (msg instanceof FlatHttpResponse && ((FlatHttpResponse) msg).isEmpty()) || super.isContentAlwaysEmpty(msg);
	}

}

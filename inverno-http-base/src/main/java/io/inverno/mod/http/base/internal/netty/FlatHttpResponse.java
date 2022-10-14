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
package io.inverno.mod.http.base.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * <p>
 * Optimized {@link HttpResponse} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class FlatHttpResponse implements HttpResponse, HttpContent {

	protected HttpVersion version;
	protected HttpResponseStatus status;
	protected HttpHeaders headers;
	protected final ByteBuf content;
	
	protected DecoderResult result = DecoderResult.SUCCESS;
	
	private boolean empty;

	public FlatHttpResponse(HttpVersion version, HttpResponseStatus status, HttpHeaders headers, boolean empty) {
		this(version, status, headers, Unpooled.EMPTY_BUFFER);
		this.empty = empty;
	}

	public FlatHttpResponse(HttpVersion version, HttpResponseStatus status, HttpHeaders headers, ByteBuf content) {
		this.status = status;
		this.version = version;
		this.headers = headers;
		this.content = content;
		this.empty = content.readableBytes() == 0;
	}

	public boolean isEmpty() {
		return empty;
	}
	
	@Override
	@Deprecated
	public HttpVersion getProtocolVersion() {
		return this.version;
	}

	@Override
	public HttpVersion protocolVersion() {
		return this.version;
	}

	@Override
	public HttpHeaders headers() {
		return this.headers;
	}

	@Override
	@Deprecated
	public DecoderResult getDecoderResult() {
		return this.result;
	}

	@Override
	public DecoderResult decoderResult() {
		return this.result;
	}

	@Override
	public void setDecoderResult(DecoderResult result) {
		this.result = result;
	}

	@Override
	public ByteBuf content() {
		return this.content;
	}

	@Override
	public int refCnt() {
		return this.content.refCnt();
	}

	@Override
	public boolean release() {
		return this.content.release();
	}

	@Override
	public boolean release(int decrement) {
		return this.content.release(decrement);
	}

	@Override
	public FlatHttpResponse copy() {
		return replace(this.content.copy());
	}

	@Override
	public FlatHttpResponse duplicate() {
		return replace(this.content.duplicate());
	}

	@Override
	public FlatHttpResponse retainedDuplicate() {
		return replace(this.content.retainedDuplicate());
	}

	@Override
	public FlatHttpResponse replace(ByteBuf content) {
		return new FlatHttpResponse(this.version, this.status, this.headers.copy(), content);
	}

	@Override
	public FlatHttpResponse retain() {
		this.content.retain();
		return this;
	}

	@Override
	public FlatHttpResponse retain(int increment) {
		this.content.retain(increment);
		return this;
	}

	@Override
	public FlatHttpResponse touch() {
		this.content.touch();
		return this;
	}

	@Override
	public FlatHttpResponse touch(Object hint) {
		this.content.touch(hint);
		return this;
	}

	@Override
	@Deprecated
	public HttpResponseStatus getStatus() {
		return this.status;
	}

	@Override
	public HttpResponseStatus status() {
		return this.status;
	}

	@Override
	public FlatHttpResponse setStatus(HttpResponseStatus status) {
		this.status = status;
		return this;
	}

	@Override
	public FlatHttpResponse setProtocolVersion(HttpVersion version) {
		this.version = version;
		return this;
	}
}

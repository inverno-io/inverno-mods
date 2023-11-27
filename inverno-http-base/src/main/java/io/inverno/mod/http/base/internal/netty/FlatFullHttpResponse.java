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
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * <p>
 * Optimized {@link FullHttpResponse} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class FlatFullHttpResponse extends FlatHttpResponse implements FullHttpResponse {

	private final HttpHeaders trailingHeaders;

	/**
	 * <p>
	 * Creates a flat full HTTP response.
	 * </p>
	 * 
	 * @param version         the HTTP version
	 * @param status          the HTTP response status
	 * @param headers         the HTTP headers
	 * @param content         the response content
	 * @param trailingHeaders the trailing HTTP headers
	 */
	public FlatFullHttpResponse(HttpVersion version, HttpResponseStatus status, HttpHeaders headers, ByteBuf content, HttpHeaders trailingHeaders) {
		super(version, status, headers, content);
		this.trailingHeaders = trailingHeaders;
	}

	@Override
	public HttpHeaders trailingHeaders() {
		return this.trailingHeaders;
	}

	@Override
	public FlatFullHttpResponse copy() {
		return this.replace(this.content.copy());
	}

	@Override
	public FlatFullHttpResponse duplicate() {
		return this.replace(this.content.duplicate());
	}

	@Override
	public FlatFullHttpResponse retainedDuplicate() {
		return this.replace(this.content.retainedDuplicate());
	}

	@Override
	public FlatFullHttpResponse replace(ByteBuf content) {
		return new FlatFullHttpResponse(this.version, this.status, this.headers.copy(), content, this.trailingHeaders);
	}

	@Override
	public FlatFullHttpResponse retain(int increment) {
		super.retain(increment);
		return this;
	}

	@Override
	public FlatFullHttpResponse retain() {
		super.retain();
		return this;
	}

	@Override
	public FlatFullHttpResponse touch() {
		super.touch();
		return this;
	}

	@Override
	public FlatFullHttpResponse touch(Object hint) {
		super.touch(hint);
		return this;
	}

	@Override
	public FlatFullHttpResponse setProtocolVersion(HttpVersion version) {
		super.setProtocolVersion(version);
		return this;
	}

	@Override
	public FlatFullHttpResponse setStatus(HttpResponseStatus status) {
		super.setStatus(status);
		return this;
	}
}

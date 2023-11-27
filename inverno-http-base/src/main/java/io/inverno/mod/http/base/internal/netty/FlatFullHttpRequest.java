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

package io.inverno.mod.http.base.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * <p>
 * Optimized {@link FullHttpRequest} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class FlatFullHttpRequest extends FlatHttpRequest implements FullHttpRequest {

	private final HttpHeaders trailingHeaders;
	
	/**
	 * <p>
	 * Creates a flat full HTTP request.
	 * </p>
	 * 
	 * @param version         the HTTP version
	 * @param method          the HTTP method
	 * @param uri             the request URI
	 * @param headers         the HTTP headers
	 * @param content         the request content
	 * @param trailingHeaders the trailing HTTPheaders
	 */
	public FlatFullHttpRequest(HttpVersion version, HttpMethod method, String uri, HttpHeaders headers, ByteBuf content, HttpHeaders trailingHeaders) {
		super(version, method, uri, headers, content);
		this.trailingHeaders = trailingHeaders;
	}
	
	@Override
	public FlatFullHttpRequest copy() {
		return this.replace(this.content.copy());
	}

	@Override
	public FlatFullHttpRequest duplicate() {
		return this.replace(this.content.duplicate());
	}

	@Override
	public FlatFullHttpRequest retainedDuplicate() {
		return this.replace(this.content.retainedDuplicate());
	}

	@Override
	public FlatFullHttpRequest replace(ByteBuf content) {
		return new FlatFullHttpRequest(this.version, this.method, this.uri, this.headers.copy(), content, this.trailingHeaders);
	}

	@Override
	public FlatFullHttpRequest retain(int increment) {
		super.retain(increment);
		return this;
	}

	@Override
	public FlatFullHttpRequest retain() {
		super.retain();
		return this;
	}

	@Override
	public FlatFullHttpRequest touch() {
		super.touch();
		return this;
	}

	@Override
	public FlatFullHttpRequest touch(Object hint) {
		super.touch(hint);
		return this;
	}

	@Override
	public FlatFullHttpRequest setProtocolVersion(HttpVersion version) {
		super.setProtocolVersion(version);
		return this;
	}

	@Override
	public FlatFullHttpRequest setMethod(HttpMethod method) {
		super.setMethod(method);
		return this;
	}

	@Override
	public FlatFullHttpRequest setUri(String uri) {
		super.setUri(uri);
		return this;
	}

	@Override
	public HttpHeaders trailingHeaders() {
		return this.trailingHeaders;
	}
}

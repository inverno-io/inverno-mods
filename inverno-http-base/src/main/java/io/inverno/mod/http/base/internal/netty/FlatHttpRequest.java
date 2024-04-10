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
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;

/**
 * <p>
 * Optimized {@link HttpResponse} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class FlatHttpRequest implements HttpRequest, HttpContent {

	protected HttpMethod method;
	protected String uri;
	protected HttpVersion version;
	protected HttpHeaders headers;
	protected final ByteBuf content;
	
	protected DecoderResult result = DecoderResult.SUCCESS;
	
	/**
	 * <p>
	 * Creates a flat HTTP request.
	 * </p>
	 * 
	 * @param content the request content
	 */
	public FlatHttpRequest(ByteBuf content) {
		this.content = content;
	}
	
	/**
	 * <p>
	 * Creates a flat HTTP request.
	 * </p>
	 * 
	 * @param version the HTTP version
	 * @param method  the HTTP method
	 * @param uri     the request URI
	 * @param headers the HTTP headers
	 */
	public FlatHttpRequest(HttpVersion version, HttpMethod method, String uri, HttpHeaders headers) {
		this(version, method, uri, headers, Unpooled.EMPTY_BUFFER);
	}

	/**
	 * <p>
	 * Creates a flat HTTP request.
	 * </p>
	 * 
	 * @param version the HTTP version
	 * @param method  the HTTP method
	 * @param uri     the request URI
	 * @param headers the HTTP headers
	 * @param content the request content
	 */
	public FlatHttpRequest(HttpVersion version, HttpMethod method, String uri, HttpHeaders headers, ByteBuf content) {
		this.method = method;
		this.uri = uri;
		this.version = version;
		this.headers = headers;
		this.content = content;
	}
	
	@Override
	@Deprecated
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public HttpMethod method() {
		return this.method;
	}

	@Override
	public HttpRequest setMethod(HttpMethod method) {
		this.method = method;
		return this;
	}

	@Override
	@Deprecated
	public String getUri() {
		return this.uri;
	}

	@Override
	public String uri() {
		return this.uri;
	}

	@Override
	public HttpRequest setUri(String uri) {
		this.uri = uri;
		return this;
	}

	@Override
	public HttpRequest setProtocolVersion(HttpVersion version) {
		this.version = version;
		return this;
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
	public FlatHttpRequest copy() {
		return replace(this.content.copy());
	}

	@Override
	public FlatHttpRequest duplicate() {
		return replace(this.content.duplicate());
	}

	@Override
	public FlatHttpRequest retainedDuplicate() {
		return replace(this.content.retainedDuplicate());
	}

	@Override
	public FlatHttpRequest replace(ByteBuf content) {
		return new FlatHttpRequest(this.version, this.method, this.uri, this.headers.copy(), content);
	}

	@Override
	public FlatHttpRequest retain() {
		this.content.retain();
		return this;
	}

	@Override
	public FlatHttpRequest retain(int increment) {
		this.content.retain(increment);
		return this;
	}

	@Override
	public FlatHttpRequest touch() {
		this.content.touch();
		return this;
	}

	@Override
	public FlatHttpRequest touch(Object hint) {
		this.content.touch(hint);
		return this;
	}
}

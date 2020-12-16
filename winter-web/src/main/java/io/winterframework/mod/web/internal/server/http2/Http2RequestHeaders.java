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
package io.winterframework.mod.web.internal.server.http2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.netty.handler.codec.http2.Http2Headers;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestHeaders;

/**
 * @author jkuhn
 *
 */
public class Http2RequestHeaders implements RequestHeaders {

	private HeaderService HeaderService;
	
	private Http2Headers httpHeaders;
	
	public Http2RequestHeaders(HeaderService HeaderService, Http2Headers headers) {
		this.HeaderService = HeaderService;
		this.httpHeaders = headers;
	}
	
	private String getHeaderValue(String name) {
		CharSequence header = this.httpHeaders.get(name);
		return header != null ? header.toString() : null;
	}

	@Override
	public String getAuthority() {
		return this.getHeaderValue(Headers.PSEUDO_AUTHORITY);
	}

	@Override
	public String getPath() {
		return this.getHeaderValue(Headers.PSEUDO_PATH);
	}

	@Override
	public Method getMethod() {
		return Method.valueOf(this.getHeaderValue(Headers.PSEUDO_METHOD));
	}

	@Override
	public String getScheme() {
		return this.getHeaderValue(Headers.PSEUDO_SCHEME);
	}

	@Override
	public String getContentType() {
		return this.getHeaderValue(Headers.CONTENT_TYPE);
	}

	@Override
	public Long getSize() {
		return this.httpHeaders.getLong(Headers.CONTENT_LENGTH);
	}
	
	@Override
	public Set<String> getNames() {
		return this.httpHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public <T extends Header> Optional<T> getHeader(String name) {
		return this.<T>getAllHeader(name).stream().findFirst();
	}
	
	@Override
	public <T extends Header> List<T> getAllHeader(String name) {
		return this.httpHeaders.getAll(name).stream().map(value -> this.HeaderService.<T>decode(name, value.toString())).collect(Collectors.toList());
	}
	
	@Override
	public Map<String, List<Header>> getAllHeader() {
		// TODO optimize see Http1xRequestHeader
		return this.httpHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toMap(Function.identity(), this::<Header>getAllHeader));
	}
	
	@Override
	public boolean contains(String name, String value) {
		return this.httpHeaders.contains(name, value);
	}
}

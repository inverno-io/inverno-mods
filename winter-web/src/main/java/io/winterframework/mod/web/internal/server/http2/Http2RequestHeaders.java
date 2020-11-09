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

import java.nio.charset.Charset;
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
import io.winterframework.mod.web.internal.Charsets;

/**
 * @author jkuhn
 *
 */
public class Http2RequestHeaders implements RequestHeaders {

	private HeaderService HeaderService;
	
	private Http2Headers headers;
	
	public Http2RequestHeaders(HeaderService HeaderService, Http2Headers headers) {
		this.HeaderService = HeaderService;
		this.headers = headers;
	}
	
	private String getHeader(String name) {
		CharSequence header = this.headers.get(name);
		return header != null ? header.toString() : null;
	}

	@Override
	public String getAuthority() {
		return this.getHeader(Headers.PSEUDO_AUTHORITY);
	}

	@Override
	public String getPath() {
		return this.getHeader(Headers.PSEUDO_PATH);
	}

	@Override
	public Method getMethod() {
		return Method.valueOf(this.getHeader(Headers.PSEUDO_METHOD));
	}

	@Override
	public String getScheme() {
		return this.getHeader(Headers.PSEUDO_SCHEME);
	}

	@Override
	public String getContentType() {
		return this.getHeader(Headers.CONTENT_TYPE);
	}

	@Override
	public Charset getCharset() {
		return this.<Headers.ContentType>get(Headers.CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
	}

	@Override
	public Long getSize() {
		return this.headers.getLong(Headers.CONTENT_LENGTH);
	}
	
	@Override
	public Set<String> getNames() {
		return this.headers.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public <T extends Header> Optional<T> get(String name) {
		return this.<T>getAll(name).stream().findFirst();
	}
	
	@Override
	public <T extends Header> List<T> getAll(String name) {
		return this.headers.getAll(name).stream().map(value -> this.HeaderService.<T>decode(name, value.toString())).collect(Collectors.toList());
	}
	
	@Override
	public Map<String, List<? extends Header>> getAll() {
		return this.headers.names().stream().map(CharSequence::toString).collect(Collectors.toMap(Function.identity(), this::<Header>getAll));
	}
}

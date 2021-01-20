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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.handler.codec.http2.Http2Headers;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.internal.server.GenericParameter;
import io.winterframework.mod.web.server.RequestHeaders;

/**
 * @author jkuhn
 *
 */
public class Http2RequestHeaders implements RequestHeaders {

	private final Http2Headers internalHeaders;
	
	private final HeaderService headerService;
	
	private final ObjectConverter<String> parameterConverter;
	
	public Http2RequestHeaders(Http2Headers headers, HeaderService HeaderService, ObjectConverter<String> parameterConverter) {
		this.internalHeaders = headers;
		this.headerService = HeaderService;
		this.parameterConverter = parameterConverter;
	}
	
	Http2Headers getHttpHeaders() {
		return this.internalHeaders;
	}
	
	private String getHeaderValue(String name) {
		CharSequence header = this.internalHeaders.get(name);
		return header != null ? header.toString() : null;
	}

	@Override
	public String getAuthority() {
		return this.getHeaderValue(Headers.NAME_PSEUDO_AUTHORITY);
	}

	@Override
	public String getPath() {
		return this.getHeaderValue(Headers.NAME_PSEUDO_PATH);
	}

	@Override
	public Method getMethod() {
		return Method.valueOf(this.getHeaderValue(Headers.NAME_PSEUDO_METHOD));
	}

	@Override
	public String getScheme() {
		return this.getHeaderValue(Headers.NAME_PSEUDO_SCHEME);
	}

	@Override
	public String getContentType() {
		return this.getHeaderValue(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public Long getContentLength() {
		return this.internalHeaders.getLong(Headers.NAME_CONTENT_LENGTH);
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.internalHeaders.contains(name, value, true);
	}
	
	@Override
	public Set<String> getNames() {
		return this.internalHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}
	
	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.internalHeaders.get(name)).map(Object::toString);
	}
	
	@Override
	public List<String> getAll(CharSequence name) {
		return this.internalHeaders.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}
	
	@Override
	public List<Entry<String, String>> getAll() {
		List<Entry<String, String>> result = new LinkedList<>();
		this.internalHeaders.forEach(e -> {
			result.add(Map.entry(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}

	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.decode(name.toString(), value));
	}
	
	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.internalHeaders.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value.toString())).collect(Collectors.toList());
	}
	
	@Override
	public List<Header> getAllHeader() {
		List<Header> result = new LinkedList<>();
		this.internalHeaders.forEach(e -> {
			result.add(this.headerService.<Header>decode(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(this.parameterConverter, name.toString(), value));
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.internalHeaders.getAll(name).stream().map(value -> new GenericParameter(this.parameterConverter, name.toString(), value.toString())).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		List<Parameter> result = new LinkedList<>();
		this.internalHeaders.forEach(e -> {
			result.add(new GenericParameter(this.parameterConverter, e.getValue().toString(), e.getValue().toString()));
		});
		return result;
	}
}

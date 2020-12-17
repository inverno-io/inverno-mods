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

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Headers.PseudoHeaderName;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.ContentType;
import io.winterframework.mod.web.Status;
import io.winterframework.mod.web.internal.server.AbstractResponseHeaders;

/**
 * @author jkuhn
 *
 */
public class Http2ResponseHeaders implements AbstractResponseHeaders {

	private final HeaderService headerService;
	
	private final Http2Headers httpHeaders;
	
	private boolean written;
	
	public Http2ResponseHeaders(HeaderService headerService) {
		this.headerService = headerService;
		this.httpHeaders = new DefaultHttp2Headers();
		this.httpHeaders.set(PseudoHeaderName.STATUS.value(), "200");
	}
	
	Http2Headers getHttpHeaders() {
		return this.httpHeaders;
	}
	
	private void requireNonWritten() {
		if(this.written) {
			throw new IllegalStateException("Headers have been already written");
		}
	}

	@Override
	public Http2ResponseHeaders status(Status status) {
		return this.status(status.getCode());
	}

	@Override
	public Http2ResponseHeaders status(int status) {
		this.requireNonWritten();
		this.httpHeaders.setInt(PseudoHeaderName.STATUS.value(), status);
		return this;
	}

	@Override
	public Http2ResponseHeaders contentType(String contentType) {
		this.requireNonWritten();
		this.httpHeaders.set(Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}

	@Override
	public Http2ResponseHeaders size(long size) {
		this.requireNonWritten();
		this.httpHeaders.setLong(Headers.NAME_CONTENT_LENGTH, size);
		return this;
	}

	@Override
	public Http2ResponseHeaders add(String name, String value) {
		this.requireNonWritten();
		this.httpHeaders.add(name, value);
		return this;
	}

	@Override
	public Http2ResponseHeaders add(CharSequence name, CharSequence value) {
		this.requireNonWritten();
		this.httpHeaders.add(name, value);
		return this;
	}

	@Override
	public Http2ResponseHeaders add(Header... headers) {
		for(Header header : headers) {
			this.httpHeaders.add(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}

	@Override
	public boolean isWritten() {
		return this.written;
	}

	@Override
	public void setWritten(boolean written) {
		this.written = written;
	}

	@Override
	public Optional<ContentType> getContentType() {
		return this.get(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public String getContentTypeString() {
		return this.getString(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public CharSequence getContentTypeCharSequence() {
		return this.getCharSequence(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public <T extends Header> Optional<T> get(String name) {
		return Optional.ofNullable(this.getString(name)).map(value -> this.headerService.decode(name, value));
	}

	@Override
	public String getString(String name) {
		CharSequence value = this.httpHeaders.get(name);
		return value != null ? value.toString() : null;
	}

	@Override
	public CharSequence getCharSequence(String name) {
		return this.httpHeaders.get(name);
	}

	@Override
	public <T extends Header> List<T> getAll(String name) {
		return this.getAllString(name).stream().map(value -> this.headerService.<T>decode(name, value)).collect(Collectors.toList());
	}

	@Override
	public List<String> getAllString(String name) {
		return this.httpHeaders.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}

	@Override
	public List<CharSequence> getAllCharSequence(String name) {
		return this.httpHeaders.getAll(name);
	}

	@Override
	public List<Header> getAll() {
		return this.getAllString().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	@Override
	public List<Entry<String, String>> getAllString() {
		List<Entry<String, String>> result = new LinkedList<>();
		this.httpHeaders.forEach(e -> {
			result.add(Map.entry(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}

	@Override
	public List<Entry<CharSequence, CharSequence>> getAllCharSequence() {
		List<Entry<CharSequence, CharSequence>> result = new LinkedList<>();
		this.httpHeaders.forEach(e -> {
			result.add(Map.entry(e.getKey(), e.getValue()));
		});
		return result;
	}

	@Override
	public Set<String> getNames() {
		return this.httpHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Long getSize() {
		return this.httpHeaders.getLong(Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public int getStatus() {
		return this.httpHeaders.getInt(PseudoHeaderName.STATUS.value());
	}
}

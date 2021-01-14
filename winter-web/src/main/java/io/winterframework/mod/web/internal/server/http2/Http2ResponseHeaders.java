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
import io.winterframework.mod.web.Status;
import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.internal.server.AbstractResponseHeaders;
import io.winterframework.mod.web.server.ResponseHeaders;

/**
 * @author jkuhn
 *
 */
public class Http2ResponseHeaders implements AbstractResponseHeaders {

	private final HeaderService headerService;
	
	private final Http2Headers internalHeaders;
	
	private boolean written;
	
	public Http2ResponseHeaders(HeaderService headerService) {
		this.headerService = headerService;
		this.internalHeaders = new DefaultHttp2Headers();
		this.internalHeaders.set(PseudoHeaderName.STATUS.value(), "200");
	}
	
	Http2Headers getInternalHeaders() {
		return this.internalHeaders;
	}

	@Override
	public Http2ResponseHeaders status(Status status) {
		return this.status(status.getCode());
	}

	@Override
	public Http2ResponseHeaders status(int status) {
		this.internalHeaders.setInt(PseudoHeaderName.STATUS.value(), status);
		return this;
	}

	@Override
	public Http2ResponseHeaders contentType(String contentType) {
		this.internalHeaders.set(Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}

	@Override
	public Http2ResponseHeaders contentLength(long contentLength) {
		this.internalHeaders.setLong(Headers.NAME_CONTENT_LENGTH, contentLength);
		return this;
	}

	@Override
	public Http2ResponseHeaders add(CharSequence name, CharSequence value) {
		this.internalHeaders.add(name, value);
		return this;
	}

	@Override
	public Http2ResponseHeaders add(Header... headers) {
		for(Header header : headers) {
			this.internalHeaders.add(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseHeaders set(CharSequence name, CharSequence value) {
		this.internalHeaders.set(name, value);
		return this;
	}
	
	@Override
	public ResponseHeaders set(Header... headers) {
		for(Header header : headers) {
			this.internalHeaders.set(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}
	
	@Override
	public ResponseHeaders remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.internalHeaders.remove(name);
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
	public Optional<String> getContentType() {
		return Optional.ofNullable(this.internalHeaders.get(Headers.NAME_CONTENT_TYPE).toString());
	}

	@Override
	public Optional<Headers.ContentType> getContentTypeHeader() {
		return this.getHeader(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public CharSequence getContentTypeCharSequence() {
		return this.internalHeaders.get(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.internalHeaders.get(name)).map(Object::toString);
	}
	
	@Override
	public CharSequence getCharSequence(CharSequence name) {
		return this.internalHeaders.get(name);
	}
	
	@Override
	public List<String> getAll(CharSequence name) {
		return this.internalHeaders.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}
	
	@Override
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.internalHeaders.getAll(name);
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
	public List<Entry<CharSequence, CharSequence>> getAllCharSequence() {
		List<Entry<CharSequence, CharSequence>> result = new LinkedList<>();
		this.internalHeaders.forEach(e -> {
			result.add(Map.entry(e.getKey(), e.getValue()));
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
	public Set<String> getNames() {
		return this.internalHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Long getContentLength() {
		return this.internalHeaders.getLong(Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public int getStatus() {
		return this.internalHeaders.getInt(PseudoHeaderName.STATUS.value());
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.internalHeaders.contains(name, value, true);
	}
}

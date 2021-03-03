/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.web.internal.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.Status;
import io.winterframework.mod.http.base.header.Header;
import io.winterframework.mod.http.base.header.HeaderService;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.ResponseHeaders;

/**
 * @author jkuhn
 *
 */
public class MockResponseHeaders implements ResponseHeaders {

	private final HeaderService headerService;
	
	private int status;
	
	private final Map<String, List<String>> headers;
	
	public MockResponseHeaders(HeaderService headerService) {
		this.headerService = headerService;
		this.headers = new HashMap<>();
	}

	@Override
	public ResponseHeaders status(Status status) {
		this.status = status.getCode();
		return this;
	}

	@Override
	public ResponseHeaders status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public ResponseHeaders contentType(String contentType) {
		this.set(Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}

	@Override
	public ResponseHeaders contentLength(long length) {
		this.set(Headers.NAME_CONTENT_LENGTH, Long.toString(length));
		return this;
	}

	@Override
	public ResponseHeaders add(CharSequence name, CharSequence value) {
		if(!this.headers.containsKey(name.toString())) {
			this.headers.put(name.toString(), new ArrayList<>());
		}
		this.headers.get(name.toString()).add(value.toString());
		return this;
	}

	@Override
	public ResponseHeaders add(Header... headers) {
		for(Header header : headers) {
			this.add(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseHeaders set(CharSequence name, CharSequence value) {
		this.remove(name);
		this.add(name, value);
		return this;
	}

	@Override
	public ResponseHeaders set(Header... headers) {
		for(Header header : headers) {
			this.set(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseHeaders remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.headers.remove(name.toString());
		}
		return this;
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public Optional<String> getContentType() {
		return Optional.ofNullable(this.headers.get(Headers.NAME_CONTENT_TYPE)).map(l -> l.get(0));
	}

	@Override
	public Optional<Headers.ContentType> getContentTypeHeader() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(CharSequence name) {
		return this.headers.containsKey(name.toString());
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.headers.containsKey(name.toString()) ? this.headers.get(name.toString()).contains(value.toString()) : false;
	}

	@Override
	public Set<String> getNames() {
		return this.headers.keySet();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name.toString())).map(l -> l.get(0));
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name.toString())).orElse(List.of());
	}

	@Override
	public List<Entry<String, String>> getAll() {
		return this.headers.entrySet().stream().flatMap(e -> e.getValue().stream().map(value -> Map.entry(e.getKey(), value))).collect(Collectors.toList());
	}

	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.decode(name.toString(), value));
	}

	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		return this.getAll().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name.toString()).map(value -> new MockParameter(name.toString(), value));
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.getAll(name.toString()).stream().map(value -> new MockParameter(name.toString(), value)).collect(Collectors.toList());
	}

	@Override
	public List<Parameter> getAllParameter() {
		return this.getAll().stream().map(e -> new MockParameter(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

}

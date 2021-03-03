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
package io.winterframework.mod.http.server.internal.http1x;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpHeaders;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.Status;
import io.winterframework.mod.http.base.header.Header;
import io.winterframework.mod.http.base.header.HeaderService;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.base.internal.GenericParameter;
import io.winterframework.mod.http.server.ResponseHeaders;
import io.winterframework.mod.http.server.internal.AbstractResponseHeaders;
import io.winterframework.mod.http.server.internal.netty.LinkedHttpHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xResponseHeaders implements AbstractResponseHeaders {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final LinkedHttpHeaders internalHeaders;
	
	private int status = 200;
	
	private boolean written;
	
	public Http1xResponseHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.internalHeaders = new LinkedHttpHeaders();
	}
	
	HttpHeaders getInternalHeaders() {
		return this.internalHeaders;
	}

	@Override
	public Http1xResponseHeaders status(Status status) {
		return this.status(status.getCode());
	}

	@Override
	public Http1xResponseHeaders status(int status) {
		this.status = status;
		return this;
	}

	@Override
	public Http1xResponseHeaders contentType(String contentType) {
		this.internalHeaders.set((CharSequence)Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}
	
	@Override
	public Http1xResponseHeaders contentLength(long contentLength) {
		this.internalHeaders.setLong((CharSequence)Headers.NAME_CONTENT_LENGTH, contentLength);
		return this;
	}
	
	@Override
	public Http1xResponseHeaders add(CharSequence name, CharSequence value) {
		this.internalHeaders.addCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseHeaders add(Header... headers) {
		for(Header header : headers) {
			this.internalHeaders.addCharSequence(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}
	
	@Override
	public ResponseHeaders set(CharSequence name, CharSequence value) {
		this.internalHeaders.setCharSequence(name, value);
		return this;
	}
	
	@Override
	public ResponseHeaders set(Header... headers) {
		for(Header header : headers) {
			this.internalHeaders.setCharSequence(header.getHeaderName(), header.getHeaderValue());
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
		return Optional.ofNullable(this.internalHeaders.get((CharSequence)Headers.NAME_CONTENT_TYPE));
	}
	
	@Override
	public Optional<Headers.ContentType> getContentTypeHeader() {
		return this.getHeader(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public CharSequence getContentTypeCharSequence() {
		return this.internalHeaders.getCharSequence((CharSequence)Headers.NAME_CONTENT_TYPE);
	}
	
	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.<T>decode(name.toString(), value));
	}
	
	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.internalHeaders.get(name));
	}
	
	@Override
	public CharSequence getCharSequence(CharSequence name) {
		return this.internalHeaders.getCharSequence(name);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.internalHeaders.getAll(name);
	}
	
	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
	}

	@Override
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.internalHeaders.getAllCharSequence(name);
	}
	
	@Override
	public List<Map.Entry<String, String>> getAll() {
		return this.internalHeaders.entries();
	}
	
	@Override
	public List<Header> getAllHeader() {
		return this.getAll().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence() {
		return this.internalHeaders.entriesCharSequence();
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(this.parameterConverter, name.toString(), value));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.internalHeaders.getAll(name).stream().map(value -> new GenericParameter(this.parameterConverter, name.toString(), value)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		return this.internalHeaders.entries().stream().map(e -> new GenericParameter(this.parameterConverter, e.getKey(), e.getValue())).collect(Collectors.toList());
	}
	
	@Override
	public Set<String> getNames() {
		return this.internalHeaders.names();
	}

	@Override
	public Long getContentLength() {
		return this.internalHeaders.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public boolean contains(CharSequence name) {
		return this.internalHeaders.contains(name);
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.internalHeaders.contains(name, value, true);
	}
}

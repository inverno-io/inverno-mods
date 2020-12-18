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
package io.winterframework.mod.web.internal.server.http1x;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpHeaders;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.ContentType;
import io.winterframework.mod.web.Status;
import io.winterframework.mod.web.internal.netty.LinkedHttpHeaders;
import io.winterframework.mod.web.internal.server.AbstractResponseHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xResponseHeaders implements AbstractResponseHeaders {

	private final LinkedHttpHeaders internalHeaders;
	
	private final HeaderService headerService;
	
	private int status = 200;
	
	private boolean written;
	
	public Http1xResponseHeaders(HeaderService headerService) {
		this.headerService = headerService;
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
		this.internalHeaders.set((CharSequence)Headers.NAME_CONTENT_LENGTH, contentType);
		return this;
	}
	
	@Override
	public Http1xResponseHeaders contentLength(long contentLength) {
		this.internalHeaders.setLong((CharSequence)Headers.NAME_CONTENT_LENGTH, contentLength);
		return this;
	}

	@Override
	public Http1xResponseHeaders add(String name, String value) {
		this.internalHeaders.addCharSequence((CharSequence)name, (CharSequence)value);
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
			this.internalHeaders.addCharSequence((CharSequence)header.getHeaderName(), (CharSequence)header.getHeaderValue());
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
		return this.internalHeaders.get((CharSequence)Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public CharSequence getContentTypeCharSequence() {
		return this.internalHeaders.getCharSequence((CharSequence)Headers.NAME_CONTENT_TYPE);
	}
	
	@Override
	public <T extends Header> Optional<T> get(String name) {
		return Optional.ofNullable(this.getString(name)).map(value -> this.headerService.decode(name, value));
	}
	
	@Override
	public String getString(String name) {
		return this.internalHeaders.get((CharSequence)name);
	}
	
	@Override
	public CharSequence getCharSequence(String name) {
		return this.internalHeaders.getCharSequence((CharSequence)name);
	}

	@Override
	public <T extends Header> List<T> getAll(String name) {
		return this.getAllString(name).stream().map(value -> this.headerService.<T>decode(name, value)).collect(Collectors.toList());
	}

	@Override
	public List<String> getAllString(String name) {
		return this.internalHeaders.getAll((CharSequence)name);
	}
	
	@Override
	public List<CharSequence> getAllCharSequence(String name) {
		return this.internalHeaders.getAllCharSequence((CharSequence)name);
	}
	
	@Override
	public List<Header> getAll() {
		return this.getAllString().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<String, String>> getAllString() {
		return this.internalHeaders.entries();
	}
	
	@Override
	public List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence() {
		return this.internalHeaders.entriesCharSequence();
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
}

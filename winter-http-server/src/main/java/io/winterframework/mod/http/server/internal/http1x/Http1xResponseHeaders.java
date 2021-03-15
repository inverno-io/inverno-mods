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

import io.netty.handler.codec.http.DefaultHttpHeaders;
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
 * <p>
 * HTTP1.x {@link ResponseHeaders} implementation.
 * </p>
 * 
 * <p>
 * This implementation uses {@link LinkedHttpHeaders} instead of Netty's
 * {@link DefaultHttpHeaders} as internal headers in order to increase
 * performances.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractResponseHeaders
 */
public class Http1xResponseHeaders implements AbstractResponseHeaders {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final LinkedHttpHeaders underlyingHeaders;
	
	private int statusCode = 200;
	
	private boolean written;
	
	/**
	 * <p>
	 * Creates HTTP1.x server response headers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter a string object converter
	 */
	public Http1xResponseHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingHeaders = new LinkedHttpHeaders();
	}

	/**
	 * <p>
	 * Returns the underlying headers.
	 * </p>
	 * 
	 * @return the underlying headers
	 */
	LinkedHttpHeaders getUnderlyingHeaders() {
		return this.underlyingHeaders;
	}

	@Override
	public Http1xResponseHeaders status(Status status) {
		return this.status(status.getCode());
	}

	@Override
	public Http1xResponseHeaders status(int status) {
		this.statusCode = status;
		return this;
	}

	@Override
	public Http1xResponseHeaders contentType(String contentType) {
		this.underlyingHeaders.set((CharSequence)Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}
	
	@Override
	public Http1xResponseHeaders contentLength(long contentLength) {
		this.underlyingHeaders.setLong((CharSequence)Headers.NAME_CONTENT_LENGTH, contentLength);
		return this;
	}
	
	@Override
	public Http1xResponseHeaders add(CharSequence name, CharSequence value) {
		this.underlyingHeaders.addCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseHeaders add(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.addCharSequence(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}
	
	@Override
	public ResponseHeaders set(CharSequence name, CharSequence value) {
		this.underlyingHeaders.setCharSequence(name, value);
		return this;
	}
	
	@Override
	public ResponseHeaders set(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.setCharSequence(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}
	
	@Override
	public ResponseHeaders remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.underlyingHeaders.remove(name);
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
		return Optional.ofNullable(this.underlyingHeaders.get((CharSequence)Headers.NAME_CONTENT_TYPE));
	}
	
	@Override
	public Optional<Headers.ContentType> getContentTypeHeader() {
		return this.getHeader(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public CharSequence getContentTypeCharSequence() {
		return this.underlyingHeaders.getCharSequence((CharSequence)Headers.NAME_CONTENT_TYPE);
	}
	
	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.<T>decode(name.toString(), value));
	}
	
	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.underlyingHeaders.get(name));
	}
	
	@Override
	public CharSequence getCharSequence(CharSequence name) {
		return this.underlyingHeaders.getCharSequence(name);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingHeaders.getAll(name);
	}
	
	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
	}

	@Override
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.underlyingHeaders.getAllCharSequence(name);
	}
	
	@Override
	public List<Map.Entry<String, String>> getAll() {
		return this.underlyingHeaders.entries();
	}
	
	@Override
	public List<Header> getAllHeader() {
		return this.getAll().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence() {
		return this.underlyingHeaders.entriesCharSequence();
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.underlyingHeaders.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value, this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		return this.underlyingHeaders.entries().stream().map(e -> new GenericParameter(e.getKey(), e.getValue(), this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public Set<String> getNames() {
		return this.underlyingHeaders.names();
	}

	@Override
	public Long getContentLength() {
		return this.underlyingHeaders.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public Status getStatus() {
		return Status.valueOf(this.statusCode);
	}
	
	@Override
	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public boolean contains(CharSequence name) {
		return this.underlyingHeaders.contains(name);
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.underlyingHeaders.contains(name, value, true);
	}
}

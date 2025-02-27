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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.server.internal.AbstractResponseHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Http/1.x {@link OutboundResponseHeaders} implementation.
 * </p>
 * 
 * <p>
 * This implementation uses {@link LinkedHttpHeaders} instead of Netty's {@link DefaultHttpHeaders} as internal headers in order to increase performances.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http1xResponseHeaders extends AbstractResponseHeaders<Http1xResponseHeaders> {

	private final LinkedHttpHeaders headers;

	private int statusCode = 200;
	
	/**
	 * <p>
	 * Creates Http/1.x response headers.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headersValidator   the headers validator or null
	 */
	public Http1xResponseHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, HeadersValidator headersValidator) {
		super(headerService, parameterConverter);
		this.headers = new LinkedHttpHeaders(headersValidator);
	}

	/**
	 * <p>
	 * Returns the headers to send as part of the HTTP response.
	 * </p>
	 * 
	 * @return the wrapped headers
	 */
	LinkedHttpHeaders unwrap() {
		return this.headers;
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
	public Status getStatus() throws IllegalArgumentException {
		return Status.valueOf(this.statusCode);
	}

	@Override
	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public Http1xResponseHeaders contentType(String contentType) {
		this.headers.set((CharSequence)Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}
	
	@Override
	public String getContentType() {
		return this.headers.get((CharSequence)Headers.NAME_CONTENT_TYPE);
	}
	
	@Override
	public Headers.ContentType getContentTypeHeader() {
		return this.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
	}

	@Override
	public Http1xResponseHeaders contentLength(long contentLength) {
		this.headers.set((CharSequence)Headers.NAME_CONTENT_LENGTH, Long.toString(contentLength));
		return this;
	}
	
	@Override
	public Long getContentLength() {
		return this.headers.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}
	
	@Override
	public Http1xResponseHeaders add(CharSequence name, CharSequence value) {
		this.headers.addCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseHeaders add(List<? extends Header> headers) {
		for(Header header : headers) {
			this.headers.addCharSequence(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http1xResponseHeaders set(CharSequence name, CharSequence value) {
		this.headers.setCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseHeaders set(List<? extends Header> headers) {
		for(Header header : headers) {
			this.headers.setCharSequence(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http1xResponseHeaders remove(Set<? extends CharSequence> names) {
		for(CharSequence name : names) {
			this.headers.remove(name);
		}
		return this;
	}

	@Override
	public boolean contains(CharSequence name) {
		return this.headers.contains(name);
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.headers.contains(name, value, true);
	}

	@Override
	public Set<String> getNames() {
		return this.headers.names();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name));
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.headers.getAll(name);
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		return this.headers.entries();
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.headers.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value, this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		return this.headers.entries().stream().map(e -> new GenericParameter(e.getKey(), e.getValue(), this.parameterConverter)).collect(Collectors.toList());
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
}

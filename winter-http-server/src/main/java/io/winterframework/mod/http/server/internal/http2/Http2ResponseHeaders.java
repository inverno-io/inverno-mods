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
package io.winterframework.mod.http.server.internal.http2;

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
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.Status;
import io.winterframework.mod.http.base.header.Header;
import io.winterframework.mod.http.base.header.HeaderService;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.base.internal.GenericParameter;
import io.winterframework.mod.http.server.ResponseHeaders;
import io.winterframework.mod.http.server.internal.AbstractResponseHeaders;

/**
 * <p>
 * HTTP/2 {@link ResponseHeaders} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractResponseHeaders
 */
public class Http2ResponseHeaders implements AbstractResponseHeaders {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Http2Headers underlyingHeaders;
	
	private boolean written;
	
	/**
	 * <p>
	 * Creates HTTP/2 response headers.
	 * </p>
	 * 
	 * @param headerService the header service
	 * @param parameterConverter a string object converter 
	 */
	public Http2ResponseHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingHeaders = new DefaultHttp2Headers();
		this.underlyingHeaders.set(PseudoHeaderName.STATUS.value(), "200");
	}
	
	/**
	 * <p>
	 * Returns the underlying headers.
	 * </p>
	 * 
	 * @return the underlying headers
	 */
	Http2Headers getUnderlyingHeaders() {
		return this.underlyingHeaders;
	}

	@Override
	public Http2ResponseHeaders status(Status status) {
		return this.status(status.getCode());
	}

	@Override
	public Http2ResponseHeaders status(int status) {
		this.underlyingHeaders.setInt(PseudoHeaderName.STATUS.value(), status);
		return this;
	}

	@Override
	public Http2ResponseHeaders contentType(String contentType) {
		this.underlyingHeaders.set(Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}

	@Override
	public Http2ResponseHeaders contentLength(long contentLength) {
		this.underlyingHeaders.setLong(Headers.NAME_CONTENT_LENGTH, contentLength);
		return this;
	}

	@Override
	public Http2ResponseHeaders add(CharSequence name, CharSequence value) {
		this.underlyingHeaders.add(name, value);
		return this;
	}

	@Override
	public Http2ResponseHeaders add(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.add(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseHeaders set(CharSequence name, CharSequence value) {
		this.underlyingHeaders.set(name, value);
		return this;
	}
	
	@Override
	public ResponseHeaders set(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.set(header.getHeaderName(), header.getHeaderValue());
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
		return Optional.ofNullable(this.underlyingHeaders.get(Headers.NAME_CONTENT_TYPE).toString());
	}

	@Override
	public Optional<Headers.ContentType> getContentTypeHeader() {
		return this.getHeader(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public CharSequence getContentTypeCharSequence() {
		return this.underlyingHeaders.get(Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.underlyingHeaders.get(name)).map(Object::toString);
	}
	
	@Override
	public CharSequence getCharSequence(CharSequence name) {
		return this.underlyingHeaders.get(name);
	}
	
	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingHeaders.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}
	
	@Override
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.underlyingHeaders.getAll(name);
	}
	
	@Override
	public List<Entry<String, String>> getAll() {
		List<Entry<String, String>> result = new LinkedList<>();
		this.underlyingHeaders.forEach(e -> {
			result.add(Map.entry(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}
	
	@Override
	public List<Entry<CharSequence, CharSequence>> getAllCharSequence() {
		List<Entry<CharSequence, CharSequence>> result = new LinkedList<>();
		this.underlyingHeaders.forEach(e -> {
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
		return this.underlyingHeaders.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value.toString())).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		List<Header> result = new LinkedList<>();
		this.underlyingHeaders.forEach(e -> {
			result.add(this.headerService.<Header>decode(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.underlyingHeaders.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value.toString(), this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		List<Parameter> result = new LinkedList<>();
		this.underlyingHeaders.forEach(e -> {
			result.add(new GenericParameter(e.getKey().toString(), e.getValue().toString(), this.parameterConverter));
		});
		return result;
	}

	@Override
	public Set<String> getNames() {
		return this.underlyingHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Long getContentLength() {
		return this.underlyingHeaders.getLong(Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public Status getStatus() {
		return Status.valueOf(this.getStatusCode());
	}
	
	@Override
	public int getStatusCode() {
		return this.underlyingHeaders.getInt(PseudoHeaderName.STATUS.value());
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

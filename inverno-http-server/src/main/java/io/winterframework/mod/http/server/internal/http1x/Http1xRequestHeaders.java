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

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpRequest;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.server.RequestHeaders;
import io.inverno.mod.http.server.internal.netty.LinkedHttpHeaders;

/**
 * <p>
 * HTTP1.x {@link RequestHeaders} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class Http1xRequestHeaders implements RequestHeaders {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final LinkedHttpHeaders underlyingHeaders;
	
	/**
	 * <p>
	 * Creates HTTP1.x server request headers.
	 * </p>
	 * 
	 * @param httpRequest        the underlying HTTP request
	 * @param headerService      the header service
	 * @param parameterConverter a string object converter
	 */
	public Http1xRequestHeaders(HttpRequest httpRequest, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingHeaders = (LinkedHttpHeaders)httpRequest.headers();
	}

	/**
	 * <p>
	 * Returns the underlyinh headers.
	 * </p>
	 * 
	 * @return the underlyinh headers
	 */
	LinkedHttpHeaders getUnderlyingHeaders() {
		return this.underlyingHeaders;
	}

	@Override
	public String getContentType() {
		return this.underlyingHeaders.get((CharSequence)Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public Long getContentLength() {
		return this.underlyingHeaders.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public boolean contains(CharSequence name) {
		return this.underlyingHeaders.contains(name);
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.underlyingHeaders.contains(name, value, true);
	}
	
	@Override
	public Set<String> getNames() {
		return this.underlyingHeaders.names();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.underlyingHeaders.get(name));
	}
	
	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingHeaders.getAll(name);
	}
	
	@Override
	public List<Entry<String, String>> getAll() {
		return this.underlyingHeaders.entries();
	}
	
	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.decode(name.toString(), value));
	}

	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.underlyingHeaders.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		return this.underlyingHeaders.entries().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
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
}

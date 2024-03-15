/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.OutboundCookies;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Base {@link OutboundRequestHeaders} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class GenericRequestHeaders<A extends GenericRequestHeaders<A>> implements OutboundRequestHeaders {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	protected final LinkedHttpHeaders underlyingHeaders;
	protected GenericRequestCookies requestCookies;
	
	/**
	 * <p>
	 * Creates empty request headers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public GenericRequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingHeaders = new LinkedHttpHeaders();
	}
	
	/**
	 * <p>
	 * Returns the underlyinh headers.
	 * </p>
	 * 
	 * @return the underlyinh headers
	 */
	public LinkedHttpHeaders getUnderlyingHeaders() {
		return this.underlyingHeaders;
	}
	
	@Override
	public boolean isWritten() {
		return false;
	}

	@Override
	public A contentType(String contentType) {
		this.underlyingHeaders.set((CharSequence)Headers.NAME_CONTENT_TYPE, contentType);
		return (A)this;
	}

	@Override
	public String getContentType() {
		return this.underlyingHeaders.get((CharSequence)Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public Headers.ContentType getContentTypeHeader() {
		return this.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
	}
	
	@Override
	public A contentLength(long contentLength) {
		this.underlyingHeaders.setLong((CharSequence)Headers.NAME_CONTENT_LENGTH, contentLength);
		return (A)this;
	}
	
	@Override
	public Long getContentLength() {
		return this.underlyingHeaders.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public A cookies(Consumer<OutboundCookies> cookiesConfigurer) {
		if(this.requestCookies == null) {
			this.requestCookies = new GenericRequestCookies(this, this.headerService, this.parameterConverter);
		}
		this.requestCookies.load();
		cookiesConfigurer.accept(this.requestCookies);
		this.requestCookies.commit();
		return (A)this;
	}

	@Override
	public InboundCookies cookies() {
		if(this.requestCookies == null) {
			this.requestCookies = new GenericRequestCookies(this, this.headerService, this.parameterConverter);
		}
		this.requestCookies.load();
		return this.requestCookies;
	}
	
	@Override
	public A add(CharSequence name, CharSequence value) {
		this.underlyingHeaders.addCharSequence(name, value);
		return (A)this;
	}

	@Override
	public A add(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.addCharSequence(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return (A)this;
	}

	@Override
	public A set(CharSequence name, CharSequence value) {
		this.underlyingHeaders.setCharSequence(name, value);
		return (A)this;
	}

	@Override
	public A set(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.setCharSequence(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return (A)this;
	}

	@Override
	public A remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.underlyingHeaders.remove(name);
		}
		return (A)this;
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
	public List<Map.Entry<String, String>> getAll() {
		return this.underlyingHeaders.entries();
	}

	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.<T>decode(name.toString(), value));
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
	
	/**
	 * <p>
	 * Returns the value of the header with the specified name as a char sequence.
	 * </p>
	 * 
	 * @param name the header name
	 * 
	 * @return the header value or null if there's no header with the specified name
	 */
	public CharSequence getCharSequence(CharSequence name) {
		return this.underlyingHeaders.getCharSequence(name);
	}

	/**
	 * <p>
	 * Returns the values of all headers with the specified name as char sequences.
	 * </p>
	 *
	 * @param name a header name
	 *
	 * @return a list of header values or an empty list if there's no header with the specified name
	 */
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.underlyingHeaders.getAllCharSequence(name);
	}

	/**
	 * <p>
	 * Returns all headers.
	 * </p>
	 *
	 * @return a list of header entries or an empty list if there's no header
	 */
	public List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence() {
		return this.underlyingHeaders.entriesCharSequence();
	}
}

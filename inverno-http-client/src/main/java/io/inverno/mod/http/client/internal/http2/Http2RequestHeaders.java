/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.client.internal.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.OutboundCookies;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.GenericRequestCookies;
import io.inverno.mod.http.client.internal.HttpConnectionRequestHeaders;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * HTTP/2 {@link OutboundRequestHeaders} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http2RequestHeaders implements HttpConnectionRequestHeaders {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Http2Headers underlyingHeaders;
	protected GenericRequestCookies requestCookies;
	
	private boolean written;

	/**
	 * <p>
	 * Creates blank HTTP/2 request headers.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param endpointRequest    the original endpoint request 
	 * @param validateHeaders    true to validate headers, false otherwise
	 */
	public Http2RequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, EndpointRequest endpointRequest, boolean validateHeaders) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingHeaders = new DefaultHttp2Headers(validateHeaders);
		endpointRequest.headers().getAll().forEach(e -> this.add(e.getKey(), e.getValue()));
	}
	
	/**
	 * <p>
	 * Returns the underlyinh headers.
	 * </p>
	 * 
	 * @return the underlyinh headers
	 */
	public Http2Headers getUnderlyingHeaders() {
		return this.underlyingHeaders;
	}
	
	@Override
	public void setWritten(boolean written) {
		this.written = written;
	}
	
	@Override
	public boolean isWritten() {
		return this.written;
	}

	@Override
	public Http2RequestHeaders contentType(String contentType) {
		this.underlyingHeaders.set(Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}
	
	@Override
	public String getContentType() {
		return this.underlyingHeaders.get(Headers.NAME_CONTENT_TYPE).toString();
	}

	@Override
	public Headers.ContentType getContentTypeHeader() {
		return this.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
	}

	@Override
	public Http2RequestHeaders contentLength(long contentLength) {
		this.underlyingHeaders.setLong(Headers.NAME_CONTENT_LENGTH, contentLength);
		return this;
	}
	
	@Override
	public Long getContentLength() {
		return this.underlyingHeaders.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public OutboundRequestHeaders cookies(Consumer<OutboundCookies> cookiesConfigurer) {
		if(cookiesConfigurer != null) {
			cookiesConfigurer.accept((OutboundCookies)this.cookies());
			this.requestCookies.commit();
		}
		return this;
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
	public Http2RequestHeaders add(CharSequence name, CharSequence value) {
		this.underlyingHeaders.add(name, value);
		return this;
	}

	@Override
	public Http2RequestHeaders add(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.add(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http2RequestHeaders set(CharSequence name, CharSequence value) {
		this.underlyingHeaders.set(name, value);
		return this;
	}

	@Override
	public Http2RequestHeaders set(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.set(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http2RequestHeaders remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.underlyingHeaders.remove(name);
		}
		return this;
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
		return this.underlyingHeaders.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.underlyingHeaders.get(name)).map(Object::toString);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingHeaders.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		List<Map.Entry<String, String>> result = new LinkedList<>();
		this.underlyingHeaders.forEach(e -> {
			result.add(Map.entry(e.getKey().toString(), e.getValue().toString()));
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
	public CharSequence getCharSequence(CharSequence name) {
		return this.underlyingHeaders.get(name);
	}

	@Override
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.underlyingHeaders.getAll(name);
	}

	@Override
	public List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence() {
		List<Map.Entry<CharSequence, CharSequence>> result = new LinkedList<>();
		this.underlyingHeaders.forEach(e -> {
			result.add(Map.entry(e.getKey(), e.getValue()));
		});
		return result;
	}
}

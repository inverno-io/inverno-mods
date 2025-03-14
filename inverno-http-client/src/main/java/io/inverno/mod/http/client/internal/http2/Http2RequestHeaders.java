/*
 * Copyright 2022 Jeremy Kuhn
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
import io.inverno.mod.http.base.OutboundCookies;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.client.internal.AbstractRequestHeaders;
import io.inverno.mod.http.client.internal.EndpointRequestHeaders;
import io.inverno.mod.http.client.internal.GenericRequestCookies;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Http/2 {@link OutboundRequestHeaders} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2RequestHeaders extends AbstractRequestHeaders<Http2Headers> {
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final Http2Headers headers;
	
	private GenericRequestCookies cookies;

	/**
	 * <p>
	 * Creates Http/2 request headers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param endpointHeaders    the original endpoint request
	 * @param validateHeaders    true to validate headers, false otherwise
	 */
	public Http2RequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, EndpointRequestHeaders endpointHeaders, boolean validateHeaders) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.headers = new DefaultHttp2Headers(validateHeaders);
		endpointHeaders.getAll().forEach(e -> this.add(e.getKey(), e.getValue()));
	}
	
	@Override
	protected Http2Headers unwrap() {
		return this.headers;
	}
	
	@Override
	public Http2RequestHeaders contentType(String contentType) {
		this.headers.set(Headers.NAME_CONTENT_TYPE, contentType);
		return this;
	}
	
	@Override
	public String getContentType() {
		return this.headers.get(Headers.NAME_CONTENT_TYPE).toString();
	}
	
	@Override
	public Headers.ContentType getContentTypeHeader() {
		return this.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
	}

	@Override
	public OutboundRequestHeaders accept(String accept) {
		this.headers.set(Headers.NAME_ACCEPT, accept);
		return this;
	}

	@Override
	public String getAccept() {
		return this.headers.get(Headers.NAME_ACCEPT).toString();
	}

	@Override
	public Headers.Accept getAcceptHeader() {
		return Headers.Accept.merge(this.getAllHeader(Headers.NAME_ACCEPT)).orElse(null);
	}

	@Override
	public Http2RequestHeaders contentLength(long contentLength) {
		this.headers.setLong(Headers.NAME_CONTENT_LENGTH, contentLength);
		return this;
	}
	
	@Override
	public Long getContentLength() {
		return this.headers.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public Http2RequestHeaders cookies(Consumer<OutboundCookies> cookiesConfigurer) {
		if(cookiesConfigurer != null) {
			cookiesConfigurer.accept(this.cookies());
		}
		return this;
	}

	@Override
	public GenericRequestCookies cookies() {
		if(this.cookies == null) {
			this.cookies = new GenericRequestCookies(this.headerService, this.parameterConverter, this);
		}
		return this.cookies;
	}
	
	@Override
	public Http2RequestHeaders add(CharSequence name, CharSequence value) {
		this.headers.add(name, value);
		return this;
	}

	@Override
	public <T> Http2RequestHeaders addParameter(CharSequence name, T value) {
		return this.add(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> OutboundRequestHeaders addParameter(CharSequence name, T value, Type type) {
		return this.add(name, this.parameterConverter.encode(value, type));
	}

	@Override
	public Http2RequestHeaders add(List<? extends Header> headers) {
		for(Header header : headers) {
			this.headers.add(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http2RequestHeaders set(CharSequence name, CharSequence value) {
		this.headers.set(name, value);
		return this;
	}

	@Override
	public <T> Http2RequestHeaders setParameter(CharSequence name, T value) {
		return this.set(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> OutboundRequestHeaders setParameter(CharSequence name, T value, Type type) {
		return this.set(name, this.parameterConverter.encode(value, type));
	}

	@Override
	public Http2RequestHeaders set(List<? extends Header> headers) {
		for(Header header : headers) {
			this.headers.set(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http2RequestHeaders remove(Set<? extends CharSequence> names) {
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
		return this.headers.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name)).map(Object::toString);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.headers.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		List<Map.Entry<String, String>> result = new LinkedList<>();
		this.headers.forEach(e -> result.add(Map.entry(e.getKey().toString(), e.getValue().toString())));
		return result;
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.headers.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value.toString(), this.parameterConverter)).collect(Collectors.toList());
	}

	@Override
	public List<Parameter> getAllParameter() {
		List<Parameter> result = new LinkedList<>();
		this.headers.forEach(e -> result.add(new GenericParameter(e.getValue().toString(), e.getValue().toString(), this.parameterConverter)));
		return result;
	}

	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.decode(name.toString(), value));
	}

	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.headers.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value.toString())).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		List<Header> result = new LinkedList<>();
		this.headers.forEach(e -> result.add(this.headerService.decode(e.getKey().toString(), e.getValue().toString())));
		return result;
	}
}

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
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.base.InboundSetCookies;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.OutboundSetCookies;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.client.internal.AbstractResponseHeaders;
import io.inverno.mod.http.client.internal.EndpointInterceptedResponseCookies;
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
 * Http/2 {@link InboundResponseHeaders} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2ResponseHeaders extends AbstractResponseHeaders {

	private final HeaderService headerService;
	private final Http2Headers headers;

	/**
	 * <p>
	 * Creates Http/2 response headers.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headers            the originating HTTP headers
	 */
	public Http2ResponseHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, Http2Headers headers) {
		super(parameterConverter);
		this.headerService = headerService;
		this.headers = headers;
	}

	@Override
	protected void configureInterceptedHeaders(Consumer<OutboundResponseHeaders> headersConfigurer) {
		headersConfigurer.accept(new OutboundResponseHeaders() {

			private EndpointInterceptedResponseCookies responseCookies;

			@Override
			public OutboundResponseHeaders status(Status status) {
				return this;
			}

			@Override
			public OutboundResponseHeaders status(int status) {
				return this;
			}

			@Override
			public OutboundResponseHeaders contentType(String contentType) {
				return this;
			}

			@Override
			public OutboundResponseHeaders contentLength(long contentLength) {
				return this;
			}

			@Override
			public OutboundResponseHeaders cookies(Consumer<OutboundSetCookies> cookiesConfigurer) {
				if(cookiesConfigurer != null) {
					cookiesConfigurer.accept((OutboundSetCookies)this.cookies());
				}
				return this;
			}

			@Override
			public Status getStatus() throws IllegalArgumentException {
				return Http2ResponseHeaders.this.getStatus();
			}

			@Override
			public int getStatusCode() {
				return Http2ResponseHeaders.this.getStatusCode();
			}

			@Override
			public String getContentType() {
				return Http2ResponseHeaders.this.getContentType();
			}

			@Override
			public Headers.ContentType getContentTypeHeader() {
				return Http2ResponseHeaders.this.getContentTypeHeader();
			}

			@Override
			public Long getContentLength() {
				return Http2ResponseHeaders.this.getContentLength();
			}

			@Override
			public InboundSetCookies cookies() {
				if(this.responseCookies == null) {
					this.responseCookies = new EndpointInterceptedResponseCookies(Http2ResponseHeaders.this.headerService, Http2ResponseHeaders.this.parameterConverter, this);
				}
				return this.responseCookies;
			}

			@Override
			public boolean isWritten() {
				return true;
			}

			@Override
			public OutboundResponseHeaders add(CharSequence name, CharSequence value) {
				Http2ResponseHeaders.this.headers.add(name, value);
				return this;
			}

			@Override
			public <T> OutboundResponseHeaders addParameter(CharSequence name, T value) {
				return this.add(name, Http2ResponseHeaders.this.parameterConverter.encode(value));
			}

			@Override
			public <T> OutboundResponseHeaders addParameter(CharSequence name, T value, Type type) {
				return this.add(name, Http2ResponseHeaders.this.parameterConverter.encode(value, type));
			}

			@Override
			public OutboundResponseHeaders add(List<? extends Header> headers) {
				for(Header header : headers) {
					Http2ResponseHeaders.this.headers.add(header.getHeaderName(), Http2ResponseHeaders.this.headerService.encodeValue(header));
				}
				return this;
			}

			@Override
			public OutboundResponseHeaders set(CharSequence name, CharSequence value) {
				Http2ResponseHeaders.this.headers.set(name, value);
				return null;
			}

			@Override
			public <T> OutboundResponseHeaders setParameter(CharSequence name, T value) {
				return this.set(name, Http2ResponseHeaders.this.parameterConverter.encode(value));
			}

			@Override
			public <T> OutboundResponseHeaders setParameter(CharSequence name, T value, Type type) {
				return this.set(name, Http2ResponseHeaders.this.parameterConverter.encode(value, type));
			}

			@Override
			public OutboundResponseHeaders set(List<? extends Header> headers) {
				for(Header header : headers) {
					Http2ResponseHeaders.this.headers.set(header.getHeaderName(), Http2ResponseHeaders.this.headerService.encodeValue(header));
				}
				return this;
			}

			@Override
			public OutboundResponseHeaders remove(Set<? extends CharSequence> names) {
				for(CharSequence name : names) {
					Http2ResponseHeaders.this.headers.remove(name);
				}
				return this;
			}

			@Override
			public boolean contains(CharSequence name) {
				return Http2ResponseHeaders.this.headers.contains(name);
			}

			@Override
			public boolean contains(CharSequence name, CharSequence value) {
				return Http2ResponseHeaders.this.headers.contains(name, value, true);
			}

			@Override
			public Set<String> getNames() {
				return Http2ResponseHeaders.this.headers.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
			}

			@Override
			public Optional<String> get(CharSequence name) {
				return Optional.ofNullable(Http2ResponseHeaders.this.headers.get(name)).map(Object::toString);
			}

			@Override
			public List<String> getAll(CharSequence name) {
				return Http2ResponseHeaders.this.headers.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
			}

			@Override
			public List<Map.Entry<String, String>> getAll() {
				List<Map.Entry<String, String>> result = new LinkedList<>();
				Http2ResponseHeaders.this.headers.forEach(e -> result.add(Map.entry(e.getKey().toString(), e.getValue().toString())));
				return result;
			}

			@Override
			public Optional<Parameter> getParameter(CharSequence name) {
				return this.get(name).map(value -> new GenericParameter(name.toString(), value, Http2ResponseHeaders.this.parameterConverter));
			}

			@Override
			public List<Parameter> getAllParameter(CharSequence name) {
				return Http2ResponseHeaders.this.headers.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value.toString(), Http2ResponseHeaders.this.parameterConverter)).collect(Collectors.toList());
			}

			@Override
			public List<Parameter> getAllParameter() {
				List<Parameter> result = new LinkedList<>();
				Http2ResponseHeaders.this.headers.forEach(e -> result.add(new GenericParameter(e.getKey().toString(), e.getValue().toString(), Http2ResponseHeaders.this.parameterConverter)));
				return result;
			}

			@Override
			public <T extends Header> Optional<T> getHeader(CharSequence name) {
				return this.get(name).map(value -> Http2ResponseHeaders.this.headerService.decode(name.toString(), value));
			}

			@Override
			public <T extends Header> List<T> getAllHeader(CharSequence name) {
				return Http2ResponseHeaders.this.headers.getAll(name).stream().map(value -> Http2ResponseHeaders.this.headerService.<T>decode(name.toString(), value.toString())).collect(Collectors.toList());
			}

			@Override
			public List<Header> getAllHeader() {
				List<Header> result = new LinkedList<>();
				Http2ResponseHeaders.this.headers.forEach(e -> result.add(Http2ResponseHeaders.this.headerService.decode(e.getKey().toString(), e.getValue().toString())));
				return result;
			}
		});
	}

	@Override
	public Status getStatus() throws IllegalArgumentException {
		return Status.valueOf(this.getStatusCode());
	}

	@Override
	public int getStatusCode() {
		return this.headers.getInt(Http2Headers.PseudoHeaderName.STATUS.value());
	}

	@Override
	public String getContentType() {
		CharSequence value = this.headers.get(Headers.NAME_CONTENT_TYPE);
		return value != null ? value.toString() : null;
	}

	@Override
	public Headers.ContentType getContentTypeHeader() {
		return this.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
	}

	@Override
	public Long getContentLength() {
		return this.headers.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
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
		this.headers.forEach(e -> result.add(new GenericParameter(e.getKey().toString(), e.getValue().toString(), this.parameterConverter)));
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

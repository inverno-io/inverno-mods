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
package io.inverno.mod.http.client.internal.http1x;

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
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.client.internal.AbstractResponseHeaders;
import io.inverno.mod.http.client.internal.EndpointInterceptedResponseCookies;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Http/1.x {@link InboundResponseHeaders} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xResponseHeaders extends AbstractResponseHeaders {
	
	private final HeaderService headerService;
	private final LinkedHttpHeaders headers;
	private final int statusCode;
	
	private Status status;

	/**
	 * <p>
	 * Creates Http/1.x response headers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headers            the originating HTTP headers
	 * @param statusCode         the response status code
	 */
	public Http1xResponseHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, LinkedHttpHeaders headers, int statusCode) {
		super(parameterConverter);
		this.headerService = headerService;
		this.headers = headers;
		this.statusCode = statusCode;
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
				return Http1xResponseHeaders.this.getStatus();
			}

			@Override
			public int getStatusCode() {
				return Http1xResponseHeaders.this.getStatusCode();
			}

			@Override
			public String getContentType() {
				return Http1xResponseHeaders.this.getContentType();
			}

			@Override
			public Headers.ContentType getContentTypeHeader() {
				return Http1xResponseHeaders.this.getContentTypeHeader();
			}

			@Override
			public Long getContentLength() {
				return Http1xResponseHeaders.this.getContentLength();
			}

			@Override
			public InboundSetCookies cookies() {
				if(this.responseCookies == null) {
					this.responseCookies = new EndpointInterceptedResponseCookies(Http1xResponseHeaders.this.headerService, Http1xResponseHeaders.this.parameterConverter, this);
				}
				return this.responseCookies;
			}

			@Override
			public boolean isWritten() {
				return true;
			}

			@Override
			public OutboundResponseHeaders add(CharSequence name, CharSequence value) {
				Http1xResponseHeaders.this.headers.addCharSequence(name, value);
				return this;
			}

			@Override
			public <T> OutboundResponseHeaders addParameter(CharSequence name, T value) {
				return this.add(name, Http1xResponseHeaders.this.parameterConverter.encode(value));
			}

			@Override
			public <T> OutboundResponseHeaders addParameter(CharSequence name, T value, Type type) {
				return this.add(name, Http1xResponseHeaders.this.parameterConverter.encode(value, type));
			}

			@Override
			public OutboundResponseHeaders add(List<? extends Header> headers) {
				for(Header header : headers) {
					Http1xResponseHeaders.this.headers.addCharSequence(header.getHeaderName(), Http1xResponseHeaders.this.headerService.encodeValue(header));
				}
				return this;
			}

			@Override
			public OutboundResponseHeaders set(CharSequence name, CharSequence value) {
				Http1xResponseHeaders.this.headers.setCharSequence(name, value);
				return this;
			}

			@Override
			public <T> OutboundResponseHeaders setParameter(CharSequence name, T value) {
				return this.set(name, Http1xResponseHeaders.this.parameterConverter.encode(value));
			}

			@Override
			public <T> OutboundResponseHeaders setParameter(CharSequence name, T value, Type type) {
				return this.set(name, Http1xResponseHeaders.this.parameterConverter.encode(value, type));
			}

			@Override
			public OutboundResponseHeaders set(List<? extends Header> headers) {
				for(Header header : headers) {
					Http1xResponseHeaders.this.headers.setCharSequence(header.getHeaderName(), Http1xResponseHeaders.this.headerService.encodeValue(header));
				}
				return this;
			}

			@Override
			public OutboundResponseHeaders remove(Set<? extends CharSequence> names) {
				for(CharSequence name : names) {
					Http1xResponseHeaders.this.headers.remove(name);
				}
				return this;
			}

			@Override
			public boolean contains(CharSequence name) {
				return Http1xResponseHeaders.this.headers.contains(name);
			}

			@Override
			public boolean contains(CharSequence name, CharSequence value) {
				return Http1xResponseHeaders.this.headers.contains(name, value, true);
			}

			@Override
			public Set<String> getNames() {
				return Http1xResponseHeaders.this.headers.names();
			}

			@Override
			public Optional<String> get(CharSequence name) {
				return Optional.ofNullable(Http1xResponseHeaders.this.headers.get(name));
			}

			@Override
			public List<String> getAll(CharSequence name) {
				return Http1xResponseHeaders.this.headers.getAll(name);
			}

			@Override
			public List<Map.Entry<String, String>> getAll() {
				return Http1xResponseHeaders.this.headers.entries();
			}

			@Override
			public Optional<Parameter> getParameter(CharSequence name) {
				return this.get(name).map(value -> new GenericParameter(name.toString(), value, Http1xResponseHeaders.this.parameterConverter));
			}

			@Override
			public List<Parameter> getAllParameter(CharSequence name) {
				return Http1xResponseHeaders.this.headers.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value, Http1xResponseHeaders.this.parameterConverter)).collect(Collectors.toList());
			}

			@Override
			public List<Parameter> getAllParameter() {
				return Http1xResponseHeaders.this.headers.entries().stream().map(e -> new GenericParameter(e.getKey(), e.getValue(), Http1xResponseHeaders.this.parameterConverter)).collect(Collectors.toList());
			}

			@Override
			public <T extends Header> Optional<T> getHeader(CharSequence name) {
				return this.get(name).map(value -> Http1xResponseHeaders.this.headerService.decode(name.toString(), value));
			}

			@Override
			public <T extends Header> List<T> getAllHeader(CharSequence name) {
				return this.getAll(name).stream().map(value -> Http1xResponseHeaders.this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
			}

			@Override
			public List<Header> getAllHeader() {
				return this.getAll().stream().map(e -> Http1xResponseHeaders.this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
			}
		});
	}

	@Override
	public Status getStatus() throws IllegalArgumentException {
		if(this.status == null) {
			this.status = Status.valueOf(this.statusCode);
		}
		return this.status;
	}

	@Override
	public int getStatusCode() {
		return this.statusCode;
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

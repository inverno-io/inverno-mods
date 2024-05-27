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
package io.inverno.mod.http.server.internal.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.server.internal.AbstractResponseTrailers;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Http/2 {@link OutboundHeaders} implementation representing Http trailers.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http2ResponseTrailers extends AbstractResponseTrailers<Http2ResponseTrailers> {

	private final Http2Headers trailers;
	
	/**
	 * <p>
	 * Creates Http/2 response trailers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param validate           true to validate trailers, false otherwise
	 */
	public Http2ResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter, boolean validate) {
		super(headerService, parameterConverter);
		this.trailers = new DefaultHttp2Headers(validate, validate, 16);
	}
	
	/**
	 * <p>
	 * Returns the trailers to send as part of the Http response.
	 * </p>
	 * 
	 * @return the wrapped trailers
	 */
	Http2Headers unwrap() {
		return this.trailers;
	}

	@Override
	public Http2ResponseTrailers add(CharSequence name, CharSequence value) {
		this.trailers.add(name, value);
		return this;
	}

	@Override
	public Http2ResponseTrailers add(Header... headers) {
		for(Header header : headers) {
			this.trailers.add(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http2ResponseTrailers set(CharSequence name, CharSequence value) {
		this.trailers.set(name, value);
		return this;
	}

	@Override
	public Http2ResponseTrailers set(Header... headers) {
		for(Header header : headers) {
			this.trailers.set(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http2ResponseTrailers remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.trailers.remove(name);
		}
		return this;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.trailers.contains(name);
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.trailers.contains(name, value, true);
	}

	@Override
	public Set<String> getNames() {
		return this.trailers.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.trailers.get(name)).map(Object::toString);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.trailers.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		List<Map.Entry<String, String>> result = new LinkedList<>();
		this.trailers.forEach(e -> {
			result.add(Map.entry(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}

	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.trailers.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value.toString(), this.parameterConverter)).collect(Collectors.toList());
	}

	@Override
	public List<Parameter> getAllParameter() {
		List<Parameter> result = new LinkedList<>();
		this.trailers.forEach(e -> {
			result.add(new GenericParameter(e.getKey().toString(), e.getValue().toString(), this.parameterConverter));
		});
		return result;
	}
	
	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.decode(name.toString(), value));
	}

	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.trailers.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value.toString())).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		List<Header> result = new LinkedList<>();
		this.trailers.forEach(e -> {
			result.add(this.headerService.<Header>decode(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}
}

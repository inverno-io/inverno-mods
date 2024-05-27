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
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.server.internal.AbstractResponseTrailers;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Http/1.x {@link OutboundHeaders} implementation representing Http trailers.
 * </p>
 * 
 * <p>
 * This implementation uses {@link LinkedHttpHeaders} instead of Netty's {@link DefaultHttpHeaders} as internal headers in order to increase performances.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http1xResponseTrailers extends AbstractResponseTrailers<Http1xResponseTrailers> {

	private final LinkedHttpHeaders trailers;

	/**
	 * <p>
	 * Creates Http/1.x trailers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headersValidator   the headers validator
	 */
	public Http1xResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter, HeadersValidator headersValidator) {
		super(headerService, parameterConverter);
		this.trailers = new LinkedHttpHeaders(headersValidator);
	}

	/**
	 * <p>
	 * Returns the trailers to send as part of the Http response.
	 * </p>
	 * 
	 * @return the wrapped trailers
	 */
	LinkedHttpHeaders unwrap() {
		return trailers;
	}

	@Override
	public Http1xResponseTrailers add(CharSequence name, CharSequence value) {
		this.trailers.addCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseTrailers add(Header... headers) {
		for(Header header : headers) {
			this.trailers.addCharSequence(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http1xResponseTrailers set(CharSequence name, CharSequence value) {
		this.trailers.setCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseTrailers set(Header... headers) {
		for(Header header : headers) {
			this.trailers.setCharSequence(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return this;
	}

	@Override
	public Http1xResponseTrailers remove(CharSequence... names) {
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
		return this.trailers.names();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.trailers.get(name));
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.trailers.getAll(name);
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		return this.trailers.entries();
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.trailers.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value, this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		return this.trailers.entries().stream().map(e -> new GenericParameter(e.getKey(), e.getValue(), this.parameterConverter)).collect(Collectors.toList());
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
}

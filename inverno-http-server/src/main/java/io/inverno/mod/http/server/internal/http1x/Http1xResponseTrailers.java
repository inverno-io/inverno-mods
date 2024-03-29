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
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * HTTP1.x response trailers implementation.
 * </p>
 *
 * <p>
 * This implementation uses {@link LinkedHttpHeaders} instead of Netty's {@link DefaultHttpHeaders} as internal headers in order to increase performances.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http1xResponseTrailers implements OutboundHeaders<Http1xResponseTrailers> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final LinkedHttpHeaders underlyingTrailers;
	
	private boolean written;
	
	/**
	 * <p>
	 * Creates HTTP1.x server response trailers.
	 * </p>
	 * 
	 * @param httpRequest        the underlying HTTP request
	 * @param headerService      the header service
	 * @param parameterConverter a string object converter
	 */
	public Http1xResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingTrailers = new LinkedHttpHeaders();
	}

	@Override
	public boolean isWritten() {
		return this.written;
	}

	public void setWritten(boolean written) {
		this.written = written;
	}
	
	/**
	 * <p>
	 * Returns the underlying trailers.
	 * </p>
	 * 
	 * @return the underlying trailers
	 */
	LinkedHttpHeaders getUnderlyingTrailers() {
		return this.underlyingTrailers;
	}
	
	@Override
	public Http1xResponseTrailers add(CharSequence name, CharSequence value) {
		this.underlyingTrailers.addCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseTrailers add(Header... trailers) {
		for(Header trailer : trailers) {
			this.underlyingTrailers.addCharSequence(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public Http1xResponseTrailers set(CharSequence name, CharSequence value) {
		this.underlyingTrailers.setCharSequence(name, value);
		return this;
	}

	@Override
	public Http1xResponseTrailers set(Header... trailers) {
		for(Header trailer : trailers) {
			this.underlyingTrailers.setCharSequence(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public Http1xResponseTrailers remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.underlyingTrailers.remove(name);
		}
		return this;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.underlyingTrailers.contains(name);
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.underlyingTrailers.contains(name, value, true);
	}

	@Override
	public Set<String> getNames() {
		return this.underlyingTrailers.names();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.underlyingTrailers.get((CharSequence)name));
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingTrailers.getAll(name);
	}

	@Override
	public List<Entry<String, String>> getAll() {
		return this.underlyingTrailers.entries();
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
		return this.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value, this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		return this.getAll().stream().map(e -> new GenericParameter(e.getKey(), e.getValue(), this.parameterConverter)).collect(Collectors.toList());
	}
}

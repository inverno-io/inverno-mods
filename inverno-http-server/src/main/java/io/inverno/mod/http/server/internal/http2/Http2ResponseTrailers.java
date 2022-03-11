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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.server.ResponseTrailers;

/**
 * <p>
 * HTTP/2 {@link ResponseTrailers} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http2ResponseTrailers implements ResponseTrailers {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Http2Headers underlyingTrailers;
	
	/**
	 * <p>
	 * Creates HTTP/2 response trailers.
	 * </p>
	 * 
	 * @param headerService the header service
	 * @param parameterConverter a string object converter 
	 */
	public Http2ResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingTrailers = new DefaultHttp2Headers();
	}

	/**
	 * <p>
	 * Returns the underlying headers.
	 * </p>
	 * 
	 * @return the underlying headers
	 */
	Http2Headers getUnderlyingTrailers() {
		return this.underlyingTrailers;
	}

	@Override
	public ResponseTrailers add(CharSequence name, CharSequence value) {
		this.underlyingTrailers.add(name, value);
		return this;
	}

	@Override
	public ResponseTrailers add(Header... trailers) {
		for(Header trailer : trailers) {
			this.underlyingTrailers.add(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseTrailers set(CharSequence name, CharSequence value) {
		this.underlyingTrailers.set(name, value);
		return this;
	}

	@Override
	public ResponseTrailers set(Header... trailers) {
		for(Header trailer : trailers) {
			this.underlyingTrailers.set(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseTrailers remove(CharSequence... names) {
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
		return this.underlyingTrailers.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.of(this.underlyingTrailers.get(name)).map(Object::toString);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingTrailers.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}

	@Override
	public List<Entry<String, String>> getAll() {
		List<Entry<String, String>> result = new LinkedList<>();
		this.underlyingTrailers.forEach(e -> {
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
		return this.underlyingTrailers.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value.toString())).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		List<Header> result = new LinkedList<>();
		this.underlyingTrailers.forEach(e -> {
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
		return this.underlyingTrailers.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value.toString(), this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		List<Parameter> result = new LinkedList<>();
		this.underlyingTrailers.forEach(e -> {
			result.add(new GenericParameter(e.getKey().toString(), e.getValue().toString(), this.parameterConverter));
		});
		return result;
	}
}

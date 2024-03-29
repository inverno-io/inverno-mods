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
import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * HTTP/2 {@link InboundHeaders} implementation to represent HTTP trailers.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http2ResponseTrailers implements InboundHeaders {

	private final Http2Headers underlyingTrailers;
	
	private final HeaderService headerService;
	
	private final ObjectConverter<String> parameterConverter;
	
	/**
	 * <p>
	 * Creates HTTP/1.x response trailers.
	 * </p>
	 *
	 * @param httpTrailers       the underlying HTTP trailers
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public Http2ResponseTrailers(Http2Headers httpTrailers, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.underlyingTrailers = httpTrailers;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
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
		return Optional.ofNullable(this.underlyingTrailers.get(name)).map(Object::toString);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingTrailers.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		List<Map.Entry<String, String>> result = new LinkedList<>();
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
			result.add(new GenericParameter(e.getValue().toString(), e.getValue().toString(), this.parameterConverter));
		});
		return result;
	}
}

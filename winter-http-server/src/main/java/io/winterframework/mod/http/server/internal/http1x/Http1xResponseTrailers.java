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
package io.winterframework.mod.http.server.internal.http1x;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;

import io.netty.handler.codec.http.HttpHeaders;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.header.Header;
import io.winterframework.mod.http.base.header.HeaderService;
import io.winterframework.mod.http.base.internal.GenericParameter;
import io.winterframework.mod.http.server.ResponseTrailers;
import io.winterframework.mod.http.server.internal.netty.LinkedHttpHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xResponseTrailers implements ResponseTrailers {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final LinkedHttpHeaders internalTrailers;
	
	public Http1xResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.internalTrailers = new LinkedHttpHeaders();
	}

	HttpHeaders getInternalTrailers() {
		return this.internalTrailers;
	}
	
	@Override
	public ResponseTrailers add(CharSequence name, CharSequence value) {
		this.internalTrailers.addCharSequence(name, value);
		return this;
	}

	@Override
	public ResponseTrailers add(Header... trailers) {
		for(Header trailer : trailers) {
			this.internalTrailers.addCharSequence(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseTrailers set(CharSequence name, CharSequence value) {
		this.internalTrailers.setCharSequence(name, value);
		return this;
	}

	@Override
	public ResponseTrailers set(Header... trailers) {
		for(Header trailer : trailers) {
			this.internalTrailers.setCharSequence(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseTrailers remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.internalTrailers.remove(name);
		}
		return this;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.internalTrailers.contains(name);
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.internalTrailers.contains(name, value, true);
	}

	@Override
	public Set<String> getNames() {
		return this.internalTrailers.names();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.internalTrailers.get((CharSequence)name));
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.internalTrailers.getAll(name);
	}

	@Override
	public List<Entry<String, String>> getAll() {
		return this.internalTrailers.entries();
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
		return this.get(name).map(value -> new GenericParameter(this.parameterConverter, name.toString(), value));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.getAll(name).stream().map(value -> new GenericParameter(this.parameterConverter, name.toString(), value)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		return this.getAll().stream().map(e -> new GenericParameter(this.parameterConverter, e.getKey(), e.getValue())).collect(Collectors.toList());
	}
}

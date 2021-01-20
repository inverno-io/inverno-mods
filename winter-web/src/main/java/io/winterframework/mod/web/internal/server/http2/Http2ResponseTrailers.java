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
package io.winterframework.mod.web.internal.server.http2;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.internal.server.GenericParameter;
import io.winterframework.mod.web.server.ResponseTrailers;

/**
 * @author jkuhn
 *
 */
public class Http2ResponseTrailers implements ResponseTrailers {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Http2Headers internalTrailers;
	
	public Http2ResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.internalTrailers = new DefaultHttp2Headers();
	}

	Http2Headers getInternalTrailers() {
		return this.internalTrailers;
	}

	@Override
	public ResponseTrailers add(CharSequence name, CharSequence value) {
		this.internalTrailers.add(name, value);
		return this;
	}

	@Override
	public ResponseTrailers add(Header... trailers) {
		for(Header trailer : trailers) {
			this.internalTrailers.add(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public ResponseTrailers set(CharSequence name, CharSequence value) {
		this.internalTrailers.set(name, value);
		return this;
	}

	@Override
	public ResponseTrailers set(Header... trailers) {
		for(Header trailer : trailers) {
			this.internalTrailers.set(trailer.getHeaderName(), trailer.getHeaderValue());
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
	public boolean contains(CharSequence name, CharSequence value) {
		return this.internalTrailers.contains(name, value, true);
	}
	
	@Override
	public Set<String> getNames() {
		return this.internalTrailers.names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.of(this.internalTrailers.get(name)).map(Object::toString);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.internalTrailers.getAll(name).stream().map(CharSequence::toString).collect(Collectors.toList());
	}

	@Override
	public List<Entry<String, String>> getAll() {
		List<Entry<String, String>> result = new LinkedList<>();
		this.internalTrailers.forEach(e -> {
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
		return this.internalTrailers.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value.toString())).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		List<Header> result = new LinkedList<>();
		this.internalTrailers.forEach(e -> {
			result.add(this.headerService.<Header>decode(e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(this.parameterConverter, name.toString(), value));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.internalTrailers.getAll(name).stream().map(value -> new GenericParameter(this.parameterConverter, name.toString(), value.toString())).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		List<Parameter> result = new LinkedList<>();
		this.internalTrailers.forEach(e -> {
			result.add(new GenericParameter(this.parameterConverter, e.getKey().toString(), e.getValue().toString()));
		});
		return result;
	}
}

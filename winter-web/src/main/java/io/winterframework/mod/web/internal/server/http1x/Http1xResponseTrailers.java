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
package io.winterframework.mod.web.internal.server.http1x;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;

import io.netty.handler.codec.http.HttpHeaders;
import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.internal.netty.LinkedHttpHeaders;
import io.winterframework.mod.web.server.ResponseTrailers;

/**
 * @author jkuhn
 *
 */
public class Http1xResponseTrailers implements ResponseTrailers {

	private final LinkedHttpHeaders internalTrailers;
	
	private final HeaderService headerService;
	
	public Http1xResponseTrailers(HeaderService headerService) {
		this.headerService = headerService;
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
	public boolean contains(CharSequence name, CharSequence value) {
		return this.internalTrailers.contains(name, value, true);
	}
}

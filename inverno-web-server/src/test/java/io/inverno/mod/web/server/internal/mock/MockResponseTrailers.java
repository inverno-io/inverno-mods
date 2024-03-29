/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.server.internal.mock;

import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MockResponseTrailers implements OutboundHeaders<MockResponseTrailers> {

	private final HeaderService headerService;
	
	private final Map<String, List<String>> trailers;
	
	private boolean written;
	
	/**
	 * 
	 */
	public MockResponseTrailers(HeaderService headerService) {
		this.headerService = headerService;
		this.trailers = new HashMap<>();
	}

	@Override
	public boolean isWritten() {
		return this.written;
	}

	public void setWritten(boolean written) {
		this.written = written;
	}

	@Override
	public MockResponseTrailers add(CharSequence name, CharSequence value) {
		if(!this.trailers.containsKey(name.toString())) {
			this.trailers.put(name.toString(), new ArrayList<>());
		}
		this.trailers.get(name.toString()).add(value.toString());
		return this;
	}

	@Override
	public MockResponseTrailers add(Header... trailers) {
		for(Header trailer : trailers) {
			this.add(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public MockResponseTrailers set(CharSequence name, CharSequence value) {
		this.remove(name);
		this.add(name, value);
		return this;
	}

	@Override
	public MockResponseTrailers set(Header... trailers) {
		for(Header trailer : trailers) {
			this.set(trailer.getHeaderName(), trailer.getHeaderValue());
		}
		return this;
	}

	@Override
	public MockResponseTrailers remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.trailers.remove(name.toString());
		}
		return this;
	}

	@Override
	public boolean contains(CharSequence name) {
		return this.trailers.containsKey(name.toString());
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.trailers.containsKey(name.toString()) ? this.trailers.get(name.toString()).contains(value.toString()) : false;
	}

	@Override
	public Set<String> getNames() {
		return this.trailers.keySet();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.trailers.get(name.toString())).map(l -> l.get(0));
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return Optional.ofNullable(this.trailers.get(name.toString())).orElse(List.of());
	}

	@Override
	public List<Entry<String, String>> getAll() {
		return this.trailers.entrySet().stream().flatMap(e -> e.getValue().stream().map(value -> Map.entry(e.getKey(), value))).collect(Collectors.toList());
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

	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name.toString()).map(value -> new MockParameter(name.toString(), value));
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.getAll(name.toString()).stream().map(value -> new MockParameter(name.toString(), value)).collect(Collectors.toList());
	}

	@Override
	public List<Parameter> getAllParameter() {
		return this.getAll().stream().map(e -> new MockParameter(e.getKey(), e.getValue())).collect(Collectors.toList());
	}
}

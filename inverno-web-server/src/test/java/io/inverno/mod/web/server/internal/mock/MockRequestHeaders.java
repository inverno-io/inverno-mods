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

import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
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
public class MockRequestHeaders implements InboundRequestHeaders {

	private final HeaderService headerService;
	
	private final Map<String, List<String>> headers;
	
	private final MockRequestCookies cookies;
	
	public MockRequestHeaders(HeaderService headerService, Map<String, List<String>> headers, MockRequestCookies cookies) {
		this.headers = headers;
		this.headerService = headerService;
		this.cookies = cookies;
	}

	@Override
	public String getContentType() {
		return Optional.ofNullable(this.headers.get(Headers.NAME_CONTENT_TYPE)).map(l -> l.get(0)).orElse(null);
	}

	@Override
	public Headers.ContentType getContentTypeHeader() {
		return this.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
	}

	@Override
	public Long getContentLength() {
		return Optional.ofNullable(this.headers.get(Headers.NAME_CONTENT_LENGTH)).map(l -> Long.parseLong(l.get(0))).orElse(null);
	}

	@Override
	public InboundCookies cookies() {
		return this.cookies;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.headers.containsKey(name.toString());
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.headers.containsKey(name.toString()) ? this.headers.get(name.toString()).contains(value.toString()) : false;
	}

	@Override
	public Set<String> getNames() {
		return this.headers.keySet();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name.toString())).map(l -> l.get(0));
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name.toString())).orElse(List.of());
	}

	@Override
	public List<Entry<String, String>> getAll() {
		return this.headers.entrySet().stream().flatMap(e -> e.getValue().stream().map(value -> Map.entry(e.getKey(), value))).collect(Collectors.toList());
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

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

package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericOutboundHeaders<A extends GenericOutboundHeaders<A>> implements OutboundHeaders<A>  {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final LinkedHttpHeaders underlyingHeaders;
	
	private boolean written;

	public GenericOutboundHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.underlyingHeaders = new LinkedHttpHeaders();
	}
	
	public GenericOutboundHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, List<Map.Entry<String, String>> entries) {
		this(headerService, parameterConverter);
		if(entries != null && !entries.isEmpty()) {
			entries.forEach(e -> this.add(e.getKey(), e.getValue()));
		}
	}

	private void checkWritable() throws IllegalStateException {
		if(this.written) {
			throw new IllegalStateException("Headers already written");
		}
	}
	
	@Override
	public boolean isWritten() {
		return this.written;
	}

	@Override
	public void setWritten(boolean written) {
		this.written = written;
	}

	public CharSequence getContentTypeCharSequence() {
		return this.underlyingHeaders.getCharSequence((CharSequence)Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public CharSequence getCharSequence(CharSequence name) {
		return this.underlyingHeaders.getCharSequence(name);
	}

	@Override
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.underlyingHeaders.getAllCharSequence(name);
	}

	@Override
	public List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence() {
		return this.underlyingHeaders.entriesCharSequence();
	}

	@Override
	public Long getContentLength() {
		return this.underlyingHeaders.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	public A contentType(String contentType) {
		this.checkWritable();
		this.underlyingHeaders.set((CharSequence)Headers.NAME_CONTENT_TYPE, contentType);
		return (A)this;
	}

	public A contentLength(long contentLength) {
		this.checkWritable();
		this.underlyingHeaders.setLong((CharSequence)Headers.NAME_CONTENT_LENGTH, contentLength);
		return (A)this;
	}

	public A add(CharSequence name, CharSequence value) {
		this.checkWritable();
		this.underlyingHeaders.addCharSequence(name, value);
		return (A)this;
	}

	public A add(Header... headers) {
		this.checkWritable();
		for(Header header : headers) {
			this.underlyingHeaders.addCharSequence(header.getHeaderName(), header.getHeaderValue());
		}
		return (A)this;
	}

	public A set(CharSequence name, CharSequence value) {
		this.checkWritable();
		this.underlyingHeaders.setCharSequence(name, value);
		return (A)this;
	}

	public A set(Header... headers) {
		this.checkWritable();
		for(Header header : headers) {
			this.underlyingHeaders.setCharSequence(header.getHeaderName(), this.headerService.encodeValue(header));
		}
		return (A)this;
	}

	public A remove(CharSequence... names) {
		this.checkWritable();
		for(CharSequence name : names) {
			this.underlyingHeaders.remove(name);
		}
		return (A)this;
	}

	public Optional<String> getContentType() {
		return Optional.ofNullable(this.underlyingHeaders.get((CharSequence)Headers.NAME_CONTENT_TYPE));
	}

	public Optional<Headers.ContentType> getContentTypeHeader() {
		return this.getHeader(Headers.NAME_CONTENT_TYPE);
	}

	public boolean contains(CharSequence name) {
		return this.underlyingHeaders.contains(name);
	}

	public boolean contains(CharSequence name, CharSequence value) {
		return this.underlyingHeaders.contains(name, value, true);
	}

	public Set<String> getNames() {
		return this.underlyingHeaders.names();
	}

	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.underlyingHeaders.get(name));
	}

	public List<String> getAll(CharSequence name) {
		return this.underlyingHeaders.getAll(name);
	}

	public List<Map.Entry<String, String>> getAll() {
		return this.underlyingHeaders.entries();
	}

	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.<T>decode(name.toString(), value));
	}

	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
	}

	public List<Header> getAllHeader() {
		return this.getAll().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}

	public List<Parameter> getAllParameter(CharSequence name) {
		return this.underlyingHeaders.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value, this.parameterConverter)).collect(Collectors.toList());
	}

	public List<Parameter> getAllParameter() {
		return this.underlyingHeaders.entries().stream().map(e -> new GenericParameter(e.getKey(), e.getValue(), this.parameterConverter)).collect(Collectors.toList());
	}
	
	public LinkedHttpHeaders toHttp1xHeaders() {
		return this.underlyingHeaders;
	}
}
